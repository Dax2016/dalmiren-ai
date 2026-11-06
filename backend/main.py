import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from database import engine, Base
from config import settings
from providers.local_ai import LocalAIProvider
from providers.openai_provider import OpenAIProvider
from services.health_monitor import HealthMonitor
from services.routing_engine import RoutingPolicyEngine
from routers import chat, guidance, profiles, memories, goals, consent, auth

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("DALMIREN-Backend")

# Global instances
local_provider = LocalAIProvider()
openai_provider = OpenAIProvider()
health_monitor = HealthMonitor({
    "LocalAI": local_provider,
    "OpenAI": openai_provider
})
routing_engine = RoutingPolicyEngine(health_monitor, local_provider, openai_provider)

@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Initializing DALMIREN AI Backend Database...")
    Base.metadata.create_all(bind=engine)

    logger.info("Starting AI Provider Health Monitoring...")
    await health_monitor.start_periodic_monitoring(settings.HEALTH_CHECK_INTERVAL_SECONDS)

    yield

    logger.info("Stopping Health Monitoring...")
    await health_monitor.stop_monitoring()

app = FastAPI(
    title="DALMIREN AI Backend",
    description="Master Hybrid Intelligence & Persistent Memory Nexus Backend",
    version="1.0.0",
    lifespan=lifespan
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Health & Provider Status Endpoint
@app.get("/api/health")
def health_check():
    return {
        "status": health_monitor.get_overall_status(),
        "brand": "DALMIREN AI by DARCHUMS",
        "tagline": "Understand. Guide. Nurture.",
        "providers": health_monitor.get_all_states(),
        "routing_policy": "Zero User Selection Intent Routing Engine"
    }

app.include_router(chat.router)
app.include_router(guidance.router)
app.include_router(profiles.router)
app.include_router(memories.router)
app.include_router(goals.router)
app.include_router(consent.router)
app.include_router(auth.router)
