from abc import ABC, abstractmethod
from enum import Enum
from typing import List, Dict, Any, Optional
from pydantic import BaseModel, Field

class ProviderState(str, Enum):
    INITIALISING = "INITIALISING"
    AVAILABLE = "AVAILABLE"
    DEGRADED = "DEGRADED"
    UNAVAILABLE = "UNAVAILABLE"
    MISCONFIGURED = "MISCONFIGURED"

class AIProviderRequest(BaseModel):
    prompt: str
    system_prompt: Optional[str] = None
    conversation_history: List[Dict[str, str]] = Field(default_factory=list)
    memory_context: List[str] = Field(default_factory=list)
    temperature: float = 0.7
    max_tokens: int = 1000

class AIProviderResponse(BaseModel):
    text: str
    provider_name: str
    model_name: str
    latency_ms: float
    usage_tokens: Optional[Dict[str, int]] = None
    is_fallback: bool = False
    routing_reason: str = "Standard routing"
    raw_metadata: Dict[str, Any] = Field(default_factory=dict)

class AIProvider(ABC):
    @property
    @abstractmethod
    def provider_name(self) -> str:
        pass

    @abstractmethod
    def is_configured(self) -> bool:
        pass

    @abstractmethod
    async def health_check(self) -> ProviderState:
        pass

    @abstractmethod
    async def generate_response(self, request: AIProviderRequest) -> AIProviderResponse:
        pass

    @abstractmethod
    async def generate_structured_output(self, request: AIProviderRequest, schema: Dict[str, Any]) -> Dict[str, Any]:
        pass
