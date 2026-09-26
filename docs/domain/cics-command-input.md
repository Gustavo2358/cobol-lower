# SP2.43 CICS command preservation

R7-R6 explicitly admits SP2.43 in the closed version profile. It inherits 2.42 and
adds a lower-owned CicsCommandFact: header, commandKind, syntaxStatus, rawText,
ordered CicsOption records, gapCodes. Supported kinds are SYNCPOINT, RECEIVE_MAP,
SEND_MAP. Future versions remain unsupported before semantic shape validation.
Old versions cannot carry this fact or its new control proof family.

Domain validation checks family/options, offsets/order, duplicates, operand
shape, canonical operand owners/roles and syntax-status/gap consistency. Existing
CallAdmission reference validation applies before NOT_READY, including canonical
typed CICS host access. It does not parse rawText or resolve target syntax.

ControlTopology and FactDependencies are preserved. Control already published by
the frontend is the only execution authority. HandlerStateAnalyzer is unchanged.
No command-specific successor is reconstructed in this consumer. Source condition
remainders remain UNKNOWN_LOCAL alongside a positive ordinary outcome.

CicsCommandFact.executableLowering() is NOT_READY. Facts survive in admitted typed
input; factual validation precedes nonexecutable handler assessment and the
readiness barrier. No AIR publication, new operation, dependency or runtime
handler dispatch is produced. Documents previously lowered with opaque CICS may
now report IMPLEMENTATION_LIMIT because explicit command effects are not yet
qualified for executable AIR; this is reported, not hidden by dropping facts.

Transport first loss: version/sum-type admission lacked SP2.43/CICS_COMMAND.
The existing input domain and pre-AIR analysis suffice; AIR/CFG contracts require
no expansion. Historical 2.40/2.41/2.42 bytes retain their meaning.
