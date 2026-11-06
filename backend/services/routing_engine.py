import logging
from enum import Enum
from typing import Dict, Any, Tuple, Optional
from providers.base import AIProvider, AIProviderRequest, AIProviderResponse, ProviderState
from services.health_monitor import HealthMonitor

logger = logging.getLogger("RoutingPolicyEngine")


class TaskIntent(str, Enum):
    ROUTINE_GUIDANCE = "ROUTINE_GUIDANCE"
    COMPLEX_REASONING = "COMPLEX_REASONING"
    SUMMARY_OR_TITLE = "SUMMARY_OR_TITLE"
    MEMORY_EXTRACTION = "MEMORY_EXTRACTION"
    SAFETY_SENSITIVE = "SAFETY_SENSITIVE"


class RoutingPolicyEngine:
    def __init__(self, health_monitor: HealthMonitor, local_provider: AIProvider, openai_provider: AIProvider):
        self.health_monitor = health_monitor
        self.local_provider = local_provider
        self.openai_provider = openai_provider

    def classify_intent(self, prompt: str, memory_count: int = 0, history_length: int = 0) -> TaskIntent:
        p_lower = prompt.lower()

        safety_keywords = [
            "suicide", "abuse", "self-harm", "hit me", "emergency",
            "poison", "hospital", "choking"
        ]
        if any(kw in p_lower for kw in safety_keywords):
            return TaskIntent.SAFETY_SENSITIVE

        complex_keywords = [
            "synthesize", "multi-step plan", "compare last week",
            "long term strategy", "multiple children", "complex conflict",
            "deep evaluation", "historical pattern"
        ]
        if any(kw in p_lower for kw in complex_keywords) or memory_count > 5 or history_length > 10:
            return TaskIntent.COMPLEX_REASONING

        if "summarize" in p_lower or "title" in p_lower or "short headline" in p_lower:
            return TaskIntent.SUMMARY_OR_TITLE

        if "extract memory" in p_lower or "identify key facts" in p_lower:
            return TaskIntent.MEMORY_EXTRACTION

        return TaskIntent.ROUTINE_GUIDANCE

    def select_provider(
        self,
        intent: TaskIntent,
        allow_cloud: bool = False
    ) -> Tuple[AIProvider, str]:
        # Privacy is the default: never select OpenAI without explicit consent.
        local_state = self.health_monitor.get_provider_state("LocalAI")

        if not allow_cloud:
            if local_state == ProviderState.AVAILABLE:
                return (
                    self.local_provider,
                    "Privacy restriction: local-only processing; cloud consent not granted."
                )
            raise RuntimeError(
                "Cloud processing is not permitted without explicit consent, "
                "and Local AI is unavailable."
            )

        local_state = self.health_monitor.get_provider_state("LocalAI")
        openai_state = self.health_monitor.get_provider_state("OpenAI")

        local_ok = local_state == ProviderState.AVAILABLE
        openai_ok = openai_state == ProviderState.AVAILABLE

        if intent == TaskIntent.COMPLEX_REASONING:
            if openai_ok:
                return self.openai_provider, "Rule D: Preferred OpenAI for complex reasoning"
            if local_ok:
                return self.local_provider, "Rule D fallback: OpenAI unavailable; using Local AI"

        if intent in (
            TaskIntent.ROUTINE_GUIDANCE,
            TaskIntent.SUMMARY_OR_TITLE,
            TaskIntent.MEMORY_EXTRACTION
        ):
            if local_ok:
                return self.local_provider, f"Rule {intent.name}: Preferred Local AI"
            if openai_ok:
                return self.openai_provider, f"Rule {intent.name} fallback: Local AI unavailable"

        if intent == TaskIntent.SAFETY_SENSITIVE:
            if local_ok:
                return self.local_provider, "Safety-sensitive request: Local AI selected"
            if openai_ok:
                return self.openai_provider, "Safety-sensitive request: OpenAI selected"

        if local_ok:
            return self.local_provider, "Default fallback to Local AI"
        if openai_ok:
            return self.openai_provider, "Default fallback to OpenAI"

        raise RuntimeError("No AI provider is currently available.")

    async def execute_routed_request(
        self,
        request: AIProviderRequest,
        memory_count: int = 0,
        history_length: int = 0,
        allow_cloud: bool = False,
        prefer_cloud: bool = False
    ) -> AIProviderResponse:
        intent = self.classify_intent(
            request.prompt,
            memory_count=memory_count,
            history_length=history_length
        )

        if not allow_cloud:
            if self.health_monitor.get_provider_state("LocalAI") != ProviderState.AVAILABLE:
                raise RuntimeError(
                    "Cloud processing is not permitted without explicit consent, "
                    "and Local AI is unavailable."
                )
            primary_provider = self.local_provider
            routing_reason = (
                "Privacy restriction: local-only processing; cloud consent not granted."
            )
        elif prefer_cloud:
            if self.health_monitor.get_provider_state("OpenAI") == ProviderState.AVAILABLE:
                primary_provider = self.openai_provider
                routing_reason = (
                    "Explicit cloud consent granted: OpenAI preferred for specialist reasoning."
                )
            elif self.health_monitor.get_provider_state("LocalAI") == ProviderState.AVAILABLE:
                primary_provider = self.local_provider
                routing_reason = (
                    "Explicit cloud consent granted, but OpenAI is unavailable; "
                    "using Local AI."
                )
            else:
                raise RuntimeError(
                    "No available AI provider can process this specialist request."
                )
        else:
            primary_provider, routing_reason = self.select_provider(intent, allow_cloud=True)

        try:
            response = await primary_provider.generate_response(request)
            response.routing_reason = routing_reason
            return response

        except Exception as primary_ex:
            logger.warning(
                "Primary provider %s failed: %s",
                primary_provider.provider_name,
                primary_ex
            )

            secondary_provider = (
                self.openai_provider
                if primary_provider.provider_name == "LocalAI"
                else self.local_provider
            )

            if secondary_provider.provider_name == "OpenAI" and not allow_cloud:
                raise RuntimeError(
                    "Local AI failed. Cloud fallback is blocked because explicit consent was not granted."
                ) from primary_ex

            if self.health_monitor.get_provider_state(
                secondary_provider.provider_name
            ) != ProviderState.AVAILABLE:
                raise RuntimeError(
                    f"Primary provider {primary_provider.provider_name} failed, "
                    "and the permitted fallback is unavailable."
                ) from primary_ex

            try:
                response = await secondary_provider.generate_response(request)
                response.is_fallback = True
                response.routing_reason = (
                    f"Fallback: {primary_provider.provider_name} failed; "
                    f"using {secondary_provider.provider_name}."
                )
                return response
            except Exception as secondary_ex:
                logger.error("Secondary provider failed: %s", secondary_ex)
                raise RuntimeError(
                    "Both the primary provider and permitted fallback failed."
                ) from secondary_ex
