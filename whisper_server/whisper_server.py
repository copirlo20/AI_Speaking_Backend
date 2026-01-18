"""
Whisper Server - API chuyển giọng nói thành văn bản (Speech-to-Text)
Sử dụng mô hình Whisper của OpenAI để chuyển đổi file âm thanh thành văn bản
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
import whisper
import base64
import os
import logging
import uuid
from pathlib import Path
import soundfile as sf
import numpy as np

# Cấu hình logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Khởi tạo Flask app
app = Flask(__name__)
CORS(app)

# Tạo thư mục temp để lưu file audio tạm thời
TEMP_DIR = Path('temp')
TEMP_DIR.mkdir(exist_ok=True)

# Load mô hình Whisper
logger.info("Whisper load model...")
model = whisper.load_model("medium", download_root=".")
logger.info("Whisper model loaded successfully!")

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
        # Tạo tên file unique cho mỗi request để xử lý đồng thời nhiều request
        unique_id = str(uuid.uuid4())
        temp_audio_path = TEMP_DIR / f'audio_{unique_id}.wav'
        data = request.get_json(silent=True)
        if data and 'audio_data' in data:
            audio_data = data['audio_data']
            if isinstance(audio_data, str):
                audio_bytes = base64.b64decode(audio_data)
            else:
                audio_bytes = audio_data
            # Ghi audio vào file tạm thời unique
            with open(str(temp_audio_path), 'wb') as f:
                f.write(audio_bytes)
        elif 'audio' in request.files:
            audio_file = request.files['audio']
            # Lưu file audio vào file tạm thời unique
            audio_file.save(str(temp_audio_path))
        else:
            return jsonify({'error': 'No audio data provided'}), 400
        logger.info(f"Processing audio file: {temp_audio_path}")
        if not os.path.exists(str(temp_audio_path)):
            raise FileNotFoundError(str(temp_audio_path))
        # Lấy đường dẫn tuyệt đối
        abs_path = os.path.abspath(str(temp_audio_path))
        # Kiểm tra kích thước file
        file_size = os.path.getsize(abs_path)
        logger.info(f"Audio file size: {file_size} bytes")
        logger.info(f"Absolute path: {abs_path}")
        if file_size == 0:
            raise ValueError("Audio file is empty")
        # Thử chuyển đổi audio sang text
        try:
            # Load audio bằng soundfile
            audio_data, sample_rate = sf.read(abs_path)
            # Chuyển sang mono nếu là stereo
            if len(audio_data.shape) > 1:
                audio_data = np.mean(audio_data, axis=1)
            # Đảm bảo định dạng float32
            audio_data = audio_data.astype(np.float32)
            logger.info(f"Audio loaded: sample_rate={sample_rate}, duration={len(audio_data)/sample_rate:.2f}s")
            # Chuyển đổi sang text (fp16=False để tương thích CPU)
            result = model.transcribe(audio_data, language='en', fp16=False)
        except Exception as transcribe_error:
            logger.error(f"Transcription failed: {type(transcribe_error).__name__}: {str(transcribe_error)}")
            raise
        # Xóa file tạm thời sau khi xử lý xong
        try:
            os.unlink(str(temp_audio_path))
            logger.info(f"Cleaned up temp file: {temp_audio_path}")
        except Exception as cleanup_error:
            logger.warning(f"Failed to cleanup temp file: {cleanup_error}")
        return jsonify({
            'transcribedText': result['text'].strip(),
            'language': result.get('language', 'en'),
            'segments': len(result.get('segments', []))
        }), 200
    except Exception as e:
        logger.error(f"Error during transcription: {str(e)}")
        # Xóa file tạm thời nếu có lỗi
        if temp_audio_path and os.path.exists(str(temp_audio_path)):
            try:
                os.unlink(str(temp_audio_path))
            except:
                pass
        return jsonify({'error': str(e)}), 500

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
        # Tạo tên file unique cho request này
        unique_id = str(uuid.uuid4())
        temp_audio_path = TEMP_DIR / f'audio_{unique_id}.wav'
        # Lưu vào file tạm thời unique
        audio_file.save(str(temp_audio_path))
        logger.info(f"Processing audio file: {audio_file.filename} (ID: {unique_id})")
        # Chuyển đổi bằng Whisper
        if language:
            result = model.transcribe(str(temp_audio_path), language=language)
        else:
            result = model.transcribe(str(temp_audio_path))
        # Xóa file tạm thời
        try:
            os.unlink(str(temp_audio_path))
            logger.info(f"Cleaned up temp file: {temp_audio_path}")
        except Exception as cleanup_error:
            logger.warning(f"Failed to cleanup temp file: {cleanup_error}")
        response = {
            'text': result['text'].strip(),
            'language': result.get('language', 'unknown'),
            'segments': [
                {
                    'start': seg['start'],
                    'end': seg['end'],
                    'text': seg['text']
                }
                for seg in result.get('segments', [])
            ]
        }
        logger.info(f"Transcription successful: {response['text'][:50]}...")
        return jsonify(response), 200
    except Exception as e:
        logger.error(f"Error during transcription: {str(e)}")
        # Xóa file tạm thời nếu có lỗi
        if temp_audio_path and os.path.exists(str(temp_audio_path)):
            try:
                os.unlink(str(temp_audio_path))
            except:
                pass
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    print("=" * 60)
    print("Whisper Server Starting...")
    print("Model: medium")
    print("Port: 5000")
    print("Endpoints:")
    print("  - GET  /health")
    print("  - POST /transcribe")
    print("  - POST /transcribe-file")
    print("=" * 60)
    app.run(host='0.0.0.0', port=5000, debug=False)