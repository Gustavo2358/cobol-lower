# Production source fixture

`invariant.json` is unmodified ExplorerMain output from `invariant.cbl` (source
filename during generation: call-unknown.cbl). Frontend commit
f853bc3c06a0aed3259929926fbcd556969c225c, SP 2.15.0/storage 1.4.0.

CLI: `ExplorerMain --source invariant.cbl --copybooks . --output frontend
--storage-profile ibm-enterprise-6.4-fixed-display-1047@1`. Entry mode is default
UNKNOWN. The lower test checks physical IBM1047 bytes for `PROGA   ` and explicit
DECLARATIVE_INVARIANT proof. Test mutations are synthetic contract challenges;
they are never relabeled as frontend output.
