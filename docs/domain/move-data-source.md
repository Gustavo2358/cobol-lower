# Scalar MOVE data source — SP 1.5.0

The coordinated source contract adds a typed DATA source to MOVE. Wire15 is a
closed reader; historical Wire11–14 retain their existing shapes. SpInput has a
sealed MoveSource (LiteralSource or DataReference). No COBOL or AST is consulted.

Authority: pinned SP `docs/domain/move-data-source.md` and AIR 2.0.0 operations
Assign/Read (analysis-ir 51b4d9a8ae0364232bd97103cd73a77e1a34996c). The verified
[IBM MOVE rule](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=statements-move-statement)
is the frontend's responsibility; the lower consumes its claims only.

SPECIFICATION_GUARANTEED: FULL_IDENTITY, unique READ and WRITE whole-item scalar
TEXT with equal extents. Literal fitting remains the published adjusted value.
DATA becomes Assign(ObjectPlace(target), Read(ObjectPlace(source))), preserving
statement, source and target origins and roles. The source value is not evaluated.
IndependentStorageSet, when KNOWN, uses the existing StoragePremise translation;
no disjointness is inferred from object identity. Missing proof remains missing.

Finite indexed admission and one translation per MOVE cost O(facts + references
+ provenance + literal text), with no search from CALL or copy hop bound.
Oracle: literal versus Read, correct ObjectIds, roles/origins, same extents,
source binding/access refusal, unchanged W1 fitting and W2 shape admission.
AIR model/JSON/normative contracts are unchanged. IF composer is unchanged.
