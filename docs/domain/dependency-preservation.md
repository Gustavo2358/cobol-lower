# Dependency preservation under incomplete physical evidence

Status: historical dependency-preservation slice, refined by W3-R1 local logical Cells.
The [harness principle](../engineering/lean-harness.md#dependency-preservation-principle)
is normative. Evidence hierarchy: exact physical/dataflow proof; semantic nominal/value
support; source-supported possibilities; no usable evidence. Lower levels keep supported
candidates with remainder. They cannot invent storage, aliases, control edges, or values.

The change reuses AIR 2.0.0 ObjectPlace/Read, logical Assign, UnknownBinding and
`target.possibilities@1`; no AIR schema or solver change. Possibilities are attached to
specific definitions and queried BEFORE the dependency site through existing CFG/RD
and values services. A failed physical name-area check opens interpretation remainder.
Runtime paths and missing source keep their existing uncertainty.

## Contract and limits

SP 2.32.0 adds `POSSIBLE_TEXT` for a single literal MOVE to a uniquely resolved whole
elementary DISPLAY PIC X receiver. Exact local syntax establishes receiver text length
and right padding, independently of allocation proof. It excludes truncation, tables,
subscripts, ref-mod, groups, unsupported clauses and representation conversions.
It publishes `logicalWholeItem` and `textAdjustment`, never a fabricated scalar or
physical view. Typed CICS PROGRAM/FILE hosts may carry canonical whole-item evidence
although flattened EXEC provenance is inexact. Physical provenance remains inexact.
Older SP versions cannot silently acquire the new copy semantics.

Strong regional transfers retain precedence. The lower validates the local contract,
preserves value definitions. The W3-R1 contract distinguishes supported logical
storage (`CellBinding`), true semantic location uncertainty (`UnknownBinding` with
its published bound), and physical representation gaps (coverage). It never
creates a Cell independently for each declaration name.
CICS PROGRAM and FILE consumers query readable nominal targets even without physical
8-byte IBM1047 proof. Name spelling/length validation still applies; raw value evidence
and interpretation remainder remain independent. SYSID keeps its separate context policy.

Nonliteral copies across unproved representations, generalized missing-source control,
interprocedural propagation and SQL/IMS typed calls remain outside this bounded change.
No claim of globally complete dependencies is made.

## Semantic authority and algorithm

[IBM elementary MOVE](https://www.ibm.com/docs/en/cobol-zos/6.3.0?topic=items-assigning-values-elementary-data-move)
provides left alignment/right space padding for the admitted alphanumeric case.
[IBM CICS XCTL](https://www.ibm.com/docs/en/cics-ts/5.5.0?topic=summary-xctl)
requires the computed program data area to be 8 bytes; lack of that physical proof
therefore prevents closure, but does not delete supported logical name possibilities.
AIR pinned specification 14 governs target possibilities and unknown domains.

Producer declaration and resolution indexes are built once; local transfer recognition
is linear in AST/symbols/bindings plus emitted text. Lower type evidence is indexed in
one pass over statements and entry conditions. Existing solver termination/complexity
is unchanged; there is no new dataflow, path enumeration, or global MOVE collection.

## Regression

`DependencyPreservationSuite` (FastAdapterSuite) consumes an emitted SP fixture, checks
CellBinding + logical Assign + CICS Read identity, AIR round trip, forged adjustment
rejection and rejection of new semantics mislabeled as an older SP version.
`CicsProgramControlSuite` checks short/partial nominal targets stay readable without
asserting an exact name area. FAST also covers existing regional/control contracts.

## Unknown layout and REDEFINES

The old SP snapshot lacked logical identity proof and is preserved under
`docs/campaigns/positive-memory-topology/evidence/`. W3-R1 SP 2.34/storage 1.10
publishes complete local TEXT view identity for A/B even with COPY missing. The
lower creates two ObjectIds with one Cell, so a mandatory second MOVE through A
replaces the first MOVE through B. The producer test regenerates the fixture;
missing COPY remains coverage. Same-start partial overlap does not get this
complete-view identity and continues through supported Region/views when present.
In compilation units, an explicitly captured logical CALL target carries its TEXT
use to the declaring unit. The owner receives the Cell; the captured ObjectId uses
AliasBinding to that owner. A physical qualifier that leaves a solitary root's
byte shape opaque does not turn this known logical value into a self bound.

SP 2.35/storage 1.11 extends this same fact to a complete local group/child
TEXT chain and an independent complete elementary TEXT root. The lower checks
the published hierarchy, extent agreement, component closure and old 2.34
shape separately. It assigns one Cell to the complete record/child pair and
keeps an unrelated same-sized value in another Cell. FILE effect strength is
unchanged: `MAY_UNKNOWN` remains MAY while its record scope becomes grounded
`StorageMemory(Cell)` through the existing DataLink path.
