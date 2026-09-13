# Compositionality and conservative partial lowering

The default pipeline consumes structurally usable programs. Each supported region
has precise AIR; each semantic gap has a conservative operation and local
uncertainty. A semantic gap does not remove its program or dependency sites.

## Permanent invariants

1. **No Artificial Cardinality.** Capability limits semantic forms, never a finite
   number of occurrences. New features must pass 1, 2, 5 and a larger N, plus
   mixtures. A unique selected candidate in a RESOLVED nominal reference is a
   legitimate identity invariant, not a program/profile cardinality gate.
2. **No Silent Elision.** Unknown statements retain identity, provenance and an
   explicit operation/gap. They cannot disappear or become Nop without a proof
   of absence of effects.
3. **Conservative Fallback.** Reuse Assign/Branch/Jump/Invoke/Return for exact facts,
   HavocMust for a proved mandatory write, HavocMay for a possible scoped write,
   and Opaque with declared envelopes for insufficient effects/control facts.
4. **Local Degradation.** Apply effects at their ProgramPoint. A later unknown
   memory effect does not retroactively change an earlier observation. Unknown
   control may revisit an earlier region only when its envelope permits that.
5. **Known Facts Survive Uncertainty.** A possible write retains surviving known
   candidates and opens the remainder; a mandatory overwrite kills the old value.
   Strong later assignments can restore a closed model value.
6. **Completeness != Usefulness.** COMPLETE, PARTIAL and unknown knowledge are
   independent of publication success. Coverage, uncertainties and per-site
   remainders remain separate; no dependency-result wire change is needed.
7. **Whole-program rejection is exceptional.** Reject contradictory/invalid
   structure, unusable frontend output, identity/validation failure or an explicit
   resource limit. An unsupported statement/surface alone is not such a failure.

## Completion contract for future constructions

A construction is complete only with multiplicity, mixed composition, unsupported
neighbors, localized uncertainty and deterministic identity/control tests. This
applies to future EVALUATE, GO TO, I/O, SQL, CICS and PERFORM variants. Test numbers
are examples, never productive limits. No N-sized program gate may implement a
semantic profile. Physical inventory order does not create control edges.

The next product activity after this wave is CARDDEMO BASELINE: run the corpus,
count unsupported constructs and affected programs, measure CALL-site impact,
rank blockers by expected coverage gain, and rerun CardDemo after each vertical.
No precise EVALUATE/GO TO/READ/WRITE, SQL/CICS, general PERFORM, RD, Def-Use or SSA
is implemented by this wave. Git/PR/tests/merge are the record; remote FAST only.

## Current and legacy boundary

SP1.8 selects PartialProgramLowerer: structural validation first, precision per
statement second. Source gaps are scoped to their output operations. BASIC bodies
specialize by activation, with distinct operand/operation/label identities and an
explicit return to each callsite. One source body can have multiple output links.
Each already-qualified whole-item text MOVE remains precise; unknown conversion
to a proved receiver uses HavocMust. Opaque carries available operands, conservative
memory/dependency bounds and only proved control. A known normal continuation
retains possible abnormal exits; missing control uses a unit control envelope.

CALL target discovery survives USING/RETURNING/handlers. Present but unmodeled
signature parts use independent unknown remainders; all-memory may effects remain.
An unavailable name value is a computed unknown TEXT name, preserving the site.
IF with the published BOOLEAN/PURE/TOTAL predicate proof keeps both branches even
with nested or unsupported arm contents. Other IF predicates fall back without
invented Boolean proof or a Jump that skips the arms.

Decoder 1.1–1.7 and their explicitly versioned qualified profiles remain compatible.
Their historical restrictions do not govern SP1.8. BASIC intrinsic-body facts cannot
be relabeled as SP1.7. This is a semantic version transition, not a silent wire
reinterpretation. AIR 2.0 is unchanged; the file pipeline uses the extended shared
codec in air-java #12.

PartialIntegrationSuite protects 1/2/5/40 mixed occurrences, both mandatory PERFORM
cases, all source links, activation returns, partial regions and deterministic
bytes under reversed physical statement inventory. Old seven MULTI-CALL fixtures
remain frozen regressions; real source is rerun through SP1.8 locally.

A control-only frontier after Assign or HavocMust has empty memory/dependency
effects: the write was already applied. Missing continuation does not cancel the
proof of a mandatory receiver and does not add a second possible write. Unit
provenance links entry and the actual initial sequence, with no duplicate inputs.

Specialized BASIC return provenance joins the activation, resolved target,
paragraph, body write and resume. Interior body jumps use intrinsic continuation
provenance. Normalization preserves the original control evidence per activation.
