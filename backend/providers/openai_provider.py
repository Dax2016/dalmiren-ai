import time
import json
from typing import Dict, Any, Optional
from openai import AsyncOpenAI
from config import settings
from providers.base import AIProvider, AIProviderRequest, AIProviderResponse, ProviderState
from prompts.prompt_registry import PromptRegistry

class OpenAIProvider(AIProvider):
    def __init__(self):
        self.api_key = settings.OPENAI_API_KEY
        self.model = settings.OPENAI_MODEL
        self.timeout = settings.OPENAI_TIMEOUT_SECONDS
        self.enabled = settings.OPENAI_ENABLED
        self.client: Optional[AsyncOpenAI] = None
        if self.api_key:
            self.client = AsyncOpenAI(api_key=self.api_key, timeout=float(self.timeout))

    @property
    def provider_name(self) -> str:
        return "OpenAI"

    def is_configured(self) -> bool:
        return self.enabled and bool(self.api_key) and self.api_key != "your_private_openai_api_key_here"

    async def health_check(self) -> ProviderState:
        if not self.is_configured():
            return ProviderState.MISCONFIGURED
        if not self.client:
            return ProviderState.UNAVAILABLE
        try:
            await self.client.models.list()
            return ProviderState.AVAILABLE
        except Exception as ex:
            err_msg = str(ex).lower()
            if "auth" in err_msg or "key" in err_msg or "401" in err_msg:
                return ProviderState.MISCONFIGURED
            return ProviderState.UNAVAILABLE

    async def generate_response(self, request: AIProviderRequest) -> AIProviderResponse:
        start_time = time.time()
        if not self.is_configured() or not self.client:
            raise RuntimeError("OpenAI provider is disabled or missing API key.")

        messages = []
        system_prompt = request.system_prompt or PromptRegistry.get_system_prompt()
        messages.append({"role": "system", "content": system_prompt})

        if request.memory_context:
            context_str = "Relevant Family & Child Context:\n" + "\n".join(f"- {c}" for c in request.memory_context)
            messages.append({"role": "system", "content": context_str})

        for msg in request.conversation_history:
            messages.append({"role": msg.get("role", "user"), "content": msg.get("content", "")})

        messages.append({"role": "user", "content": request.prompt})

        response = await self.client.chat.completions.create(
            model=self.model,
            messages=messages,
            temperature=request.temperature,
            max_tokens=request.max_tokens
        )

        latency_ms = (time.time() - start_time) * 1000.0
        text = response.choices[0].message.content or ""

        usage = {}
        if response.usage:
            usage = {
                "prompt_tokens": response.usage.prompt_tokens,
                "completion_tokens": response.usage.completion_tokens,
                "total_tokens": response.usage.total_tokens
            }

        return AIProviderResponse(
            text=text.strip(),
            provider_name=self.provider_name,
            model_name=self.model,
            latency_ms=latency_ms,
            usage_tokens=usage,
            is_fallback=False,
            routing_reason="OpenAI cloud inference succeeded",
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
