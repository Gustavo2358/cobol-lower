# WORK-LOWER-007 — policy and authorization

Explicit user authorization on 2026-09-09 covers one CP0, implementation through
PR, followed by human review. Supported SP 1.1/1.2 must not be rejected because
of total byte size, JSON node count or admission visits. CP4C deliberately
calibrated limits to its measured corpus; this new policy removes that admission
criterion. No semantic or schema change is authorized.

Preserve malformed/UTF-8/duplicate keys/shape/version/reference/profile/value
rejection, depth64, overflow safeguards, diagnostic exhaustion on invalid inputs,
AIR validation, deterministic identities/provenance/coverage and atomic output.
AIR transport remains pinned and independently limited. BACKLOG-LOWER-017 and
BACKLOG-LOWER-018 remain separate; no streaming/heap redesign or sibling changes.
