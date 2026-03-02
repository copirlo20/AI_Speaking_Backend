"""
Qwen Server - API chấm điểm câu trả lời sử dụng mô hình Qwen.
Cung cấp API để chấm điểm câu trả lời của thí sinh sử dụng mô hình Qwen.

Sử dụng hybrid scoring: text similarity (thuật toán) + AI model → kết quả chính xác hơn.
Feedback luôn bằng tiếng Việt.
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
from transformers import AutoModelForCausalLM, AutoTokenizer
import torch
import json
import re
import os
import logging
import difflib
import time
import hashlib
from collections import Counter, OrderedDict
from functools import lru_cache

# ========== CHẾ ĐỘ OFFLINE ==========
# Dùng model đã tải về trong cache, không kết nối internet
os.environ["HF_HUB_OFFLINE"] = "1"
os.environ["TRANSFORMERS_OFFLINE"] = "1"

# Cấu hình logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Khởi tạo Flask app
app = Flask(__name__)
CORS(app)

# ========== CẤU HÌNH MODEL ==========
# Đặt biến môi trường QWEN_MODEL để chọn model. Mặc định dùng 3B.
# Gợi ý:
#   - "Qwen/Qwen2.5-3B-Instruct"  → cần ~6GB VRAM (khuyến nghị)
#   - "Qwen/Qwen2.5-7B-Instruct"  → cần ~14GB VRAM (tốt nhất)
#   - "Qwen/Qwen2.5-1.5B-Instruct" → cần ~3GB VRAM (tối thiểu chấp nhận được)
#   - "Qwen/Qwen2.5-0.5B-Instruct" → KHÔNG khuyến nghị, quá nhỏ để chấm điểm chính xác
MODEL_NAME = os.environ.get("QWEN_MODEL", "Qwen/Qwen2.5-3B-Instruct")

# Tỷ lệ kết hợp điểm: ALGO_WEIGHT + MODEL_WEIGHT = 1.0
# Điểm cuối = algo_score * ALGO_WEIGHT + model_score * MODEL_WEIGHT
ALGO_WEIGHT = float(os.environ.get("ALGO_WEIGHT", "0.4"))
MODEL_WEIGHT = float(os.environ.get("MODEL_WEIGHT", "0.6"))

# ========== TÌM ĐƯỜNG DẪN LOCAL CACHE ==========
# Khi dùng offline, cần trỏ thẳng vào thư mục snapshot trong cache
# để tránh transformers gọi API HuggingFace Hub
def resolve_local_model_path(model_name):
    """Tìm đường dẫn local cache của model đã tải về."""
    # Nếu đã là đường dẫn local → dùng luôn
    if os.path.isdir(model_name):
        return model_name

    # Tìm trong HuggingFace cache
    cache_dir = os.path.join(os.path.expanduser("~"), ".cache", "huggingface", "hub")
    # Model name "Qwen/Qwen2.5-3B-Instruct" → folder "models--Qwen--Qwen2.5-3B-Instruct"
    model_folder = "models--" + model_name.replace("/", "--")
    model_path = os.path.join(cache_dir, model_folder)

    if os.path.isdir(model_path):
        snapshots_dir = os.path.join(model_path, "snapshots")
        if os.path.isdir(snapshots_dir):
            # Lấy snapshot mới nhất (thường chỉ có 1)
            snapshots = sorted(os.listdir(snapshots_dir))
            if snapshots:
                local_path = os.path.join(snapshots_dir, snapshots[-1])
                logger.info(f"Found local cache: {local_path}")
                return local_path

    # Không tìm thấy cache → trả về tên gốc (sẽ báo lỗi rõ ràng hơn)
    logger.warning(f"Local cache not found for {model_name}, using original name")
    return model_name

LOCAL_MODEL_PATH = resolve_local_model_path(MODEL_NAME)

logger.info(f"Loading Qwen model: {MODEL_NAME}...")
logger.info(f"Local path: {LOCAL_MODEL_PATH}")
logger.info(f"Scoring weights: algorithm={ALGO_WEIGHT}, model={MODEL_WEIGHT}")

tokenizer = AutoTokenizer.from_pretrained(LOCAL_MODEL_PATH, local_files_only=True)
model = AutoModelForCausalLM.from_pretrained(
    LOCAL_MODEL_PATH,
    torch_dtype=torch.float16 if torch.cuda.is_available() else torch.float32,
    device_map="auto" if torch.cuda.is_available() else None,
    local_files_only=True
)
device = "cuda" if torch.cuda.is_available() else "cpu"
if device == "cpu":
    model = model.to(device)

# === TỐI ƯU MODEL CHO INFERENCE ===
model.eval()  # Chuyển sang chế độ inference (tắt dropout, batchnorm training)
if hasattr(torch, 'compile') and device == "cuda":
    try:
        model = torch.compile(model, mode="reduce-overhead")
        logger.info("torch.compile applied successfully")
    except Exception as e:
        logger.warning(f"torch.compile not available: {e}")

logger.info(f"Qwen model loaded successfully on {device}!")

# === CACHE CHO MODEL SCORING ===
# LRU cache đơn giản: lưu kết quả chấm điểm để tránh gọi model lại
class ScoringCache:
    """Cache kết quả chấm điểm theo hash của input."""
    def __init__(self, max_size=200):
        self.cache = OrderedDict()
        self.max_size = max_size
        self.hits = 0
        self.misses = 0

    def _make_key(self, question, transcribed_text, sample_answers):
        raw = f"{question}|{transcribed_text}|{json.dumps(sample_answers, sort_keys=True)}"
        return hashlib.md5(raw.encode()).hexdigest()

    def get(self, question, transcribed_text, sample_answers):
        key = self._make_key(question, transcribed_text, sample_answers)
        if key in self.cache:
            self.hits += 1
            self.cache.move_to_end(key)
            logger.info(f"Cache HIT (hits={self.hits}, misses={self.misses})")
            return self.cache[key]
        self.misses += 1
        return None

    def put(self, question, transcribed_text, sample_answers, result):
        key = self._make_key(question, transcribed_text, sample_answers)
        self.cache[key] = result
        self.cache.move_to_end(key)
        if len(self.cache) > self.max_size:
            self.cache.popitem(last=False)

scoring_cache = ScoringCache(max_size=200)

# ========== THUẬT TOÁN TÍNH ĐIỂM (ALGORITHMIC SCORING) ==========

def normalize_text(text):
    """Chuẩn hóa văn bản: lowercase, bỏ dấu câu thừa, gộp khoảng trắng."""
    text = text.lower().strip()
    text = re.sub(r'[^\w\s]', ' ', text)
    text = re.sub(r'\s+', ' ', text)
    return text


def get_word_set(text):
    """Tách từ và trả về tập hợp từ."""
    return set(normalize_text(text).split())


def compute_text_similarity(answer, sample):
    """Tính độ tương đồng giữa câu trả lời và mẫu bằng nhiều phương pháp."""
    norm_answer = normalize_text(answer)
    norm_sample = normalize_text(sample)

    if not norm_answer or not norm_sample:
        return 0.0

    # 1. SequenceMatcher ratio (chuỗi ký tự)
    seq_ratio = difflib.SequenceMatcher(None, norm_answer, norm_sample).ratio()

    # 2. Word overlap (Jaccard similarity)
    words_answer = get_word_set(answer)
    words_sample = get_word_set(sample)
    if words_answer and words_sample:
        intersection = words_answer & words_sample
        union = words_answer | words_sample
        jaccard = len(intersection) / len(union) if union else 0
    else:
        jaccard = 0

    # 3. Cosine similarity (word frequency vectors)
    counter_a = Counter(norm_answer.split())
    counter_s = Counter(norm_sample.split())
    all_words = set(counter_a.keys()) | set(counter_s.keys())
    dot_product = sum(counter_a.get(w, 0) * counter_s.get(w, 0) for w in all_words)
    mag_a = sum(v ** 2 for v in counter_a.values()) ** 0.5
    mag_s = sum(v ** 2 for v in counter_s.values()) ** 0.5
    cosine = dot_product / (mag_a * mag_s) if (mag_a and mag_s) else 0

    # 4. Keyword coverage: bao nhiêu từ quan trọng của mẫu có trong câu trả lời
    # Loại bỏ stop words phổ biến
    stop_words = {'the', 'a', 'an', 'is', 'are', 'was', 'were', 'be', 'been',
                  'being', 'have', 'has', 'had', 'do', 'does', 'did', 'will',
                  'would', 'could', 'should', 'may', 'might', 'can', 'shall',
                  'to', 'of', 'in', 'for', 'on', 'with', 'at', 'by', 'from',
                  'it', 'its', 'this', 'that', 'these', 'those', 'i', 'you',
                  'he', 'she', 'we', 'they', 'me', 'him', 'her', 'us', 'them',
                  'my', 'your', 'his', 'our', 'their', 'and', 'or', 'but', 'not',
                  'so', 'if', 'as', 'than', 'very', 'just', 'about', 'also'}
    key_words_sample = words_sample - stop_words
    if key_words_sample:
        keyword_coverage = len(key_words_sample & words_answer) / len(key_words_sample)
    else:
        keyword_coverage = jaccard

    # Trung bình có trọng số
    similarity = (seq_ratio * 0.15 + jaccard * 0.25 + cosine * 0.30 + keyword_coverage * 0.30)
    return min(similarity, 1.0)


def compute_algorithmic_score(transcribed_text, sample_answers):
    """
    Tính điểm thuật toán dựa trên so sánh với các mẫu.
    Thiết kế THÂN THIỆN với người mới học:
      - Điểm sàn (base) = 3.0 nếu có nỗ lực trả lời
      - Công thức: base + similarity * (ref_score - base) → nâng sàn cho beginner
      - Bonus cho nỗ lực: trả lời dài, dùng keyword đúng
    Trả về (score, best_match_info).
    """
    answer_words = normalize_text(transcribed_text).split()
    word_count = len(answer_words)

    # Điểm nỗ lực cơ bản — thưởng cho việc cố gắng trả lời
    effort_bonus = 0.0
    if word_count >= 3:
        effort_bonus += 0.5   # Đã cố gắng nói
    if word_count >= 8:
        effort_bonus += 0.5   # Trả lời có độ dài
    if word_count >= 15:
        effort_bonus += 0.5   # Trả lời khá chi tiết
    if word_count >= 25:
        effort_bonus += 0.5   # Trả lời rất chi tiết

    if not sample_answers:
        # Không có mẫu → đánh giá dựa trên độ dài + nỗ lực (khoan dung hơn)
        if word_count < 2:
            return 2.0, {"reason": "Câu trả lời quá ngắn, cần nói thêm"}
        elif word_count < 5:
            return 4.0 + effort_bonus, {"reason": "Câu trả lời ngắn nhưng có nỗ lực"}
        elif word_count < 12:
            return 5.5 + effort_bonus, {"reason": "Câu trả lời ở mức chấp nhận được"}
        elif word_count < 20:
            return 6.5 + effort_bonus, {"reason": "Câu trả lời tốt"}
        else:
            return min(7.5 + effort_bonus, 10.0), {"reason": "Câu trả lời chi tiết"}

    # ===== Có mẫu tham khảo → so sánh với base score floor =====
    BASE_SCORE = 3.0  # Điểm sàn cho người mới học (nếu có nỗ lực)

    best_score = 0
    best_similarity = 0
    best_match = None
    all_similarities = []

    for sample in sample_answers:
        content = sample.get('content', '')
        ref_score = float(sample.get('score', 7))

        similarity = compute_text_similarity(transcribed_text, content)

        # Công thức thân thiện beginner:
        # Thay vì: similarity * ref_score (quá khắt khe)
        # Dùng:    base + similarity * (ref_score - base)
        # VD: sim=0.3, ref=10 → cũ: 3.0 | mới: 3.0 + 0.3*(10-3) = 5.1
        # VD: sim=0.5, ref=10 → cũ: 5.0 | mới: 3.0 + 0.5*(10-3) = 6.5
        # VD: sim=0.7, ref=10 → cũ: 7.0 | mới: 3.0 + 0.7*(10-3) = 7.9
        computed = BASE_SCORE + similarity * (ref_score - BASE_SCORE)

        all_similarities.append({
            'sample_content': content[:80],
            'ref_score': ref_score,
            'similarity': round(similarity, 3),
            'computed_score': round(computed, 2)
        })

        if computed > best_score:
            best_score = computed
            best_similarity = similarity
            best_match = {
                'sample_content': content[:80],
                'ref_score': ref_score,
                'similarity': round(similarity, 3)
            }

    # Thêm bonus nỗ lực
    best_score += effort_bonus

    # Bonus thêm nếu câu trả lời dài và chi tiết
    if word_count >= 20:
        best_score = min(best_score + 0.3, 10.0)

    # Penalty nhẹ cho câu cực ngắn (nhưng vẫn nhẹ hơn trước)
    if word_count < 2:
        best_score = min(best_score, 3.0)
    elif word_count < 4:
        best_score = min(best_score, 5.0)

    best_score = max(0.0, min(10.0, round(best_score, 1)))

    return best_score, {
        'best_match': best_match,
        'best_similarity': best_similarity,
        'all_comparisons': all_similarities,
        'effort_bonus': effort_bonus,
        'word_count': word_count
    }


def generate_vietnamese_feedback(transcribed_text, sample_answers, algo_score, match_info):
    """
    Tạo feedback tiếng Việt — phong cách KHÍCH LỆ, phù hợp người mới học.
    Luôn có lời khen trước, sau đó mới gợi ý cải thiện.
    """
    feedback_parts = []
    words = normalize_text(transcribed_text).split()
    word_count = len(words)

    # === Phần 1: Lời khen/đánh giá tổng quan (luôn khích lệ) ===
    if algo_score >= 9.0:
        feedback_parts.append("Xuất sắc! Câu trả lời rất tốt và đầy đủ!")
    elif algo_score >= 7.5:
        feedback_parts.append("Rất tốt! Bạn đã trả lời khá hoàn chỉnh.")
    elif algo_score >= 6.0:
        feedback_parts.append("Tốt lắm! Bạn đang đi đúng hướng.")
    elif algo_score >= 4.5:
        feedback_parts.append("Khá được! Bạn đã có nỗ lực tốt trong câu trả lời.")
    elif algo_score >= 3.0:
        feedback_parts.append("Bạn đã cố gắng trả lời, đó là điều tốt! Hãy tiếp tục luyện tập nhé.")
    else:
        feedback_parts.append("Đừng nản nhé! Mỗi lần luyện tập đều giúp bạn tiến bộ.")

    # === Phần 2: Khen điểm tốt cụ thể ===
    if word_count >= 15:
        feedback_parts.append("Bạn đã trả lời khá chi tiết, rất tốt!")
    elif word_count >= 8:
        feedback_parts.append("Độ dài câu trả lời ở mức chấp nhận được.")

    if match_info and 'best_match' in match_info and match_info['best_match']:
        sim = match_info['best_match']['similarity']
        if sim >= 0.6:
            feedback_parts.append("Nội dung trả lời rất sát với yêu cầu!")
        elif sim >= 0.4:
            feedback_parts.append("Nội dung có nhiều ý đúng, tốt lắm!")
        elif sim >= 0.2:
            feedback_parts.append("Bạn đã nắm được một phần ý chính của câu hỏi.")

    # === Phần 3: Gợi ý cải thiện (nhẹ nhàng) ===
    suggestions = []
    if word_count < 5:
        suggestions.append("Thử trả lời dài hơn một chút — khoảng 2-3 câu sẽ rất tốt.")
    elif word_count < 10:
        suggestions.append("Bạn có thể thêm 1-2 câu nữa để câu trả lời đầy đủ hơn.")

    if match_info and 'best_match' in match_info and match_info['best_match']:
        sim = match_info['best_match']['similarity']
        if sim < 0.2:
            suggestions.append("Hãy đọc kỹ câu hỏi và tập trung trả lời đúng trọng tâm nhé.")
        elif sim < 0.4:
            suggestions.append("Thử bổ sung thêm ý chính liên quan đến câu hỏi.")

    if algo_score < 6.0:
        suggestions.append("Mẹo: Bắt đầu bằng 'I think...' hoặc 'In my opinion...' rồi nêu lý do sẽ giúp câu trả lời tốt hơn.")

    if suggestions:
        feedback_parts.append("Gợi ý cải thiện: " + " ".join(suggestions))

    # === Phần 4: Lời động viên cuối ===
    if algo_score < 7.0:
        feedback_parts.append("Hãy tiếp tục luyện tập, bạn sẽ tiến bộ nhanh thôi!")

    return " ".join(feedback_parts)


# ========== TRÍCH XUẤT JSON TỪ MODEL OUTPUT ==========

def extract_json_from_text(text):
    """Trích xuất JSON từ văn bản trả về của mô hình — có nhiều chiến lược fallback."""

    # Chiến lược 1: Tìm JSON block trong markdown code block
    code_block_match = re.search(r'```(?:json)?\s*(\{.*?\})\s*```', text, re.DOTALL)
    if code_block_match:
        try:
            result = json.loads(code_block_match.group(1))
            if 'score' in result:
                return result
        except json.JSONDecodeError:
            pass

    # Chiến lược 2: Tìm JSON object chứa "score"
    json_patterns = [
        r'\{[^{}]*"score"\s*:\s*[\d.]+[^{}]*"feedback"\s*:\s*"[^"]*"[^{}]*\}',
        r'\{[^{}]*"feedback"\s*:\s*"[^"]*"[^{}]*"score"\s*:\s*[\d.]+[^{}]*\}',
        r'\{[^{}]*"score"[^{}]*\}',
    ]
    for pattern in json_patterns:
        match = re.search(pattern, text, re.DOTALL | re.IGNORECASE)
        if match:
            try:
                result = json.loads(match.group())
                if 'score' in result:
                    return result
            except json.JSONDecodeError:
                pass

    # Chiến lược 3: Tìm bất kỳ JSON object nào
    try:
        all_json = re.findall(r'\{[^{}]+\}', text, re.DOTALL)
        for j in all_json:
            try:
                result = json.loads(j)
                if 'score' in result:
                    return result
            except json.JSONDecodeError:
                continue
    except:
        pass

    # Chiến lược 4: Trích xuất thủ công
    score_patterns = [
        r'"score"\s*:\s*(\d+\.?\d*)',
        r'score\s*[:=]\s*(\d+\.?\d*)',
        r'(\d+\.?\d*)\s*/\s*10',
        r'điểm\s*[:=]?\s*(\d+\.?\d*)',
    ]
    score = None
    for sp in score_patterns:
        m = re.search(sp, text, re.IGNORECASE)
        if m:
            score = float(m.group(1))
            if 0 <= score <= 10:
                break
            score = None

    feedback_patterns = [
        r'"feedback"\s*:\s*"((?:[^"\\]|\\.)*)"',
        r'"feedback"\s*:\s*\'((?:[^\'\\]|\\.)*)\'',
        r'feedback\s*[:=]\s*["\']?(.*?)(?:["\']?\s*\}|$)',
    ]
    feedback = None
    for fp in feedback_patterns:
        m = re.search(fp, text, re.IGNORECASE | re.DOTALL)
        if m:
            feedback = m.group(1).strip()
            if feedback:
                break

    if score is not None:
        return {"score": score, "feedback": feedback or text[:300]}

    # Hoàn toàn không parse được → trả None để dùng fallback
    return None

"""
Endpoint kiểm tra trạng thái server

Request:
    GET /health

Response:
    {
        "status": "healthy",
        "service": "qwen-server",
        "model": "Qwen/Qwen2.5-0.5B-Instruct",
        "device": "cuda/cpu"
    }
"""
# Kiểm tra dịch vụ có hoạt động không
@app.route('/health', methods=['GET'])
def health_check():
    return jsonify({
        'status': 'healthy',
        'service': 'qwen-server',
        'model': MODEL_NAME,
        'device': device,
        'scoring_weights': {'algorithm': ALGO_WEIGHT, 'model': MODEL_WEIGHT}
    })

"""
Chấm điểm câu trả lời sử dụng HYBRID SCORING:
  1. Tính điểm thuật toán (text similarity với mẫu)
  2. Tính điểm AI model (Qwen)
  3. Kết hợp: final_score = algo * ALGO_WEIGHT + model * MODEL_WEIGHT

Request JSON:
{
    "question": "The question text",
    "transcribedText": "Student's transcribed answer",
    "sample_answers": [
        {"content": "Sample answer 1", "score": 10},
        {"content": "Sample answer 2", "score": 8}
    ]
}

Response JSON:
{
    "score": 8.5,
    "feedback": "Nhận xét chi tiết bằng tiếng Việt",
    "algo_score": 7.2,
    "model_score": 9.0
}
"""
@app.route('/score', methods=['POST'])
def score_answer():
    try:
        data = request.get_json()
        if not data:
            return jsonify({'error': 'No data provided'}), 400

        question = data.get('question', '')
        transcribed_text = data.get('transcribedText', '')
        sample_answers = data.get('sample_answers', [])

        if not transcribed_text:
            return jsonify({'error': 'No transcribedText provided'}), 400

        logger.info(f"Scoring answer: '{transcribed_text[:80]}...'")
        t_start = time.time()

        # ===== CHECK CACHE =====
        cached = scoring_cache.get(question, transcribed_text, sample_answers)
        if cached:
            logger.info(f"Returning cached result in {time.time()-t_start:.3f}s")
            return jsonify(cached), 200

        # ===== BƯỚC 1: Tính điểm thuật toán =====
        t_algo = time.time()
        algo_score, match_info = compute_algorithmic_score(transcribed_text, sample_answers)
        logger.info(f"Algorithmic score: {algo_score} ({time.time()-t_algo:.3f}s)")

        # ===== BƯỚC 2: Tính điểm AI model =====
        model_score = None
        model_feedback = None

        try:
            # Prompt ngắn gọn — giảm input tokens để tăng tốc
            system_prompt = """Giáo viên tiếng Anh thân thiện. Chấm điểm KHOAN DUNG cho người mới học.
Khen trước, gợi ý sau. Feedback bằng tiếng Việt.
Điểm: cố gắng=4-5, đúng ý=6-7, tốt=7-9, không liên quan<3.
Chỉ trả về JSON: {"score": <0-10>, "feedback": "<tiếng Việt>"}""" 

            # Tạo phần mẫu tham khảo — ngắn gọn
            sample_text = ""
            if sample_answers:
                sample_text = "\nMẫu tham khảo:\n"
                for i, s in enumerate(sample_answers[:3], 1):  # Giới hạn 3 mẫu
                    content = s.get('content', '')[:150]  # Giới hạn độ dài
                    score = s.get('score', 'N/A')
                    sample_text += f"- Mẫu {i} ({score} điểm): {content}\n"

            user_message = f"""Q: {question}
{sample_text}
A: {transcribed_text}
Chấm điểm JSON:"""

            messages = [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_message}
            ]

            text = tokenizer.apply_chat_template(
                messages,
                tokenize=False,
                add_generation_prompt=True
            )
            model_inputs = tokenizer([text], return_tensors="pt").to(device)

            t_model = time.time()
            with torch.no_grad():  # Tắt gradient tracking → nhanh hơn + ít VRAM hơn
                generated_ids = model.generate(
                    model_inputs.input_ids,
                    max_new_tokens=200,   # JSON ngắn, không cần 1024 tokens
                    do_sample=False,
                    repetition_penalty=1.1,
                    # Dừng sớm khi gặp dấu đóng JSON
                    eos_token_id=tokenizer.eos_token_id,
                )
            model_time = time.time() - t_model

            generated_ids = [
                output_ids[len(input_ids):]
                for input_ids, output_ids in zip(model_inputs.input_ids, generated_ids)
            ]
            response_text = tokenizer.batch_decode(generated_ids, skip_special_tokens=True)[0]
            logger.info(f"Model output ({model_time:.2f}s, {len(generated_ids[0])} tokens): {response_text[:200]}")

            # Trích xuất kết quả
            parsed = extract_json_from_text(response_text)
            if parsed and 'score' in parsed:
                model_score = float(parsed['score'])
                model_score = max(0.0, min(10.0, model_score))
                model_feedback = parsed.get('feedback', '')
                logger.info(f"Model score: {model_score}, feedback: {model_feedback[:80]}")
            else:
                logger.warning(f"Failed to parse model output: {response_text[:200]}")

        except Exception as e:
            logger.error(f"Model scoring failed: {str(e)}")

        # ===== BƯỚC 3: Kết hợp điểm (hybrid) =====
        if model_score is not None:
            # Cả hai điểm đều có → kết hợp có trọng số
            final_score = round(algo_score * ALGO_WEIGHT + model_score * MODEL_WEIGHT, 1)

            # Kiểm tra chênh lệch quá lớn → ưu tiên thuật toán (model có thể sai)
            if abs(model_score - algo_score) > 4.0:
                logger.warning(f"Large score difference: algo={algo_score}, model={model_score}")
                # Nếu chênh quá nhiều, tăng trọng số thuật toán
                final_score = round(algo_score * 0.6 + model_score * 0.4, 1)
        else:
            # Model fail → chỉ dùng điểm thuật toán
            final_score = algo_score

        final_score = max(0.0, min(10.0, final_score))

        # ===== BƯỚC 4: Chọn feedback tốt nhất bằng tiếng Việt =====
        # Ưu tiên feedback từ model nếu có và hợp lệ
        feedback = None
        if model_feedback and len(model_feedback) > 10:
            # Kiểm tra xem feedback có vẻ là tiếng Việt không (có ký tự Unicode tiếng Việt)
            vn_chars = re.findall(r'[àáạảãăắằặẳẵâấầậẩẫèéẹẻẽêếềệểễìíịỉĩòóọỏõôốồộổỗơớờợởỡùúụủũưứừựửữỳýỵỷỹđ]',
                                  model_feedback.lower())
            if len(vn_chars) >= 2 or len(model_feedback) > 30:
                feedback = model_feedback

        if not feedback:
            # Model không cho feedback tiếng Việt → tự sinh
            feedback = generate_vietnamese_feedback(
                transcribed_text, sample_answers, final_score, match_info
            )

        result = {
            'score': final_score,
            'feedback': feedback,
            'algo_score': algo_score,
            'model_score': model_score
        }

        total_time = time.time() - t_start
        logger.info(f"Final: score={final_score}, algo={algo_score}, model={model_score} | Total: {total_time:.2f}s")

        # Lưu cache
        scoring_cache.put(question, transcribed_text, sample_answers, result)

        return jsonify(result), 200

    except Exception as e:
        logger.error(f"Error during scoring: {str(e)}")
        # Ngay cả khi lỗi, cố gắng trả về kết quả thuật toán
        try:
            fallback_score, fallback_info = compute_algorithmic_score(
                data.get('transcribedText', ''), data.get('sample_answers', [])
            )
            fallback_feedback = generate_vietnamese_feedback(
                data.get('transcribedText', ''), data.get('sample_answers', []),
                fallback_score, fallback_info
            )
            return jsonify({
                'score': fallback_score,
                'feedback': fallback_feedback,
                'algo_score': fallback_score,
                'model_score': None,
                'warning': 'Model failed, using algorithmic scoring only'
            }), 200
        except:
            return jsonify({
                'error': str(e),
                'score': 0.0,
                'feedback': 'Lỗi trong quá trình chấm điểm. Vui lòng thử lại.'
            }), 500

"""
Trò chuyện với mô hình Qwen.

Request JSON:
{
    "messages": [
        {"role": "system", "content": "..."},
        {"role": "user", "content": "..."}
    ]
}

Response JSON:
{
    "response": "AI response text"
}
"""
@app.route('/chat', methods=['POST'])
def chat():
    try:
        data = request.get_json()
        if not data or 'messages' not in data:
            return jsonify({'error': 'No messages provided'}), 400
        messages = data['messages']
        text = tokenizer.apply_chat_template(
            messages,
            tokenize=False,
            add_generation_prompt=True
        )
        model_inputs = tokenizer([text], return_tensors="pt").to(device)
        with torch.no_grad():
            generated_ids = model.generate(
                model_inputs.input_ids,
                max_new_tokens=512,
                temperature=0.7,
                top_p=0.9,
                do_sample=True
            )
        generated_ids = [output_ids[len(input_ids):] for input_ids, output_ids in zip(model_inputs.input_ids, generated_ids)]
        response_text = tokenizer.batch_decode(generated_ids, skip_special_tokens=True)[0]
        return jsonify({'response': response_text}), 200
    except Exception as e:
        logger.error(f"Error during chat: {str(e)}")
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    print("=" * 60)
    print("Qwen Server Starting (Hybrid Scoring)")
    print(f"Model: {MODEL_NAME}")
    print(f"Device: {device}")
    print(f"Scoring: algo_weight={ALGO_WEIGHT}, model_weight={MODEL_WEIGHT}")
    print("Port: 5001")
    print("Endpoints:")
    print("  - GET  /health")
    print("  - POST /score   (hybrid: algorithm + AI)")
    print("  - POST /chat")
    print("=" * 60)
    # threaded=True cho phép xử lý nhiều request đồng thời
    app.run(host='0.0.0.0', port=5001, debug=False, threaded=True)