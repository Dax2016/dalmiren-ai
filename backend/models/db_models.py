import time
import uuid
from sqlalchemy import Column, String, Integer, Float, Boolean, Text, ForeignKey, BigInteger
from sqlalchemy.orm import relationship
from database import Base

def generate_uuid():
    return str(uuid.uuid4())

class User(Base):
    __tablename__ = "users"

    id = Column(String(36), primary_key=True, default=generate_uuid)
    email = Column(String(255), unique=True, nullable=False, index=True)
    password_hash = Column(String(255), nullable=True)
    full_name = Column(String(255), nullable=True)
    created_at = Column(BigInteger, default=lambda: int(time.time() * 1000))

    conversations = relationship("Conversation", back_populates="user", cascade="all, delete-orphan")
    memories = relationship("Memory", back_populates="user", cascade="all, delete-orphan")
    child_profiles = relationship("ChildProfile", back_populates="user", cascade="all, delete-orphan")

class FamilyProfile(Base):
    __tablename__ = "family_profiles"

    id = Column(String(36), primary_key=True, default=generate_uuid)
    parent_id = Column(String(36), ForeignKey("users.id"), nullable=False)
    family_name = Column(String(255), nullable=False)
    created_at = Column(BigInteger, default=lambda: int(time.time() * 1000))

class ChildProfile(Base):
    __tablename__ = "child_profiles"

    id = Column(String(36), primary_key=True, default=generate_uuid)
    user_id = Column(String(36), ForeignKey("users.id"), nullable=False)
    name = Column(String(255), nullable=False)
    age_group = Column(String(50), nullable=False, default="EARLY_CHILDHOOD")
    birth_date = Column(String(50), nullable=True)
    notes = Column(Text, nullable=True)
    created_at = Column(BigInteger, default=lambda: int(time.time() * 1000))

    user = relationship("User", back_populates="child_profiles")
    memories = relationship("Memory", back_populates="child_profile", cascade="all, delete-orphan")

class Conversation(Base):
    __tablename__ = "conversations"

    id = Column(String(36), primary_key=True, default=generate_uuid)
    user_id = Column(String(36), ForeignKey("users.id"), nullable=False)
    child_id = Column(String(36), ForeignKey("child_profiles.id"), nullable=True)
    title = Column(String(255), nullable=False, default="Parenting Consultation")
    created_at = Column(BigInteger, default=lambda: int(time.time() * 1000))
    updated_at = Column(BigInteger, default=lambda: int(time.time() * 1000))

    user = relationship("User", back_populates="conversations")
    messages = relationship("Message", back_populates="conversation", cascade="all, delete-orphan")

class Message(Base):
    __tablename__ = "messages"

    id = Column(String(36), primary_key=True, default=generate_uuid)
    conversation_id = Column(String(36), ForeignKey("conversations.id"), nullable=False)
    sender = Column(String(20), nullable=False) # USER or ASSISTANT
    text = Column(Text, nullable=False)
    timestamp = Column(BigInteger, default=lambda: int(time.time() * 1000))
    provider_used = Column(String(50), nullable=True)
    model_used = Column(String(100), nullable=True)
    routing_reason = Column(String(255), nullable=True)

    conversation = relationship("Conversation", back_populates="messages")

class Memory(Base):
    __tablename__ = "memories"

    id = Column(String(36), primary_key=True, default=generate_uuid)
    user_id = Column(String(36), ForeignKey("users.id"), nullable=False)
    child_id = Column(String(36), ForeignKey("child_profiles.id"), nullable=True)
    memory_type = Column(String(50), nullable=False, default="SEMANTIC") # EPISODIC, SEMANTIC, PROFILE, TEMPORAL, PROCEDURAL, PREFERENCE
    category = Column(String(100), nullable=False, default="GENERAL")
    content = Column(Text, nullable=False)
    confidence = Column(Float, default=1.0)
    is_active = Column(Boolean, default=True)
    created_at = Column(BigInteger, default=lambda: int(time.time() * 1000))
    updated_at = Column(BigInteger, default=lambda: int(time.time() * 1000))

    user = relationship("User", back_populates="memories")
    child_profile = relationship("ChildProfile", back_populates="memories")

class ParentingGoal(Base):
    __tablename__ = "parenting_goals"

    id = Column(String(36), primary_key=True, default=generate_uuid)
    user_id = Column(String(36), ForeignKey("users.id"), nullable=False)
    child_id = Column(String(36), ForeignKey("child_profiles.id"), nullable=True)
    title = Column(String(255), nullable=False)
    description = Column(Text, nullable=True)
    status = Column(String(50), default="ACTIVE") # ACTIVE, COMPLETED, PAUSED
    target_date = Column(BigInteger, nullable=True)
    created_at = Column(BigInteger, default=lambda: int(time.time() * 1000))

class GuidanceSession(Base):
    __tablename__ = "guidance_sessions"

    id = Column(String(36), primary_key=True, default=generate_uuid)
    user_id = Column(String(36), ForeignKey("users.id"), nullable=False)
    child_id = Column(String(36), ForeignKey("child_profiles.id"), nullable=True)
    category = Column(String(50), nullable=False)
    situation_desc = Column(Text, nullable=False)
    guidance_title = Column(String(255), nullable=False)
    calm_response = Column(Text, nullable=False)
    practical_tip = Column(Text, nullable=False)
    timestamp = Column(BigInteger, default=lambda: int(time.time() * 1000))

class FollowUpRecord(Base):
    __tablename__ = "follow_up_records"

    id = Column(String(36), primary_key=True, default=generate_uuid)
    user_id = Column(String(36), ForeignKey("users.id"), nullable=False)
    child_id = Column(String(36), ForeignKey("child_profiles.id"), nullable=True)
    goal_id = Column(String(36), ForeignKey("parenting_goals.id"), nullable=True)
    notes = Column(Text, nullable=False)
    outcome = Column(String(100), nullable=True)
    timestamp = Column(BigInteger, default=lambda: int(time.time() * 1000))

class MemoryAuditEvent(Base):
    __tablename__ = "memory_audit_events"

    id = Column(String(36), primary_key=True, default=generate_uuid)
    user_id = Column(String(36), ForeignKey("users.id"), nullable=False)
    memory_id = Column(String(36), nullable=False)
    action = Column(String(50), nullable=False) # CREATED, UPDATED, DELETED, REVIEWED
    timestamp = Column(BigInteger, default=lambda: int(time.time() * 1000))

class ConsentRecord(Base):
    __tablename__ = "consent_records"

    id = Column(String(36), primary_key=True, default=generate_uuid)
    user_id = Column(String(36), ForeignKey("users.id"), nullable=False)
    consent_type = Column(String(100), nullable=False)
    granted = Column(Boolean, default=True)
    timestamp = Column(BigInteger, default=lambda: int(time.time() * 1000))


class RefreshToken(Base):
    """
    Stores only SHA-256 hashes of opaque refresh tokens.
    Raw refresh tokens must never be persisted.
    """

    __tablename__ = "refresh_tokens"

    id = Column(String(36), primary_key=True, default=generate_uuid)
    user_id = Column(
        String(36),
        ForeignKey("users.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    token_hash = Column(String(64), nullable=False, unique=True, index=True)
    expires_at = Column(BigInteger, nullable=False, index=True)
    created_at = Column(BigInteger, nullable=False, default=lambda: int(time.time() * 1000))
    revoked_at = Column(BigInteger, nullable=True)
    replaced_by_hash = Column(String(64), nullable=True)
