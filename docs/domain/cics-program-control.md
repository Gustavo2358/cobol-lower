# CICS Program Control

SP 2.13.0 adds LINK/XCTL and PROGRAM targets without masquerading as CALL.
Literal values retain spelling. A computed target requires a canonically selected
physical view and an exactly eight-byte IBM1047 range, using RegionalPlaces and
existing object/storage identities. Short areas, unknown codecs and dynamic slices
remain unknown, with the source target and binding retained in SP.

AIR uses Invoke actions call/execute, namespace cics.program and the required opaque
name policy cics-ts.program@1. Signature parameter/result remainders are both unknown.
All-memory may-read/may-write bounds conservatively include unmodeled COMMAREA,
CHANNEL, RESP/RESP2 and foreign effects. Known option places retain their identities;
no outcome-specific MUST write is claimed. The consumer queries the name BEFORE.

LINK has the source-published local normal continuation. XCTL has no normal success
continuation. RESP/NOHANDLE use bounded local-error remainder plus external scope;
unknown handlers retain unit control. Zero normal outcomes alone proves nothing.

SP localContinuation is positional and never claims XCTL success returns.
DEFAULT_ENTRY_PREFIX carries an explicit NEW_LOGICAL_LEVEL environment premise
plus complete, canonical MOVE-prefix proof. Its control bound permits external
transfer and exceptional/abend exits, with no local label or normal exit.
Unknown entry context remains open. RESP2 alone cannot select local-error policy.
Bounded CICS regions reuse paragraph activation overrides; transformed source
provenance stays inexact, while explicit IDs and syntax proofs justify boundaries.
