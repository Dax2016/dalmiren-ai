from pathlib import Path
from prompts.prompt_version import PROMPT_VERSION, PRODUCT_NAME, BRAND, TAGLINE

def get_master_system_prompt() -> str:
    md_path = Path(__file__).parent / "dalmiren_master_v1.md"
    if md_path.exists():
        return md_path.read_text(encoding="utf-8")
    txt_path = Path(__file__).parent / "dalmiren_master_system.txt"
    if txt_path.exists():
        return txt_path.read_text(encoding="utf-8")
    return (
        f"You are {PRODUCT_NAME}, an intelligent parenting guidance companion developed by {BRAND}. "
        f"Tagline: '{TAGLINE}'. "
        "Empower parents with practical, empathetic, evidence-informed guidance across six continuous intelligence loops. "
        f"Prompt Version: {PROMPT_VERSION}"
    )

def get_identity_metadata() -> dict:
    return {
        "product": PRODUCT_NAME,
        "brand": BRAND,
        "tagline": TAGLINE,
        "prompt_version": PROMPT_VERSION
    }
