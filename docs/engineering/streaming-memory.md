# Physical input lifetime

The SP decoder owns its parsed tree and normalizes historical DTO shapes in place.
A file path is read through a strict UTF-8 Reader with a bounded decoder buffer;
there is no readAllBytes or whole-document CharBuffer in this path. Byte-array
APIs remain available and preserve their bytesProcessed accounting. InputStreams
remain caller-owned. UTF-8 BOM retains historical SP admission; UTF-16/autodetection,
malformed UTF-8, duplicate fields, trailing documents and operational depth limits
retain typed failures. Jackson's documented Reader parser and Java CharsetDecoder
REPORT semantics govern physical decoding, not COBOL semantics.

Compilation envelopes parse once. Their product subtrees enter the same closed
unit decoder directly; reserialization/reparse of each unit is unnecessary and
would duplicate the live wire representation. The node entry is package-private,
used only for trees already parsed with duplicate/depth/trailing checks. No public
API admits an unchecked JsonNode. Normalization does not modify source bytes or
facts and all existing DTO, physical/coherence/version checks still execute.

Space is O(live tree + typed facts + bounded UTF-8 buffers); tree-to-model mapping
remains to be factored for repeated inventories. Work is O(wire bytes + values),
removing repeated byte/character conversion and duplicate parsing. Termination is
finite stream EOF and finite parsed inventory. Failure never publishes a partial
SP or repairs missing proof. Independent chunked stream tests cover UTF-8 scalars,
malformed/truncated sequences, BOM, trailing input, ownership and exact typed facts.

Primary sources read: [Java 21 InputStreamReader](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/InputStreamReader.html),
constructor with CharsetDecoder and read-ahead ownership semantics; Jackson
ObjectMapper Reader readTree source (FasterXML/jackson-databind 2.x). The actual
pinned Jackson jars are compiled and exercised by the closed-field fixtures.
The hosted 2.22.2 javadoc was unavailable; no behavior is inferred from it.

## Qualified source output and snapshot ownership

A writer can observe the immutable typed snapshot in transport order one element
at a time. Lazy mapped list views retain only typed owners; they neither cache
wire DTOs nor change values, order, optional fields or error semantics. A streaming
writer holds O(one element wire mapping + generator buffer) beyond its typed
input. The existing byte-array API stays available, with identical deterministic
bytes. Caller-owned output is flushed but never closed. File adapters stage and
close the compressed stream before atomic rename; exceptions leave the previous
final snapshot intact and no manifest is published.

The new physical `lowerSnapshot` entry captures document schema/version and the
SHA-256 of exactly the decoded byte stream used for admission. It retains typed
facts and this immutable identity rather than bytes. It never reopens a mutable
source path to obtain identity after admission. The legacy `lower(..., evidence)`
and three-argument Lowered constructor preserve defensive byte ownership. This
explicit new entry avoids silently changing sourceBytes behavior for existing
callers. UTF-8/BOM bytes remain in the digest; compression is outside identity.

Termination is parser EOF plus finite typed inventories. Independent qualification
covers same bytes in legacy and streamed output, caller ownership, throwing
output, compressed/plain hash parity, input mutation after admission, and all
existing optional evidence families. No dependency, proof, support or remainder
is filtered by this transport optimization.

## Certificate indexing and grounding

Qualification validates a finite AND/OR causal graph, not an enumeration of paths.
An immutable hash index can store only bucket heads and next ordinals into the
already-owned list. Hash collision resolution always compares full typed keys;
no identity is inferred from a hash. The map obeys ordinary get/contains/entry
semantics and rejects duplicate keys exactly as before. It costs O(N) primitive
slots with no per-entry object or cloned DTO. Expected lookup/build work is O(N);
buckets above eight keys use the same HashMap collision handling as the prior
implementation, retaining its tree-bin protection for comparable keys. No
validation lookup silently discards a collision. Identity renaming and deliberately colliding
keys are adversarial oracles.

Each derivation has at most one source and one caller premise. Grounding therefore
uses a byte count (0..2), primitive links and node ordinals. Multiple derivations
remain OR, distinct source/caller premises remain AND; a repeated same node is
one premise. Nodes are queued once, on first grounding. Ungrounded SCCs and
unknown references are still rejected; a rooted SCC is accepted. This is Kahn's
finite monotone certificate worklist, with O(N+D) scratch and O(N+D) work after
indexing. Observation-location indexes are built only for declared occurrences;
all nodes and derivations are still validated. A location with zero observed uses
has no validation query and needs no retained inverted set.

Independent tests cover rooted/unrooted cycles, AND requiring both premises,
duplicate identities, colliding strings, same-source/caller idempotence, and a
large legal certificate under a fixed small heap. Resource failure does not
publish any certificate. The source model and wire contract remain unchanged.

## State and certificate owners

Hash-consing an already equal immutable Node changes ownership only. The canonical
key retains the complete context, location and Support; no entry, caller, endpoint,
condition registration, restoration or cause is removed from equality. Inserted
derivations point to the canonical node, and an equal node is scheduled once, as
before. Similarly, source DTO supports share only exactly equal handler Supports
within one unit projection. There is no global cache and no cross-revision sharing.
The wire still emits each node's complete support and proof references. The
certificate AND/OR relation, facts, frontiers, metrics and order are unchanged.
Space follows distinct node/support values and causal edges rather than repeated
allocations of equal destination/support objects. Work uses the existing exact
hash/equality law; comparator/string ordering remains unchanged at this checkpoint.
Independent existing R7/R9 schedule, handler and dependency oracles remain valid;
projection equality and CLI/file parity must continue to hold.

## Incremental source certificate admission

The codec consumes one explicit inventory element at a time, in any legal JSON
field order. It never retains a JSON tree for the whole unit or a byte snapshot.
Strict duplicate detection, closed field sets, physical digest, trailing tokens
and every typed certificate invariant remain admission requirements. A stream
is caller-owned. A digest is taken over the exact same admitted decoded stream,
not a second open of a mutable file. Negative admission publishes no result.

Space is O(live typed facts + largest single JSON element + validation scratch).
Time is O(wire bytes + existing validation). Each finite inventory is consumed
once. This does not coarsen contexts or solve intrinsic logical graph size.
Independent tests compare frozen legacy bytes and all typed inventories, reorder
fields, reject extra/duplicate/truncated/trailing input and test stream ownership.

The bytecode boundary admits the codec's private `QualifiedSourceJson$Element`
parser callback beside its existing codec owner. No output adapter gains AIR JSON
serialization authority. The malicious AIR-Jackson challenge stays denied.

## Lossless owned column inventories

A unit-local builder copies every logical tuple into primitive ordinal columns,
with dictionaries of exact equal immutable strings, supports and proof lists.
Node and derivation rows are correlated by their original ordinal, not by a
Cartesian product. All opaque IDs are retained verbatim; no prefix or source
name has semantic significance. Freezing severs all builder aliases and lookup
maps. The only immutable views exempt from List.copyOf are private final owner
classes created by this factory; arbitrary caller lists are still copied.

The expansion bijection is get(i) == the original record at i. Shared values use
full equality including context, support, source, caller, proof alternatives and
selection. No graph fact is removed; all closed-wire and grounding validation
runs on the same expansion. Decoder builds one row per admitted element, in any
field order, and retains no expanded history. Builder is one-shot. Overflow or
invalid input fails admission. get is O(1); build and dictionary admission are
expected O(N+D+tuple payload), owned storage O(unique values + column cells),
with an O(unique values) temporary dictionary. Validation remains O(N+D) work
and scratch. Unique facts remain intrinsically proportional to their size.

Independent tests compare arbitrary IDs, equal/different supports and caller
premises to manually authored records, reject ungrounded/foreign references as
before, mutate input lists after freeze, attempt view mutation and builder reuse,
and compare every legacy transport byte and result. Large profiles use identical
logical inventories and observations, not a smaller graph.

Projection demand only filters its private inverted qualification index to locations
used by published dependency occurrences and native file uses. All source nodes,
derivations, proofs and branches remain in the certificate; every observed location
retains the same full qualification list. This removes an unused index, not facts.
