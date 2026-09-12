# PERFORM BASIC fixtures

Real COBOL parsed by frontend PerformBasicTest at source HEAD
`a2e9645a2d0fa5e86befdc6f60c6bf2b5cf6f84b`, SP 1.6.0. The test parser names its
input `scalar.cbl`; those provenance names are retained verbatim. Literal and
overwrite have one scalar item; copy has two and retains IndependentStorageSet.

The adapter's public decoder and in-memory lower consume these exact bytes.
Full analysis-cfg qualification separately executes the actual producer CLI from
these COBOL shapes at the locked final producer HEADs, twice, with final CALL/value
oracles. These resource snapshots are not a substitute for that real E2E.
