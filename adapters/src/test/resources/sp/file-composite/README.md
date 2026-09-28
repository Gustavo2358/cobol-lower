# Composite FILE contract witnesses

These are real frontend publications, frozen for the adapter FAST without a
runtime frontend dependency. They exercise SP 2.51 / topology R2. The source
witnesses are versioned in analysis-cfg under
`analysis-adapters/src/test/resources/file-dependencies/composite/` with matching
names. `no-event-model.json` comes from frontend
`FileCompositeTopologyTest.structuralFlowDoesNotRequireMemoryEventAdmission`.

The independent oracle is `FileCompositeFlowSuite`: handwritten operation traces,
ownership/rejection properties, context returns and permutation invariance. The
serialized products are inputs, not generated expected graphs. The source E2E in
analysis-cfg regenerates publications using locked production CLIs.

Regeneration: run the frontend CLI with storage profile
`ibm-enterprise-6.4-fixed-display-1047@1`, initial entry storage and
`new-logical-level` CICS entry; use the E2E runner for the complete invocation.
For the structural-only witness run the named frontend JUnit test and copy
`target/file-composite-topology/no-event-model.json`.
