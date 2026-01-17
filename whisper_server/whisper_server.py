"""
Whisper Server - Local Speech-to-Text API
Converts audio files to text using OpenAI's Whisper model
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
import whisper
import base64
import tempfile
import os
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)
CORS(app)

# Load Whisper model (you can choose: tiny, base, small, medium, large)
# 'base' is a good balance between speed and accuracy for local usage
logger.info("Loading Whisper model...")
model = whisper.load_model("medium", download_root=".")
logger.info("Whisper model loaded successfully!")


@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint"""
    return jsonify({
        'status': 'healthy',
        'service': 'whisper-server',
        'model': 'base'
    })


@app.route('/transcribe', methods=['POST'])
def transcribe_audio():
    try:
        # Use fixed temp file path instead of creating new temp files
        temp_audio_path = 'recorded.wav'
        data = request.get_json(silent=True)

        if data and 'audio_data' in data:
            audio_data = data['audio_data']

            if isinstance(audio_data, str):
                audio_bytes = base64.b64decode(audio_data)
            else:
                audio_bytes = audio_data

            # Write directly to fixed file (overwrite)
            with open(temp_audio_path, 'wb') as f:
                f.write(audio_bytes)

        elif 'audio' in request.files:
            audio_file = request.files['audio']

            # Save directly to fixed file (overwrite)
            audio_file.save(temp_audio_path)

        else:
            return jsonify({'error': 'No audio data provided'}), 400

        logger.info(f"Processing audio file: {temp_audio_path}")

        if not os.path.exists(temp_audio_path):
            raise FileNotFoundError(temp_audio_path)

        result = model.transcribe(temp_audio_path, language='en')

        # Don't delete the file, just keep it for next request to overwrite

        return jsonify({
            'transcribedText': result['text'].strip(),
            'language': result.get('language', 'en'),
            'segments': len(result.get('segments', []))
        }), 200

    except Exception as e:
        logger.error(f"Error during transcription: {str(e)}")
        return jsonify({'error': str(e)}), 500


@app.route('/transcribe-file', methods=['POST'])
def transcribe_file():
    """
    Transcribe audio file directly from multipart/form-data
    
    Form data:
    - audio: audio file
    - language: (optional) language code
    
    Returns:
    {
        "text": "transcribed text",
        "language": "detected language"
    }
    """
    try:
        if 'audio' not in request.files:
            return jsonify({'error': 'No audio file provided'}), 400
        
        audio_file = request.files['audio']
        language = request.form.get('language', None)
        
        # Save to temporary file
        with tempfile.NamedTemporaryFile(delete=False, suffix='.wav') as temp_audio:
            audio_file.save(temp_audio.name)
            temp_audio_path = temp_audio.name
        
        logger.info(f"Processing audio file: {audio_file.filename}")
        
        # Transcribe with Whisper
        if language:
            result = model.transcribe(temp_audio_path, language=language)
        else:
            result = model.transcribe(temp_audio_path)
        
        # Clean up temporary file
        os.unlink(temp_audio_path)
        
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
        return jsonify({'error': str(e)}), 500


if __name__ == '__main__':
    print("=" * 60)
    print("Whisper Server Starting...")
    print("Model: base")
    print("Port: 5000")
    print("Endpoints:")
    print("  - GET  /health")
    print("  - POST /transcribe")
    print("  - POST /transcribe-file")
    print("=" * 60)
    
    app.run(host='0.0.0.0', port=5000, debug=False)
