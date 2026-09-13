# SP 1.9 entry admission

SP 1.9 preserves the SP 1.8 wire fields, with a frontend-qualified primary
executable start independent of missing DATA input. The decoder accepts both
versions through the same typed materialization and validation. Older supported
versions remain accepted. No lowering algorithm, AIR contract, lattice, or
COBOL-specific rule changes here.

The [frontend rule](https://github.com/Gustavo2358/proleap-poc/blob/38b8d7d8cf5b363ec4bda0d2517450b6b1b6ffdd/docs/architecture/entry-localized-input.md)
owns the proof. Entry inventory and signature may remain INPUT_MISSING while the
explicit primary start is KNOWN. Existing partial-program admission consumes
that reference, preserves partial coverage and conservatively lowers unknown
values/storage. An unavailable start still blocks. Missing PROCEDURE COPY is
not made executable by this change.

`EntryLocalizationSuite` uses real SP outputs produced by the pinned frontend's
`LocalizedInputCompletenessContractTest` (`target/entry-localization`). It checks
DATA input gaps and unavailable storage, known entry admission, CALL retention
in AIR, and refusal of unsafe executable gaps. It runs in FAST and the local
decoder suite; the historical 1.8 partial fixture stays accepted.
