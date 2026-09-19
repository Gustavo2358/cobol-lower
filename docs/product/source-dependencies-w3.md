# W3 — nominal source resource transport

Status: IMPLEMENTED; integrated qualification and review decision are recorded in the W3 PR closeout. STACKED ON W2 #31, base
`557693a1184ddd2bd61886ceaa1176a78c0aa162`. Do not merge before W2.

The input contract is SP 2.30.0 `sourceDependencies`, documented by the pinned
frontend's `docs/product/source-dependencies-w3.md`. Closed decoding requires
availability, occurrences and gapCodes; unknown fields, kinds, missing provenance,
false DCLGEN authority and inconsistent artifact resolution reject. Older SP
contracts remain accepted with source inventory explicitly UNAVAILABLE.

## Mapping to existing AIR

No AIR model/schema changes. AIR Java remains
`646ca3ab1687d43f7d2063fc2a8f3837ab3cf9fa`. NO W3 AIR-JAVA PR.

Each source occurrence becomes an existing `Interactions.Resource` with
`LiteralTarget` and `ResourceDeclaration`. `resource.bindings` is declared.
No operation, object association, executable use, envelope or control point is
created. The versioned consumer profile is `source-dependencies@1`:

| Field | Mapping |
| --- | --- |
| LiteralTarget.category | source-copybook / source-dclgen / source-sql_include |
| LiteralTarget.name | canonical nominal name |
| LiteralTarget.namespace | qualification, or source-member when unqualified |
| LiteralTarget.namePolicy | ExactName (producer canonicalized) |
| ResourceDeclaration.owner | owning AIR program UnitId |
| ResourceDeclaration.classification | source.RESOLVED / UNRESOLVED / CYCLIC / IO_ERROR |
| ResourceDeclaration.nameSource | source.COPY_SYNTAX@1 / CONFIGURED_DCLGEN@1 / CONFIGURED_SQL_INCLUDE@1 / BUILTIN_SQL_INCLUDE@1 / UNKNOWN@1 |
| ResourceDeclaration.name | resolved logical artifact when RESOLVED, nominal name otherwise |
| Resource.origin / target.origin | existing conversion of original/expanded SP provenance |

Inventory is a LocalResource `source-dependency-inventory`, declaration name
`source-dependencies@1`, classification `source.KNOWN`/`source.PARTIAL` and nameSource
`source.inventory@1`. Each gap is a literal resource `source-dependency-gap`,
namespace `source-dependencies@1`, name=gap code, classification `source.PARTIAL`.
All these declarations have empty objects/uses. Incompleteness does not disappear.

`Origins.Artifact` identifies original source owners; Written locations and
IncludeFrame preserve source lineage. Existing SourceOrigins retains full
occurrence spans; include frames carry artifact chain, but only includeLine is
present upstream, so a complete include-site span is unavailable (existing explicit
limitation). Each inclusion's own occurrence still has its full original span.

ResourceDescription/ResourceDeclaration are sufficient: resources already carry
nominal classification, program ownership and provenance without execution meaning.
AIR `Artifacts.Relation` is another available nominal structure, but alone lacks
the program association plus resolution/classification-authority fields used here.
DependencyEnvelope would introduce unnecessary program points. No new schema,
COBOL operation or parallel AIR model is justified.

Input source facts participate in canonical publication identity. Indexed transport
is O(N), excluding source-provenance/include-chain payload size. The lower never
opens source/inventory/artifact filenames. Existing logical/physical engines are
unchanged; default LOGICAL_ONLY remains downstream policy.

`SourceDependencySuite` tests actual producer SP, existing AIR codec roundtrips,
empty executable associations and malformed-wire rejection. Cyclic preprocessing
that invalidates a usable primary entry remains BLOCKED_LOWERING; no fake runtime
entry is manufactured to force transport. Corporate NOT EXECUTED / NOT AN
ACCEPTANCE GATE / NO CORPORATE SOURCE USED.

Producer pin: `1d20897965db55fca39a6a654c0386229f1e5232` (SP 2.30.0).
Five committed SP fixtures come from this exact producer; 45 malformed mutations
exercise strict decoding, and existing AIR readers roundtrip the unchanged wire.
