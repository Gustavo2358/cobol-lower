# W3 — nominal source resource transport

Status: DB2 implementation present; current integrated qualification status is recorded in the existing stacked PR. STACKED ON W2 #31, base
`557693a1184ddd2bd61886ceaa1176a78c0aa162`. Do not merge before W2.

The current input contract is SP 2.31.0 (SP 2.30.0 remains supported) `sourceDependencies`, documented by the pinned
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

## DB2 TABLE continuation

The previous COPYBOOK/DCLGEN/SQL_INCLUDE qualification remains a completed checkpoint.
DB2 TABLE qualification is now required before the campaign returns to READY_FOR_REVIEW.
Same branch and PR; no new administrative W4, no AIR/runtime/physical engine changes.

## DB2 TABLE source dependencies

Static SQL is extracted before embedded-language framing, from the normalized original EXEC SQL region and its existing SourceMap span. A lightweight tokenizer neutralizes SQL strings, host variables, `--` and `/* */` comments; paired parentheses are indexed once. A deterministic structural scanner recognizes table positions and scoped CTEs without constructing SQL grammar/AST/IR or evaluating expressions. Each region has independent state. Unsupported/malformed supported structure rejects all tentative table facts for that region and opens a gap.

Supported: SELECT FROM/JOIN (including multiple and comma joins), schema qualification, INSERT target and INSERT SELECT, UPDATE/DELETE targets and nested SELECT, MERGE target and nominal/derived USING, CTE definitions, derived SELECT, UNION/EXCEPT/INTERSECT branches, and simple DECLARE name CURSOR FOR SELECT. Aliases are consumed at relation boundaries. Local CTE names are indexed before traversing definitions; recursive/forward CTE references conservatively open DB2_RECURSIVE_CTE_UNSUPPORTED. SQL expression validity, column binding and catalog object kinds are not certified: DB2_TABLE denotes a syntactic relation reference, which a catalog could resolve to a table/view/alias. No catalog resolution is attempted.

Identity: uppercase ordinary identifier name plus explicit qualification; CLIENTE and DBPROD.CLIENTE remain distinct. Delimited identifiers are tokenized but conservatively rejected with DB2_DELIMITED_IDENTIFIER_UNSUPPORTED because the current source identity contract folds case. Table functions, VALUES-derived relations, DDL, stored procedures, unfamiliar relation constructs, and unsupported cursor options open explicit gaps. Nesting beyond 128 levels opens a gap. No inference from DCLGEN or INCLUDE names/content.

SP 2.31.0 adds DB2_TABLE and typed operation/access per occurrence. Non-DB2 occurrences use NONE/NONE. SELECT uses READ; INSERT/UPDATE/DELETE use WRITE; MERGE target uses MERGE/READ_WRITE and nominal USING uses MERGE/READ (derived SELECT uses SELECT/READ). Authority STATIC_SQL_TABLE_POSITION is required. Resolution NOT_APPLICABLE is exclusive to DB2_TABLE: catalog lookup is outside this product and no physical artifact identity is fabricated. This nominal completeness is separate from source artifact resolution.

PREPARE and EXECUTE, including EXECUTE IMMEDIATE literals, emit DYNAMIC_SQL_NOT_ANALYZED with remainder=true and no invented table. PossibleValues is never invoked. Other unsupported SQL shapes remain open. Existing source gap transport is conservative at compilation scope; no statement-level SQL gap provenance type is introduced in this continuation.

Each support retains original EXEC SQL span, program association, sourceOwner and include chain. SQL in A.cpy is TRANSITIVE to the program and points into A.cpy. Repeated SELECT/UPDATE of the same qualified table aggregate into one dependency with separate usage-bearing supports. Source aggregation uses maps and canonical sorting, no CFG/reachability/RD/values/physical inputs. Runtime SQL stays opaque in its existing path.

AIR stays unchanged at 646ca3ab1687d43f7d2063fc2a8f3837ab3cf9fa. Existing LiteralTarget category source-db2_table and ResourceDeclaration classification source.NOT_APPLICABLE carry nominal references; nameSource source.STATIC_SQL_<operation>_<access>@1 carries a closed usage profile. No runtime uses, objects or operations are added. The dependency wire is 2.5.0, with operation/access on every source support; 2.4.0 readers reject it. The new reader retains explicit support for older wires; lower upgrades legacy SP2.30 NONE usage only after rejecting DB2/new fields in that old envelope.

Scope remains source-only, physical default OFF with NO AUTOMATIC FALLBACK. Existing cyclic COPY primary-entry admission limitation remains unchanged. Corporate NOT EXECUTED / NOT AN ACCEPTANCE GATE / NO CORPORATE SOURCE USED.

Primary language references: [IBM CTE](https://www.ibm.com/docs/en/db2-for-zos/12.0.0?topic=statement-common-table-expression), [identifiers](https://www.ibm.com/docs/en/db2/12.1.x?topic=elements-identifiers), [tokens/comments](https://www.ibm.com/docs/en/db2-as-a-service?topic=elements-tokens). Scope is deliberately smaller than the SQL language.

Current DB2 producer pin: `49ce9a7e727ad0c3301cc1fdbb6da9828439b9ce` (SP2.31).
The five earlier SP2.30 fixtures remain unchanged as compatibility evidence; three SP2.31 DB2 fixtures add mixed usage, nested provenance and INSERT SELECT roundtrips.

Lexical hardening in the producer keeps nested comments and multi-statement EXEC SQL regions incomplete. The SP2.31 transport shape and lower production code are unchanged by this final repin.
