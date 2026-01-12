"""
Local AI Service for Speech Recognition and Evaluation
Using OpenAI Whisper for transcription
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
import whisper
import os
import logging

app = Flask(__name__)
CORS(app)

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Load Whisper model (options: tiny, base, small, medium, large)
logger.info("Loading Whisper model...")
model = whisper.load_model("base")
logger.info("Model loaded successfully!")

@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint"""
    return jsonify({'status': 'healthy', 'model': 'whisper-base'})

@app.route('/api/transcribe', methods=['POST'])
def transcribe():
    """
    Transcribe audio file to text
    Expected input: JSON with 'audio_file' path
    """
    try:
        data = request.json
        audio_file = data.get('audio_file')
        
        if not audio_file or not os.path.exists(audio_file):
            return jsonify({'error': 'Audio file not found'}), 400
        
        logger.info(f"Transcribing audio file: {audio_file}")
        result = model.transcribe(audio_file)
        
        return jsonify({
            'text': result['text'],
            'language': result.get('language', 'en'),
            'segments': len(result.get('segments', []))
        })
    except Exception as e:
        logger.error(f"Transcription error: {str(e)}")
        return jsonify({'error': str(e)}), 500

@app.route('/api/evaluate', methods=['POST'])
def evaluate():
    """
    Evaluate speaking performance
    Expected input: transcription, question, sample_answer, criteria
    """
    try:
        data = request.json
        transcription = data.get('transcription', '')
        question = data.get('question', '')
        sample_answer = data.get('sample_answer', '')
        criteria = data.get('criteria', '')
        
        # Simple evaluation logic (can be enhanced with more sophisticated AI)
        word_count = len(transcription.split())
        
        # Basic scoring (you can enhance this with actual AI evaluation)
        pronunciation = min(10.0, 6.0 + (word_count / 20))
        fluency = min(10.0, 5.0 + (word_count / 15))
        grammar = 7.0  # Could use grammar checking AI
        vocabulary = min(10.0, 6.0 + (len(set(transcription.split())) / 15))
        content = 7.5  # Could use semantic similarity with sample answer
        
        return jsonify({
            'pronunciation': round(pronunciation, 1),
            'fluency': round(fluency, 1),
            'grammar': round(grammar, 1),
            'vocabulary': round(vocabulary, 1),
            'content': round(content, 1),
            'word_count': word_count
        })
    except Exception as e:
        logger.error(f"Evaluation error: {str(e)}")
        return jsonify({'error': str(e)}), 500

@app.route('/api/feedback', methods=['POST'])
def feedback():
    """
    Generate feedback based on transcription and scores
    """
    try:
        data = request.json
        transcription = data.get('transcription', '')
        scores = data.get('scores', {})
        
        avg_score = sum(scores.values()) / len(scores) if scores else 0
        
        # Generate feedback based on scores
        feedback_text = f"Overall performance: {avg_score:.1f}/10. "
        
        if avg_score >= 8:
            feedback_text += "Excellent work! Your speaking skills are very strong. "
        elif avg_score >= 6:
            feedback_text += "Good effort! You're making solid progress. "
        else:
            feedback_text += "Keep practicing! There's room for improvement. "
        
        # Specific feedback
        if scores.get('pronunciation', 0) < 6:
            feedback_text += "Focus on pronunciation and clarity. "
        if scores.get('fluency', 0) < 6:
            feedback_text += "Try to speak more smoothly and naturally. "
        if scores.get('vocabulary', 0) < 6:
            feedback_text += "Expand your vocabulary range. "
        if scores.get('grammar', 0) < 6:
            feedback_text += "Pay attention to grammar structures. "
        
        return jsonify({
            'feedback': feedback_text,
            'suggestions': [
                'Practice speaking regularly',
                'Listen to native speakers',
                'Record yourself and review',
                'Focus on weak areas identified'
            ]
        })
    except Exception as e:
        logger.error(f"Feedback generation error: {str(e)}")
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    logger.info("Starting AI Service on http://localhost:5000")
    app.run(host='0.0.0.0', port=5000, debug=True)
