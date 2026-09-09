# Capacity and safety inventory

| Limit | Enforcement | Original rationale | Classification / disposition |
| --- | --- | --- | --- |
| SP_BYTES 32MiB | CobolLower.INPUT_LIMITS → SpFileInput.readNBytes plus probe; SpJsonDecoder.decodePayload byte length | CP4C measured 18,809,400 bytes plus headroom | artificial total capacity; remove APIs and checks |
| SP_NODES 1,500,000 | CobolLower.INPUT_LIMITS → decoder node traversal | CP4C 1,050,154 nodes plus headroom | artificial capacity; retain long telemetry, remove comparison |
| ADMISSION_ENTITIES 250,000 | CobolLower.OPTIONS → EntryGobackAdmission.Context.touch used by both admissions | CP4C 190,021 visits plus headroom | artificial work budget; remove comparison/API, retain ledger |
| SP_DEPTH 64 | Jackson maxNestingDepth before recursive Wire shape validation | stack/pathological nesting protection | retain; supported flat arrays are depth-independent (CP4C depth8 including leaves) |
| Jackson string length tied to maxBytes | decoder factory maxStringLength | byte budget duplicated on individual strings | remove artificial string ceiling so a single supported value cannot restore size rejection |
| Jackson document/token defaults | parser dependency 2.22.2 | default unlimited | verify no hidden total cap |
| Jackson number/name lengths | parser dependency | lexical numeric safety / schema property names | retain; wire numbers are bounded integral values and schema names are fixed |
| 100 diagnostics | Context.require | bounded reporting after actual invalid facts | retain; never rejects a valid input |
| maximumIdentityCharacters | CanonicalRevision | final 32-character ID allocation, no longer total canonical volume | retain; input size cannot grow final ID |
| integer framing overflow / handle parse | CanonicalRevision.token, admission identities | representability / coherent handles | retain actual overflow protection |
| AIR validation options and 16MiB/depth128 codec | CobolLowerer → AirValidator; AirFileOutput → AirJson | independent pinned AIR boundary | unchanged; separate air-java work |

Search included every production Java source: limits/max/budget/counters, byte/node/entity/visit comparisons, collection sizes and numeric thresholds. Structural cardinalities (one primary entry, linear sequence, complete facts) define the existing semantic profile. No new global scan, index, serialization or whole-document representation is planned. Existing byte[]/JSON tree/Wire/SpInput/Publication amplification remains BACKLOG-LOWER-018.
