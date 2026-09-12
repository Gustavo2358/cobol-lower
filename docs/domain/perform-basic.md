# CP6 PERFORM BASIC — direct control normalization

Input: SP 1.6.0 SIMPLE_SINGLE_CALLSITE_PROCEDURE_PERFORM. Output uses existing
AIR 2.0.0 Jump, Assign(Literal/Read), Invoke and Return; no control.local@1 claim.

Normative AIR remains analysis-ir@51b4d9a8ae0364232bd97103cd73a77e1a34996c,
operations §04 and control §05. Source authority: IBM Enterprise COBOL 6.4 Basic
PERFORM, printed pp. 413–414 in the [official manual](https://publibfp.dhe.ibm.com/epubs/pdf/igy6lr40.pdf),
read 2026-09-12. Source rules enter this lower through PerformFact, never COBOL/AST.

SPECIFICATION_GUARANTEED: Jump explicitly names its destination; Assign evaluates
in prior state. SOURCE_CONTRACT: single unique local paragraph, one callsite,
linear nonempty MOVE body, unique resume and closed isolated primary flow.
ARCHITECTURE_GUARANTEED: typed IDs, membership and entry govern joins; physical
JSON/statement/sequence order is irrelevant.

Admission validates one PerformFact/CALL/GOBACK, disjoint complete primary/body
membership, endpoints, every explicit completion, exact provenance and scalar
storage. Multiple items require the existing IndependentStorageSet. Primary flow
is optional MOVE prefix, PERFORM, CALL, GOBACK. Missing facts refuse without repair.

Every admitted body state has the same pending return. Erasing that constant
preserves execution through Jump(target), MOVE transfers, Jump(resume). Two
callsites need different returns; fallthrough/GO TO adds activation without that
continuation; THRU changes the exit; loops/nested control change traversal. These
forms remain unsupported, with no generic stack or contextual solver.

Four sequences: primary prefix/Jump, target Assigns/Jump, resume Invoke, final
Return. Entry points to primary even though it serializes last. PERFORM has no
Invoke or external program dependency. Origins retain callsite, reference,
paragraph, body statements and resume. Existing MOVE/storage translations are reused.

Complexity is O(statements + data + members) through indexed passes; no path
enumeration, backward scan, RD or Def-Use. PerformOracle checks in-memory control
via source correlations. PerformIntegrationSuite exercises real SP fixtures,
codec roundtrip, physical SP permutation and wrong target/end/resume/body/entry,
second-callsite and unsupported-profile challenges. Both FAST and Full run it.

## SP1.7 composition

The earlier four-sequence shape above remains a regression fixture. The public
compositor now admits arbitrary supported root statements before/after the single
PERFORM and any supported root resume. [Current profile](supported-program.md).
The target isolation and MOVE body semantics are unchanged.
