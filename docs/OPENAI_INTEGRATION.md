# OpenAI API Integration Architecture

## Overview
DALMIREN AI integrates the official OpenAI Python SDK (`openai>=1.14.0`) via an asynchronous provider class `OpenAIProvider` on the FastAPI backend.

## Security Controls
- **Zero Client Exposure**: The OpenAI API key (`OPENAI_API_KEY`) is stored strictly in `backend/.env`. It is NEVER compiled into Android binaries, `BuildConfig`, Kotlin code, network responses, or client logs.
- **Minimal Context Policy**: Only necessary parent prompts and anonymized child age group / memory context are transmitted to OpenAI.

## Configuration Settings
```env
OPENAI_ENABLED=true
OPENAI_API_KEY=your_private_openai_api_key_here
OPENAI_MODEL=gpt-4o-mini
OPENAI_TIMEOUT_SECONDS=60
```

## Health Checks
Lightweight health checks execute via `await client.models.list()`. This validates credentials and model accessibility without incurring costs or burning paid inference tokens.
