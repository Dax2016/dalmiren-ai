import time
from typing import List, Optional, Dict, Any
from sqlalchemy.orm import Session
from models.db_models import Memory, MemoryAuditEvent

class MemoryNexus:
    def __init__(self, db: Session):
        self.db = db

    def retrieve_active_memories(
        self,
        user_id: str,
        child_id: Optional[str] = None,
        limit: int = 10
    ) -> List[Memory]:
        query = self.db.query(Memory).filter(
            Memory.user_id == user_id,
            Memory.is_active == True
        )
        if child_id:
            query = query.filter((Memory.child_id == child_id) | (Memory.child_id.is_(None)))
        else:
            query = query.filter(Memory.child_id.is_(None))
        return query.order_by(Memory.updated_at.desc()).limit(limit).all()

    def get_memory_context_strings(
        self,
        user_id: str,
        child_id: Optional[str] = None,
        limit: int = 10
    ) -> List[str]:
        memories = self.retrieve_active_memories(user_id, child_id=child_id, limit=limit)
        return [f"[{m.memory_type} - {m.category}] {m.content}" for m in memories]

    def create_memory(
        self,
        user_id: str,
        content: str,
        child_id: Optional[str] = None,
        memory_type: str = "SEMANTIC",
        category: str = "GENERAL",
        confidence: float = 1.0
    ) -> Memory:
        memory = Memory(
            user_id=user_id,
            child_id=child_id,
            memory_type=memory_type,
            category=category,
            content=content,
            confidence=confidence,
            is_active=True
        )
        self.db.add(memory)
        self.db.commit()
        self.db.refresh(memory)

        # Audit Event
        audit = MemoryAuditEvent(
            user_id=user_id,
            memory_id=memory.id,
            action="CREATED"
        )
        self.db.add(audit)
        self.db.commit()

        return memory

    def update_memory(self, memory_id: str, user_id: str, new_content: str) -> Optional[Memory]:
        memory = self.db.query(Memory).filter(Memory.id == memory_id, Memory.user_id == user_id).first()
        if not memory:
            return None

        memory.content = new_content
        memory.updated_at = int(time.time() * 1000)
        self.db.commit()
        self.db.refresh(memory)

        audit = MemoryAuditEvent(
            user_id=user_id,
            memory_id=memory.id,
            action="UPDATED"
        )
        self.db.add(audit)
        self.db.commit()

        return memory

    def delete_memory(self, memory_id: str, user_id: str) -> bool:
        memory = self.db.query(Memory).filter(Memory.id == memory_id, Memory.user_id == user_id).first()
        if not memory:
            return False

        memory.is_active = False
        memory.updated_at = int(time.time() * 1000)
        self.db.commit()

        audit = MemoryAuditEvent(
            user_id=user_id,
            memory_id=memory.id,
            action="DELETED"
        )
        self.db.add(audit)
        self.db.commit()

        return True
