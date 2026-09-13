# Conditional GO TO / DEPENDING ON — SP 2.6

Status: implementation, FAST and qualification-local PASS; integrated evaluation tracked downstream. No merge.

Authority: IBM Enterprise COBOL 6.4 Language Reference, conditional GO TO,
chapter 28, printed p.347, verified 2026-09-13:
https://publibfp.dhe.ibm.com/epubs/pdf/igy6lr40.pdf . LANGUAGE_GUARANTEED:
ordinal selection is one-based, selector is an elementary numeric integer,
and an out-of-domain selector takes the normal continuation. The compiler
profile admits at most 255 procedure-name occurrences. Above-profile parser
input keeps all occurrences and receives an explicit profile gap.

ARCHITECTURE_GUARANTEED: existing Ast.GoToStatement already carries ordered
reference occurrences and selector. Generalize GoToSemantics and reuse indexed
ReferenceResolution -> SemanticEntityId -> symbol -> executable entry joins.
Use NumericControlSemantics for integer item proofs. No numeric value analysis.
SP 2.6 adds a conditional fact, per-destination ordinals/identity/entry/origins/gaps,
selector reference and origin, integer proof, and explicit normal continuation.
Historical simple GO TO wire remains unchanged. Partial targets never erase peers.
Sections keep typed identity when available; entry precision requires the proven
paragraph profile. ALTER/input uncertainty never creates precise control.

SPECIFICATION_GUARANTEED: AIR 2.0 operations §9 and incompleteness §3.2 already
support Opaque with multiple known jump alternatives, a normal alternative and
an independent control remainder. Prefer this existing generic envelope over a
branch chain: one source selector read, one transfer, O(N) alternatives. No new
AIR operation/codec or CFG production is planned. The SP/evidence retains ordinal
multiplicity even when equal CFG edges are shared. No writes or value production.
Complete control uses no remainder; partial control keeps all known alternatives
and an open unit frontier. Selector values remain unevaluated, including MOVE 2.

Index once and visit each reference once: O(nodes + symbols + references + N)
time/storage. Closure checks traverse finite graphs, never enumerate paths.
Cycles are valid CFGs but need not prove returning PERFORM activation. All ordinary
incoming candidate edges count against BASIC/range isolation. Unknown targets
prevent range closure; escaping targets cannot be reinterpreted as local return.

Finite Multiplicity: one supported occurrence implies arbitrary valid finite
multiplicity must remain supported unless the source language itself defines a
semantic limit. Product code must not impose a smaller implementation count limit.
Lists with hundreds of destinations are normal supported inputs and must not be
truncated, rejected, or degraded merely because of target count.

Independent oracles: D1 exact four dependency alternatives including fallthrough;
D2 exclusion of unrelated textual statements; reordered/duplicate occurrences;
unresolved destination/selector and unavailable entries; IF/EVALUATE composition;
backward/mixed loops; counts 1/2/5/40/100/200/255 and parser tolerance above profile;
indexed resolution-attempt counts; malformed wire/in-memory rejection; PERFORM
incoming/escape/closure; A/B bytes and physical inventory/field/sequence permutations.
Prior 23 GO TO, 18 EVALUATE, 89 PERFORM and historical suites must remain green.
Focused -> adversarial -> regressions -> affected -> exactly one final local
CardDemo Full. Remote FAST only. decisionPolicy HUMAN; rankingAuthority ADVISORY_ONLY.

## Lower qualification

The strict 2.6 decoder is additive; versions 1.1 through 2.5 retain their historical
decoders. In-memory admission independently checks contiguous ordinals, local
identity, canonical entries, provenance agreement, selector declaration type and
normal continuation before generating any AIR. Malformed input is INVALID_INPUT.

FAST and qualification-local PASS, including the existing 10k/20k production
capacity and architecture checks. One earlier qualification attempt exhausted the
`/tmp` quota; its raw log remains preserved. The successful unchanged test used
workspace disk for temporary output, without reducing any fixture or limit.
