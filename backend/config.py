import os
from pathlib import Path
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    HOST: str = "0.0.0.0"
    PORT: int = 8000
    DEBUG: bool = True

    # Local AI Provider Settings
    LOCAL_AI_ENABLED: bool = True
    LOCAL_AI_BASE_URL: str = "http://127.0.0.1:11434"
    LOCAL_AI_MODEL: str = "gemma3:4b"
    LOCAL_AI_TIMEOUT_SECONDS: int = 60

    # OpenAI Provider Settings
    OPENAI_ENABLED: bool = True
    OPENAI_API_KEY: str = ""
    OPENAI_MODEL: str = "gpt-4o-mini"
    OPENAI_TIMEOUT_SECONDS: int = 60

    # Database Settings
    DATABASE_URL: str = "sqlite:///./dalmiren.db"

    # Authentication Settings
    AUTH_JWT_SECRET: str = ""
    AUTH_JWT_ISSUER: str = "dalmiren-ai"
    AUTH_JWT_AUDIENCE: str = "dalmiren-android"
    AUTH_ACCESS_TOKEN_MINUTES: int = 15
    AUTH_REFRESH_TOKEN_DAYS: int = 30

    # Google Play Billing Settings
    GOOGLE_PLAY_PACKAGE_NAME: str = "com.darchums.dalmiren"
    GOOGLE_PLAY_SERVICE_ACCOUNT_FILE: str = ""
    GOOGLE_PLAY_APPLICATION_NAME: str = "DALMIREN AI"
    GOOGLE_PLAY_ACCOUNT_BINDING_SECRET: str = ""
    # Health Check Interval
    HEALTH_CHECK_INTERVAL_SECONDS: int = 30

    model_config = SettingsConfigDict(
        env_file=str(Path(__file__).resolve().parent / ".env"),
        env_file_encoding="utf-8",
        extra="ignore",
    )

settings = Settings()
