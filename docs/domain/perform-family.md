# PERFORM family lowering

Goal: THRU/THROUGH + UNTIL + TIMES + VARYING, one continuous wave.
Source rule and producer proof: frontend `docs/domain/perform-family.md`, IBM
Enterprise COBOL 6.4 [PERFORM pp. 413–424](https://publibfp.dhe.ibm.com/epubs/pdf/igy6lr40.pdf),
consulted 2026-09-13. AIR 2.0 Jump/Branch/Assign/Invoke/Return remain sufficient.

SP 2.2 publishes typed ordered paragraphs, members, executable entries and normal
completion frontiers. Lower validates identities, published internal edges, body
closure and isolation before specializing an activation. A completion transfers
to the next explicitly ordered paragraph or the callsite resume. GO TO uses its
published target, and GOBACK stays terminal. No source/name/ProgramPoint inference.

The finite graph check rejects contradictory facts as INVALID_INPUT; partial
facts retain conservative control. Unequal overlaps, incoming ordinary transfers,
escapes, cycles and recursive PERFORMs cannot manufacture isolated returns.
The finite proof is bounded by O(P * (N + M)) for P activations, N source
statements and M total range membership; assembly is linear in emitted body size. Each activation
has its own IDs; no iteration-count unrolling, solver, lattice or RD change.

Oracles: source strong updates, explicit-edge inspection, distinct resumes,
malformed endpoints/membership/completion/resume, permutation and cumulative E2E.
Historical decoders remain closed and retain their previous semantics.

SP 2.3 adds an optional UNTIL loop: one activation entry Jump, one Branch with
an Unknown BOOL predicate and its published read dependencies, plus the same
range body. BEFORE enters the decision; AFTER enters the body. Range completion
targets the decision, true targets the callsite resume and false targets the
range entry. There is no predicate pruning, path enumeration or body unrolling.
Predicate admission and expression projection are shared with IF. An unproved
condition stays Opaque with its known reads and open control. SP 2.2 and older
decoders remain unchanged; a 2.2 document cannot carry the new loop field.
