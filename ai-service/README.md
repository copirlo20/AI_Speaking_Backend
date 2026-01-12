# AI Service for Speech Recognition

## Installation

1. Install Python 3.8 or higher

2. Install dependencies:
```bash
pip install -r requirements.txt
```

3. Run the service:
```bash
python whisper_service.py
```

The service will start on `http://localhost:5000`

## Endpoints

- `GET /health` - Health check
- `POST /api/transcribe` - Transcribe audio to text
- `POST /api/evaluate` - Evaluate speaking performance
- `POST /api/feedback` - Generate feedback

## Testing

```bash
curl http://localhost:5000/health
```
