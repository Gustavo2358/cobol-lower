# DVI — source-proved entry literals

Current coordinated input: SP 2.15.0 / storage 1.4.0, frontend Draft #49.
`docs/sources/sources.lock.json` pins exact source commits, trees and blobs.
No AIR production or normative change is needed: AIR 2.0.0 §3.8 already describes
literal entry knowledge, provenance, open missing conditions and no label reseeding.

The frontend now distinguishes EXPLICIT_INITIAL, EXPLICIT_PRESERVED,
PROGRAM_INITIAL, DECLARATIVE_INVARIANT and NONE per condition. A literal invariant
requires exact local independent physical storage. Automatic facts coexist with
global UNKNOWN; explicit INITIAL/PRESERVED retain their existing premise.

The new Wire215 reader requires storage1.4 and a proof enum; historic readers keep
their exact shapes and synthesize only their previously mandatory explicit
initial/preserved proof. A 2.15 fact under a 2.14 envelope, missing proof, unknown
proof or wrong storage version is rejected. There is one current writer shape.
Admission applies equally to decoded and in-memory facts, rejecting contradictory
kind/proof/mode, missing local allocation, byte count, codec or bounds.

Source proofs remain source responsibility. The lower neither reparses COBOL nor
reconstructs its lifetime write inventory. It preserves the source literal and
VALUE provenance in existing AIR LiteralInitial. A derived origin records the
proof kind and storage proof version; the proof participates in publication
identity. No synthetic MOVE or new dependency/value solver is introduced.

Algorithm: existing linear entry-condition translation and indexed physical
admission; each source condition maps to its current RegionSlice/Cell. Finiteness
and complexity remain proportional to the published input/bytes. Source gaps
remain uncertainties. This changes transport/admission/provenance only, preserving
Regional Values overwrite, copy, join and backedge behavior.

Evidence: production frontend `invariant.cbl`/`invariant.json` in `sp/dvi`, emitted
by f853bc3c06a0aed3259929926fbcd556969c225c using the physical IBM1047 profile and
default UNKNOWN. `DeclarativeValueSuite` checks exact bytes, source origin, entry
ownership, identity sensitivity and eight malformed/contradictory inputs.
Existing InitialStorageSuite and CICS contract regressions also run. The first
consumer run was RED (UNSUPPORTED_CONTRACT), followed by focused GREEN.

21 selected production CLI E2Es cover CALL, automatic INITIAL, invariants,
data MOVE, LINK/XCTL, runtime branches, disjoint/direct/group/alias/RENAMES/slice
writes, foreign effects, overwrite loops and rename/format equivalence. Known
candidates retain honest remainder. No generic CFG production change was needed.
Real sources with incomplete preprocessing retain their blockers; DVI does not
complete missing CardDemo inputs or add platform effects opportunistically.
