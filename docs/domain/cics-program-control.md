# CICS Program Control

SP 2.14.0 adds LINK/XCTL and PROGRAM targets without masquerading as CALL.
Literal values retain spelling. A computed target requires a canonically selected
physical view and an exactly eight-byte IBM1047 range, using RegionalPlaces and
existing object/storage identities. Short areas, unknown codecs and dynamic slices
remain unknown, with the source target and binding retained in SP.

AIR uses Invoke actions call/execute, namespace cics.program and the required opaque
name policy cics-ts.program@1. Signature parameter/result remainders are both unknown.
All-memory may-read/may-write bounds conservatively include unmodeled COMMAREA,
CHANNEL, RESP/RESP2 and foreign effects. Known option places retain their identities;
no outcome-specific MUST write is claimed. The consumer queries the name BEFORE.

LINK has the source-published ordinary normal continuation; an explicit PERFORM
completion instead supplies its activation resume. XCTL has no normal success
continuation. RESP/NOHANDLE use bounded local-error remainder plus external scope;
unknown handlers retain unit control. Zero normal outcomes alone proves nothing.

SP localContinuation is paragraph-local; ordinaryContinuation also crosses the
paragraph boundary. Both are positional and never claim XCTL success returns.
DEFAULT_ENTRY_PREFIX carries an explicit NEW_LOGICAL_LEVEL environment premise
plus complete, canonical MOVE-prefix proof. Its control bound permits external
transfer and exceptional/abend exits. For LINK this narrower remainder requires
a known return destination: an unavailable return retains possible local control.
Unknown entry context remains open. RESP2 alone cannot select local-error policy.
Bounded CICS regions reuse paragraph activation overrides; transformed source
provenance stays inexact, while explicit IDs and syntax proofs justify boundaries.

W4 consumes frontend READ data-area references for group/RENAMES targets without
broadening elementary-only COBOL CALL admission. Six public producer fixtures
cover variable, qualification, short-area rejection, constant slice, overlay and
group reads. Composed local qualification ran semantic/capacity once; reduced
CICS tests and unchanged-output transport tests cover final deltas. The explicit
new-logical-level premise is retained in the AIR uncertainty explanation.

Review remediation F1/F2: SP 2.14.0 requires both continuation fields and explicitly
rejects superseded Draft 2.13.0 CICS input. Intrinsic PERFORM admission still uses
the paragraph-local relation; ordinary assembly uses the ordinary relation and
activation completion overrides take precedence. No source-order edge is inferred.

Admission rejects condition profiles contradicting typed options or gaps. DEFAULT
excludes RESP/NOHANDLE/RESP2; LOCAL requires RESP/NOHANDLE. Syntax/handler gaps and
unknown options cannot authorize a narrow bound. Signature/effect and name-binding
gaps remain independent. Validation does not parse COBOL or CICS payload text.
The public suite lowers a real XCTL/NOHANDLE SP, mutates only conditions, verifies
rejection through decoder/lower and CLI, and checks the previous output is intact.
It also checks the independent LINK unavailable-return fallback under DEFAULT.

## Typed occurrence at an unavailable control frontier (R5)

For SP2.39+ the authoritative ControlTopology determines outgoing control. An
admitted typed XCTL with only UNKNOWN_LOCAL outcomes still denotes an interaction
at its own site. The lowerer preserves the existing `execute` Invoke target,
operand identity, provenance, partial signature and independently supported effects.
It carries the original empty open LabelsControl frontier, no known successor,
and control precision UNAVAILABLE with CONTROL_TOPOLOGY_REGION_UNAVAILABLE.
SP handler/signature gaps remain coverage evidence; they do not erase the target.
This rule supersedes the historical blanket physical-target and all-memory claims
above: nominal targets follow the current evidence-preserving contracts, and gaps
never justify global effects or control.

An all-UNKNOWN_LOCAL XCTL has no modeled return, result or local continuation to
bind to a PERFORM activation. Its scheduled context copies therefore reference one
operation/label for the same unit and source StatementId. AIR 01 §2 permits a single
operation reached through multiple label references. This preserves the union of
states already reaching that source occurrence without opening a path past it.
Distinct source IDs and units never share, even when target spellings agree. Any
known continuation keeps the existing contextual representation. CALL, LINK,
NOHANDLE/RESP and other frontier families retain their prior semantics.

The implementation indexes eligible source identities once, then uses constant-time
label and emission lookups. It adds no graph walk, source scan or physical-profile
requirement. No SP/AIR contract, candidate provider or handler semantics changes.
`TypedOccurrenceControlSuite` checks the typed payload, identity, 1/2/3 multiplicity,
independent uncertainty, target provenance, strict AIR codec, repeated targets and
unit separation. External R5 E2E evidence covers BEFORE value/overwrite/unreachable
safety, unchanged source control, frozen historical oracles and mutations.
