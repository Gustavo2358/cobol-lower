# Bounded local CICS producer fixtures

Synthetic source processed by real frontend d9feb884e2cc81ec842a7d8561a2da9425b8553b, SP2.40.0, default UNKNOWN CICS entry mode, no physical profile. CLI: ExplorerMain --source source/<case>.cbl --copybooks <empty-directory> --output <output-directory>. JSON is the unchanged producer product.

context-resumes: statement:6 XCTL has two distinct PERFORM activations. Local condition completions go to statement:0 (FIRST001) and statement:1 (SECOND01); no route to statement:2 (DEAD0001) at the ordinary next paragraph. No successful XCTL return.

distinct-equal: statement:1 and statement:2 are different XCTLs with identical text and literal target. Both are preserved; local destinations are respectively statement:2 and statement:0. The test never deduplicates by spelling.

The existing frozen typed-occurrence-r5/I-invokes fixture supplies the straight-line known-local case, and A-known/B-unavailable supply unknown-control negatives. No historical fixture is changed.
