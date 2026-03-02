"""
Whisper Server - API chuyển giọng nói thành văn bản (Speech-to-Text)
Sử dụng openai-whisper để chuyển đổi file âm thanh thành văn bản
trực tiếp từ file medium.pt local, không cần tải từ Internet
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
import whisper
import torch
import base64
import os
import io
import logging
import uuid
from pathlib import Path

# Cấu hình logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Khởi tạo Flask app
app = Flask(__name__)
CORS(app)

# Tạo thư mục temp để lưu file audio tạm thời
TEMP_DIR = Path('temp')
TEMP_DIR.mkdir(exist_ok=True)

# Auto-detect thiết bị
if torch.cuda.is_available():
    DEVICE = "cuda"
    FP16 = True  # GPU: dùng fp16 cho tốc độ tối đa
    logger.info(f"GPU detected: {torch.cuda.get_device_name(0)}")
else:
    DEVICE = "cpu"
    FP16 = False  # CPU: không hỗ trợ fp16
    logger.info("No GPU detected, using CPU")

# === Load model LOCAL từ medium.pt ===
PT_MODEL_PATH = Path('medium.pt')  # File model gốc OpenAI Whisper

if not PT_MODEL_PATH.exists():
    raise FileNotFoundError(
        f"{PT_MODEL_PATH} not found! "
        "Hãy đặt file medium.pt vào thư mục cùng cấp với whisper_server.py."
    )

logger.info(f"Loading model from {PT_MODEL_PATH} (device={DEVICE})...")
model = whisper.load_model(str(PT_MODEL_PATH), device=DEVICE)
logger.info(f"Model loaded successfully from LOCAL: {PT_MODEL_PATH}")

"""
Endpoint kiểm tra trạng thái server

Request:
    GET /health

Response:
    {
        "status": "healthy",
        "service": "whisper-server",
        "model": "medium"
    }
"""
@app.route('/health', methods=['GET'])
def health_check():
    return jsonify({
        'status': 'healthy',
        'service': 'whisper-server',
        'model': 'medium'
    })

"""
Endpoint chuyển đổi audio thành text

Request:
    POST /transcribe
    Content-Type: application/json
    Body:
        {
            "audio_data": "<base64_encoded_audio>"  // Audio đã encode base64
        }
    
    Hoặc
    
    POST /transcribe
    Content-Type: multipart/form-data
    Form Data:
        audio: <file>  // File audio trực tiếp

Response (Success - 200):
    {
        "transcribedText": "Nội dung văn bản được chuyển đổi",
        "language": "en",
        "segments": 5  // Số lượng đoạn văn bản
    }

Response (Error - 400/500):
    {
        "error": "Mô tả lỗi"
    }
"""
@app.route('/transcribe', methods=['POST'])
def transcribe_audio():
    temp_audio_path = None
    try:
        data = request.get_json(silent=True)
        temp_audio_path = TEMP_DIR / f"{uuid.uuid4()}.wav"

        # Xử lý audio input → lưu vào file tạm
        if data and 'audio_data' in data:
            audio_data = data['audio_data']
            if isinstance(audio_data, str):
                audio_bytes = base64.b64decode(audio_data)
            else:
                audio_bytes = audio_data
            temp_audio_path.write_bytes(audio_bytes)
        elif 'audio' in request.files:
            audio_file = request.files['audio']
            audio_file.save(str(temp_audio_path))
        else:
            return jsonify({'error': 'No audio data provided'}), 400

        # Transcribe bằng openai-whisper — cân bằng tốc độ + accuracy
        result = model.transcribe(
            str(temp_audio_path),
            language='en',
            fp16=FP16,
            beam_size=5,                          # Beam search cho accuracy cao hơn
            best_of=3,                            # Chọn kết quả tốt nhất từ 3 candidates
            patience=1.5,                         # Beam search patience — tìm kỹ hơn
            temperature=(0.0, 0.2, 0.4, 0.6, 0.8, 1.0),  # Fallback temperatures
            compression_ratio_threshold=2.4,      # Lọc segment bị lặp/rác
            logprob_threshold=-1.0,               # Lọc segment confidence thấp
            no_speech_threshold=0.6,              # Phát hiện im lặng chính xác hơn
            condition_on_previous_text=True,       # Dùng context trước để decode tốt hơn
        )

        full_text = result['text'].strip()
        segments = result.get('segments', [])
        language = result.get('language', 'en')

        logger.info(f"Transcription done: {len(segments)} segments, lang={language}")

        return jsonify({
            'transcribedText': full_text,
            'language': language,
            'segments': len(segments)
        }), 200

    except Exception as e:
        logger.error(f"Error during transcription: {str(e)}")
        return jsonify({'error': str(e)}), 500
    finally:
        # Xóa file tạm
        if temp_audio_path and temp_audio_path.exists():
            temp_audio_path.unlink(missing_ok=True)

"""
Endpoint chuyển đổi file audio thành text (có thông tin chi tiết hơn)

Request:
    POST /transcribe-file
    Content-Type: multipart/form-data
    Form Data:
        - audio: <file>           // File audio (bắt buộc)
        - language: <string>      // Mã ngôn ngữ (tùy chọn, vd: 'en', 'vi')

Response (Success - 200):
    {
        "text": "Nội dung văn bản được chuyển đổi",
        "language": "en",
        "segments": [
            {
                "start": 0.0,
                "end": 2.5,
                "text": "Đoạn văn bản 1"
            },
            {
                "start": 2.5,
                "end": 5.0,
                "text": "Đoạn văn bản 2"
            }
        ]
    }

Response (Error - 400/500):
    {
        "error": "Mô tả lỗi"
    }
"""
@app.route('/transcribe-file', methods=['POST'])
def transcribe_file():
    temp_audio_path = None
    try:
        if 'audio' not in request.files:
            return jsonify({'error': 'No audio file provided'}), 400

        audio_file = request.files['audio']
        language = request.form.get('language', None)

        logger.info(f"Processing audio file: {audio_file.filename}")

        # Lưu file tạm
        temp_audio_path = TEMP_DIR / f"{uuid.uuid4()}.wav"
        audio_file.save(str(temp_audio_path))

        # Transcribe bằng openai-whisper — cân bằng tốc độ + accuracy
        transcribe_params = dict(
            fp16=FP16,
            beam_size=5,
            best_of=3,
            patience=1.5,
            temperature=(0.0, 0.2, 0.4, 0.6, 0.8, 1.0),
            compression_ratio_threshold=2.4,
            logprob_threshold=-1.0,
            no_speech_threshold=0.6,
            condition_on_previous_text=True,
        )
        if language:
            transcribe_params['language'] = language

        result = model.transcribe(str(temp_audio_path), **transcribe_params)

        # Collect segments
        segment_list = []
        for seg in result.get('segments', []):
            segment_list.append({
                'start': seg['start'],
                'end': seg['end'],
                'text': seg['text'].strip()
            })

        full_text = result['text'].strip()
        detected_language = result.get('language', language or 'unknown')
        logger.info(f"Transcription successful: {full_text[:50]}...")

        return jsonify({
            'text': full_text,
            'language': detected_language,
            'segments': segment_list
        }), 200

    except Exception as e:
        logger.error(f"Error during transcription: {str(e)}")
        return jsonify({'error': str(e)}), 500
    finally:
        # Xóa file tạm
        if temp_audio_path and temp_audio_path.exists():
            temp_audio_path.unlink(missing_ok=True)

if __name__ == '__main__':
    from waitress import serve

    print("=" * 60)
    print("Whisper Server Starting (openai-whisper + waitress)")
    print(f"Device: {DEVICE} | FP16: {FP16}")
    print(f"CPU threads: {os.cpu_count()}")
    print("Port: 5000")
    print("Endpoints:")
    print("  - GET  /health")
    print("  - POST /transcribe")
    print("  - POST /transcribe-file")
    print("=" * 60)

    # Waitress: production WSGI server, multi-threaded, Windows compatible
    serve(app, host='0.0.0.0', port=5000, threads=4)