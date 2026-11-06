import time
import json
import logging
import httpx
from typing import Dict, Any, Optional
from config import settings
from providers.base import AIProvider, AIProviderRequest, AIProviderResponse, ProviderState
from prompts.prompt_registry import PromptRegistry

logger = logging.getLogger(__name__)

class LocalAIProvider(AIProvider):
    def __init__(self):
        self.base_url = settings.LOCAL_AI_BASE_URL.rstrip('/')
        self.model = settings.LOCAL_AI_MODEL
        self.timeout = settings.LOCAL_AI_TIMEOUT_SECONDS
        self.enabled = settings.LOCAL_AI_ENABLED

    @property
    def provider_name(self) -> str:
        return "LocalAI"

    def is_configured(self) -> bool:
        return self.enabled and bool(self.base_url)

    async def health_check(self) -> ProviderState:
        if not self.is_configured():
            return ProviderState.UNAVAILABLE
        try:
            async with httpx.AsyncClient(timeout=5.0) as client:
                res = await client.get(f"{self.base_url}/api/tags")
                if res.status_code == 200:
                    models_data = res.json()
                    models = [
                        str(m.get("name", "")).strip()
                        for m in models_data.get("models", [])
                        if isinstance(m, dict)
                    ]

                    configured_model = self.model.strip()
                    accepted_names = {
                        configured_model,
                        configured_model.removesuffix(":latest")
                    }

                    if any(
                        name in accepted_names
                        or name.removesuffix(":latest") in accepted_names
                        for name in models
                    ):
                        return ProviderState.AVAILABLE

                    logger.warning(
                        "Configured LocalAI model '%s' was not found. "
                        "Available models: %s",
                        configured_model,
                        models
                    )
                    return ProviderState.DEGRADED
                return ProviderState.UNAVAILABLE
        except Exception:
            return ProviderState.UNAVAILABLE

    async def generate_response(self, request: AIProviderRequest) -> AIProviderResponse:
        start_time = time.time()
        if not self.is_configured():
            raise RuntimeError("Local AI provider is disabled or not configured.")

        messages = []
        system_prompt = request.system_prompt or PromptRegistry.get_system_prompt()
        messages.append({"role": "system", "content": system_prompt})

        if request.memory_context:
            context_str = "Relevant Family & Child Context:\n" + "\n".join(f"- {c}" for c in request.memory_context)
            messages.append({"role": "system", "content": context_str})

        for msg in request.conversation_history:
            messages.append({"role": msg.get("role", "user"), "content": msg.get("content", "")})

        messages.append({"role": "user", "content": request.prompt})

        payload = {
            "model": self.model,
            "messages": messages,
            "stream": False,
            "options": {
                "temperature": request.temperature,
                "num_predict": request.max_tokens
            }
        }

        async with httpx.AsyncClient(timeout=float(self.timeout)) as client:
            response = await client.post(f"{self.base_url}/api/chat", json=payload)
            if response.status_code != 200:
                raise RuntimeError(f"Local AI HTTP error {response.status_code}: {response.text}")

            data = response.json()
            message_obj = data.get("message", {})
            response_text = message_obj.get("content", "").strip()

            latency_ms = (time.time() - start_time) * 1000.0
            eval_count = data.get("eval_count", 0)

            return AIProviderResponse(
                text=response_text,
                provider_name=self.provider_name,
                model_name=self.model,
                latency_ms=latency_ms,
                usage_tokens={"total_tokens": eval_count},
                is_fallback=False,
                routing_reason="Local AI inference succeeded",
                raw_metadata={"prompt_version": PromptRegistry.get_version()}
            )

    async def generate_structured_output(self, request: AIProviderRequest, schema: Dict[str, Any]) -> Dict[str, Any]:
        prompt_with_schema = (
            f"{request.prompt}\n\nPlease respond STRICTLY with a valid JSON object matching this schema:\n"
            f"{json.dumps(schema)}"
        )
        structured_request = request.model_copy(update={"prompt": prompt_with_schema})
        res = await self.generate_response(structured_request)
        try:
            text = res.text
            if "```json" in text:
                text = text.split("```json")[1].split("```")[0].strip()
            elif "```" in text:
                text = text.split("```")[1].split("```")[0].strip()
            return json.loads(text)
        except Exception:
            return {"raw_text": res.text}
