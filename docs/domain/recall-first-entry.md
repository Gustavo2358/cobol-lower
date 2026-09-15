# RF-W1 — possible declarative entry

Status: IN_PROGRESS, campaign RF-W0 through RF-W4. The user authorized continuing frontend PR #52. Exact normative/frontend/AIR pins are in sources.lock.json.

SP 2.16.0/storage 1.5.0 adds POSSIBLE_LITERAL_BYTES / DECLARATIVE_POSSIBILITY. It requires a proven local allocation, exact view, fitting source bytes and ENTRY_STATE_NOT_PROVEN. The existing wire shape is reused with explicit version/enum admission; older versions cannot impersonate this new form.

RegionalEntryTranslator emits Entries.PossibleLiterals with a VALUES remainder scoped to the entry, and the publication requires entry.possibilities@1. Initial state is declarative, not an instruction. Strong literal initialization and explicit PRESERVED semantics stay unchanged. The generic dataflow governs later MAY/MUST writes.

PossibleEntrySuite uses a source-produced fixture from frontend 8ff3b03 and checks exact bytes, typed possibility, required remainder/capability, no synthetic write, JSON roundtrip and six malformed cases. InitialStorageSuite, DeclarativeValueSuite and MixedInitialStorageSuite preserve the previous entry forms. No full lower/corpus campaign is needed for this entry contract boundary; selected vertical evidence follows in the campaign handoff.
