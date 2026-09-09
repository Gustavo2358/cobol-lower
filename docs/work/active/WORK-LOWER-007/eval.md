# Eval

Freeze 20,000 FULL_IDENTITY MOVE statements sharing one DATA followed by GOBACK,
generated deterministically from the existing scalar-move-1.2.0 fixture. Synthetic
extension, not a new frontend snapshot. Expected: 1 Object/Cell/Sequence/Entry,
20,000 Assigns, Return, complete correlations and original provenance; use existing
ScalarSuite.oracle and complete Publication equality across independent runs.
Actual SP bytes and node count must exceed 32MiB and 1,500,000; admission visits
must exceed 250,000. File and bytes decode must reach SUCCESS in lower stages.

CP3/CP4C golden bytes/hashes remain immutable. Test malformed JSON, UTF-8,
duplicate keys, shape, contract/profile, incoherent scalar values, depth64, and
atomic failures. Existing N/2N ledgers remain exact; no performance claim by time
alone. Restore reintroduced byte/node/visit gates exactly and obtain second GREEN.
