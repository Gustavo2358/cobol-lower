"""Fail closed before starting any local qualification component in Actions."""
import os


def require_local():
    if os.environ.get("GITHUB_ACTIONS", "").lower() == "true" or os.environ.get("GITHUB_RUN_ID"):
        raise RuntimeError("REMOTE_QUALIFICATION_PROHIBITED: FULL/PERFORMANCE/challenges/producer E2E are LOCAL ONLY")
