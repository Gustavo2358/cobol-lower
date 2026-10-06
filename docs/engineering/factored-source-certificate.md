# Lossless factored source certificate — design before implementation

Status: shared internal column owner implemented in producer and consumer;
qualification and final performance comparisons pending. No general closeout claim.

The R7 source certificate is a finite typed AND/OR graph. Sharing source bodies in
AIR does not make source binding contexts equivalent. Source value transfer can
observe distinct caller histories, so merging those contexts is not permitted.
The representation may factor repeated immutable tuples without changing any
logical node, premise, alternative, support, proof or remainder.

The chosen implementation retains transport 1.0.0–1.6.0 unchanged. A unit-local
immutable owner stores correlated rows in primitive columns and dictionaries of
exact equal strings, supports and whole proof lists. The dictionaries store opaque
IDs verbatim. Each original ordinal selects exactly one complete tuple; no
Cartesian product or guessed membership is introduced. The incremental parser
reads one inventory element into this owner and discards that element's JSON tree.
An explicit wire dictionary can be considered separately if physical serialization
volume becomes the limiting cost; it is not required to establish this bijection.

The expansion bijection maps each declared node/derivation ordinal to exactly one
legacy typed record. Original identities, inventory order, contexts, source/caller
premises, proofs and selection identity are retained by that map. Nodes with the
same location/support in different contexts remain distinct. Templates preserve
proof alternatives as whole correlated tuples; proof lists may be interned only
when equal. Unknown/control-possibility and branch annotations stay attached to
exact derivations. Selection/registration/DEACTIVATED validation is unchanged.

A sealed immutable inventory view owns all its template/row data, can yield a typed
record on demand, and never caches the full expansion. Public arbitrary Lists still
cross List.copyOf; only the contract's internal validated immutable owner can avoid
that copy. Admission still validates every logical identity and edge and the finite
certificate grounding law. Primitive ordinal indexes bound scratch by declared
logical counts instead of per-record object graphs. Downstream consumers retain
only the locations/supports they actually query; they may request expansion of all
records, in which case output size remains intrinsic and explicitly measurable.

Expected transport/owner space is proportional to templates, unique immutable
values and correlated membership rows. Logical validation may still take O(N+D)
work/scratch; no universal linear-in-source-size or absence of resource failure is
claimed. Canonicalization/grouping is expected linear in observed tuple data plus
sorting. Hashes are lookup accelerators, never a substitute for full equality.
Termination follows finite inventories and exact finite membership counts. Overflow,
invalid template/reference, bad encoding, overlap/duplicate ordinal, omitted node,
correlation mismatch and operational limits fail before publication.

Independent oracles must compare expansion to legacy frozen source evidence, then
compare complete dependency candidates/supports/proofs/remainders. Cases include
multiple callers with equal handler support but different nominal values, nested
returns, unknown completion, grounded/unrooted cycles, distinct proof producers,
selection guards, branches, native file alternatives and table summaries. Negative
mutations change one context column, drop one membership bit/ordinal, mix source and
caller alternatives, remove a producer or reuse a foreign unit identity. Memory
qualification uses the same logical graph and observations in explicit/factored
encoding, not a smaller semantic fixture. Encoding absence is not unavailability.
