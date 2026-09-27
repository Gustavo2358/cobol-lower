# Storage boundary dependency oracles

These fixtures test the production chain through the compilation envelope.
`expected.json` independently specifies candidates and materialized edges at each
CALL source line. The oracle also checks provenance support and reachability.

- `global_fd_may` / `global_fd_scalar`: GLOBAL record capture and READ keep the
  prior candidate under a MAY write; a subsequent proven MOVE kills that value.
- `invalid_overlay` / `invalid_renames`: invalid relationships retain uncertainty
  without discarding independent statements and dependencies.
- `filler_overlay`: anonymous storage identity does not invent a nominal object
  or prevent a proven write to the named view.
- `renames_nonallocating`: a level-66 view shares its record's value, and does not
  become a second physical child or an independent allocation.

Seven incomplete file adversaries extend these laws: FD without SELECT (local
and GLOBAL), SELECT without FD, FROM without owner/receiver/source, and missing
INTO receiver. Every case requires the old target with unknown remainder after
MAY, the new target after a proved MOVE, and the independent source candidate.
They do not claim that incomplete inputs are valid COBOL programs.

Run with a frozen runtime manifest (checkouts, source SHAs, per-stage main classes
and classpaths, and artifact hashes), including the analysis-cfg dependency oracle:

```sh
python3 scripts/harness/storage_boundary_e2e.py \
  --runtime /path/to/runtime.json --work /path/to/new-evidence-directory
```

No physical opt-in, altered expected set or source rewriting is used.
