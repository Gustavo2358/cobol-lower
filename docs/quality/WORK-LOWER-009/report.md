# Post-CP5 compatible baseline synchronization

The frozen lower main fe7e6ef includes the scalar slice already validated at2329993 and subsequently approved input capacity and certification fixes. The exact source-lock advances are in upstream-deltas.json; all production Java, POMs and fixture bytes remain unchanged by this work.

## Compatibility

Normative AIR122→51 changed only README/informative baseline notes. Air-java ce530→170 approved capacity/diagnostic semantics independently; pure normative repin PR8 merged3bafe397 with identical production JARs. SP2815→872 retains byte-identical semantic JSON writer, AIR-MOVE fixture and scalar contract;872 is the existing W5 producer.

Four test files now use approved RESOURCE_LIMIT terminology and explicit16MiB budgets for atomic rejection tests. The default10k case now asserts success, all10000 Assigns, one Object/Cell, Return, structural validity and byte-exact round-trip. No production limits are locally raised. The harness requires the successful probe marker; no suite, gate or semantic fact oracle is removed.

## Executed evidence

Fresh bootstrap from merged AIR3bafe397 installed into isolated LOWER_BUILD_ROOT/m2. Semantic203099 and performance93 checks passed; real10k route produced112117041 AIR bytes. Canonical external CP4E A/B, CP3 and generic overwrite ran all stages in fresh processes, including CFG15bd3afe and memory/file comparison. COBOL/SP/AIR/result bytes match historical CP5 exactly; see canonical-comparison.json and canonical-e2e.tar.gz raw outputs/receipts (per-file hashes in canonical-comparison.json).

Full/certification and final exact-head CI status will be recorded in CP0.json and post-push remote receipt. No unexecuted gate is claimed here.

## Remaining debts

Former default AIR16MiB limit: RESOLVED_BY_UPSTREAM_BASELINE for the tested10k route. Codec/validator operational budgets remain explicit and fail closed. Lower transport memory amplification: STILL_OPEN. No117k program or broad production/SLA qualification; no CP6, Invoke, CALL or domain resolver.

## Documentary closeout harness

Existing CI selected a closed branch HEAD for new execution and rejected the required historical lifecycle. A narrow Work-Item-Closeout trailer now selects its direct certified parent for audit only after proving a metadata-only Git delta, byte-identical certificate and actual PR linkage. No production, test, pin, contract or unrelated registry changes are allowed in that closing commit. Initial CP0 null PR certificates retain their bytes and acquire their real PR binding through history plus remote validation. Twelve positive/adversarial tests and the existing19 CI tests passed. An isolated removed-production-guard mutation was killed and exactly restored with a second GREEN. CI explicitly uses the same3GiB Maven heap as local validation; no product size/count policy changed.
