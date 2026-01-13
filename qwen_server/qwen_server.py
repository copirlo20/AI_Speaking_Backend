"""
Qwen Server - Local AI Scoring API
Uses Qwen model to score speaking test answers
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
from transformers import AutoModelForCausalLM, AutoTokenizer
import torch
import json
import re
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)
CORS(app)

# Load Qwen model
logger.info("Loading Qwen model...")
model_name = "Qwen/Qwen2.5-0.5B-Instruct"  # Using smaller model for local usage

tokenizer = AutoTokenizer.from_pretrained(model_name)
model = AutoModelForCausalLM.from_pretrained(
    model_name,
    torch_dtype=torch.float16 if torch.cuda.is_available() else torch.float32,
    device_map="auto" if torch.cuda.is_available() else None
)

device = "cuda" if torch.cuda.is_available() else "cpu"
if device == "cpu":
    model = model.to(device)

logger.info(f"Qwen model loaded successfully on {device}!")


def extract_json_from_text(text):
    """Extract JSON object from text response"""
    try:
        # Try to find JSON in the response
        json_match = re.search(r'\{[^{}]*"score"[^{}]*"feedback"[^{}]*\}', text, re.DOTALL)
        if json_match:
            json_str = json_match.group()
            return json.loads(json_str)
    except:
        pass
    
    # Fallback: extract score and feedback manually
    score_match = re.search(r'score["\s:]+(\d+\.?\d*)', text, re.IGNORECASE)
    feedback_match = re.search(r'feedback["\s:]+["\'](.*?)["\']', text, re.IGNORECASE | re.DOTALL)
    
    score = float(score_match.group(1)) if score_match else 5.0
    feedback = feedback_match.group(1) if feedback_match else text[:200]
    
    return {"score": score, "feedback": feedback}


@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint"""
    return jsonify({
        'status': 'healthy',
        'service': 'qwen-server',
        'model': model_name,
        'device': device
    })


@app.route('/score', methods=['POST'])
def score_answer():
    """
    Score a speaking test answer
    
    Expected JSON payload:
    {
        "system_prompt": "Scoring instructions and sample answers",
        "question": "The question text",
        "user_text": "Student's transcribed answer"
    }
    
    Returns:
    {
        "score": 8.5,
        "feedback": "Detailed feedback about the answer"
    }
    """
    try:
        data = request.get_json()
        
        if not data:
            return jsonify({'error': 'No data provided'}), 400
        
        system_prompt = data.get('system_prompt', '')
        question = data.get('question', '')
        user_text = data.get('user_text', '')
        
        if not user_text:
            return jsonify({'error': 'No user_text provided'}), 400
        
        # Construct the full prompt
        full_prompt = f"""{system_prompt}

Question: {question}

Student's Answer: {user_text}

Please score this answer and provide feedback in JSON format:
{{"score": <0-10>, "feedback": "<detailed feedback>"}}
"""
        
        logger.info(f"Scoring answer: {user_text[:50]}...")
        
        # Generate response
        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": f"Question: {question}\n\nStudent's Answer: {user_text}\n\nPlease score this answer (0-10) and provide detailed feedback in JSON format."}
        ]
        
        text = tokenizer.apply_chat_template(
            messages,
            tokenize=False,
            add_generation_prompt=True
        )
        
        model_inputs = tokenizer([text], return_tensors="pt").to(device)
        
        generated_ids = model.generate(
            model_inputs.input_ids,
            max_new_tokens=512,
            temperature=0.7,
            top_p=0.9,
            do_sample=True
        )
        
        generated_ids = [
            output_ids[len(input_ids):] for input_ids, output_ids in zip(model_inputs.input_ids, generated_ids)
        ]
        
        response_text = tokenizer.batch_decode(generated_ids, skip_special_tokens=True)[0]
        
        logger.info(f"Model response: {response_text[:100]}...")
        
        # Extract score and feedback
        result = extract_json_from_text(response_text)
        
        # Ensure score is within range
        if result['score'] > 10:
            result['score'] = 10.0
        elif result['score'] < 0:
            result['score'] = 0.0
        
        logger.info(f"Scoring successful: {result['score']}")
        
        return jsonify(result), 200
        
    except Exception as e:
        logger.error(f"Error during scoring: {str(e)}")
        return jsonify({
            'error': str(e),
            'score': 0.0,
            'feedback': 'Lỗi trong quá trình chấm điểm. Vui lòng thử lại.'
        }), 500


@app.route('/chat', methods=['POST'])
def chat():
    """
    General chat endpoint for testing
    
    Expected JSON payload:
    {
        "messages": [
            {"role": "system", "content": "..."},
            {"role": "user", "content": "..."}
        ]
    }
    
    Returns:
    {
        "response": "AI response text"
    }
    """
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
        
        generated_ids = model.generate(
            model_inputs.input_ids,
            max_new_tokens=512,
            temperature=0.7,
            top_p=0.9,
            do_sample=True
        )
        
        generated_ids = [
            output_ids[len(input_ids):] for input_ids, output_ids in zip(model_inputs.input_ids, generated_ids)
        ]
        
        response_text = tokenizer.batch_decode(generated_ids, skip_special_tokens=True)[0]
        
        return jsonify({'response': response_text}), 200
        
    except Exception as e:
        logger.error(f"Error during chat: {str(e)}")
        return jsonify({'error': str(e)}), 500


if __name__ == '__main__':
    print("=" * 60)
    print("Qwen Server Starting...")
    print(f"Model: {model_name}")
    print(f"Device: {device}")
    print("Port: 5001")
    print("Endpoints:")
    print("  - GET  /health")
    print("  - POST /score")
    print("  - POST /chat")
    print("=" * 60)
    
    app.run(host='0.0.0.0', port=5001, debug=False)
