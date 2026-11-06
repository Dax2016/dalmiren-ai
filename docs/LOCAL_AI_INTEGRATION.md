# Local AI Inference Integration

## Overview
DALMIREN AI supports local inference runtimes such as Ollama using `LocalAIProvider`. This provides zero-latency, private, on-device/local-network inference for routine parenting advice.

## Configuration Settings
```env
LOCAL_AI_ENABLED=true
LOCAL_AI_BASE_URL=http://127.0.0.1:11434
LOCAL_AI_MODEL=llama3.2
LOCAL_AI_TIMEOUT_SECONDS=60
```

## Setup & Model Verification
1. Download and install Ollama from [ollama.com](https://ollama.com).
2. Pull your preferred parenting inference model:
   ```bash
   ollama pull llama3.2
   ```
3. Start the Ollama server:
   ```bash
   ollama serve
   ```
4. The backend automatically queries `GET http://127.0.0.1:11434/api/tags` to verify reachability and model availability.
