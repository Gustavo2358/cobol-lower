# FILE authority review fixture

SP2.39 produced by frontend `FileTopologyAuthorityTest.everyFileDestinationHasAnAuthoritativeOutcome`
using the unchanged R1 producer at aa5671efcd238575480e2ec855ddca5dbf10f59d.
The source fixture is defined in that test: a performed paragraph with DELETE,
INVALID KEY and NOT INVALID KEY, two distinguishable DISPLAYs per handler.
Explicit IBM6.4/1047 is a fixture premise, not a corpus default.

Regenerate with the frontend test, then copy
`target/control-topology-r1-r1/file-authority.json` here.
SHA256: `2adacfc525e47285f5d8eb435656b1ee3d91fbf59bc95a5e9f65953b5ab02f0f`.

The producer test verifies event role coverage and handler-region entry. Consumer
mutations reverse only legacy handler members (AIR must be unchanged modulo
publication namespace), or remove a topology outcome and its occurrence reference
(both wire and typed admission must reject before AIR). Original R1 migration
fixtures current.json and legacy.json are unchanged.
