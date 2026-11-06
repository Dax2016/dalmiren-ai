import asyncio
import logging
from typing import Dict, Optional

from providers.base import AIProvider, ProviderState

logger = logging.getLogger("HealthMonitor")


class HealthMonitor:
    def __init__(self, providers: Dict[str, AIProvider]):
        self.providers = providers
        self.states: Dict[str, ProviderState] = {
            name: ProviderState.INITIALISING for name in providers
        }
        self._task: Optional[asyncio.Task] = None

    async def check_all_providers(self):
        for name, provider in self.providers.items():
            try:
                state = await provider.health_check()
                self.states[name] = state
            except Exception:
                logger.exception("Health check failed for provider %s", name)
                self.states[name] = ProviderState.UNAVAILABLE

    def get_provider_state(self, provider_name: str) -> ProviderState:
        return self.states.get(provider_name, ProviderState.UNAVAILABLE)

    def is_provider_available(self, provider_name: str) -> bool:
        return self.get_provider_state(provider_name) == ProviderState.AVAILABLE

    def get_overall_status(self) -> str:
        if any(state == ProviderState.INITIALISING for state in self.states.values()):
            return "STARTING"

        if any(state == ProviderState.AVAILABLE for state in self.states.values()):
            return "ONLINE"

        return "DEGRADED"

    def get_all_states(self) -> Dict[str, str]:
        return {
            name: state.value
            for name, state in self.states.items()
        }

    async def start_periodic_monitoring(self, interval_seconds: int = 30):
        if interval_seconds < 1:
            raise ValueError("Health check interval must be at least one second.")

        if self._task is not None and not self._task.done():
            logger.info("Health monitoring is already running.")
            return

        await self.check_all_providers()

        async def _loop():
            try:
                while True:
                    await asyncio.sleep(interval_seconds)
                    await self.check_all_providers()
            except asyncio.CancelledError:
                logger.info("Health monitoring task cancelled.")
                raise

        self._task = asyncio.create_task(_loop())

    async def stop_monitoring(self):
        task = self._task
        self._task = None

        if task is not None and not task.done():
            task.cancel()
            try:
                await task
            except asyncio.CancelledError:
                pass
