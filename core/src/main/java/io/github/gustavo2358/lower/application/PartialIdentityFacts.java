package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.lower.domain.SpInput;
import java.util.function.Consumer;

/** Explicit SP identity fields. No transport, reflection, or semantic reconstruction. */
final class PartialIdentityFacts {
    private PartialIdentityFacts() { }
    static void write(Object fact,Consumer<String> field,Consumer<Object> value) {
        switch(fact) {
            case SpInput r -> {
                field.accept("SpInput");
                field.accept("unit"); value.accept(r.unit());
                field.accept("policy"); value.accept(r.policy());
                field.accept("dataDeclarations"); value.accept(r.dataDeclarations());
                field.accept("statements"); value.accept(r.statements());
                field.accept("structure"); value.accept(r.structure());
                field.accept("gaps"); value.accept(r.gaps());
                field.accept("coverage"); value.accept(r.coverage());
                field.accept("entryInventory"); value.accept(r.entryInventory());
                field.accept("storageIndependence"); value.accept(r.storageIndependence());
                field.accept("compositional"); value.accept(r.compositional());
            }
            case SpInput.UnitKey r -> {
                field.accept("UnitKey");
                field.accept("compilationUnitId"); value.accept(r.compilationUnitId());
                field.accept("structuralPath"); value.accept(r.structuralPath());
                field.accept("canonicalProgramName"); value.accept(r.canonicalProgramName());
            }
            case SpInput.StatementId r -> {
                field.accept("StatementId");
                field.accept("unit"); value.accept(r.unit());
                field.accept("handle"); value.accept(r.handle());
            }
            case SpInput.EntryId r -> {
                field.accept("EntryId");
                field.accept("unit"); value.accept(r.unit());
                field.accept("handle"); value.accept(r.handle());
            }
            case SpInput.DataId r -> {
                field.accept("DataId");
                field.accept("unit"); value.accept(r.unit());
                field.accept("handle"); value.accept(r.handle());
            }
            case SpInput.Policy r -> {
                field.accept("Policy");
                field.accept("policyId"); value.accept(r.policyId());
                field.accept("version"); value.accept(r.version());
                field.accept("qualifyMode"); value.accept(r.qualifyMode());
                field.accept("pgmnameMode"); value.accept(r.pgmnameMode());
                field.accept("dynamMode"); value.accept(r.dynamMode());
                field.accept("dllMode"); value.accept(r.dllMode());
            }
            case SpInput.Location r -> {
                field.accept("Location");
                field.accept("file"); value.accept(r.file());
                field.accept("startLine"); value.accept(r.startLine());
                field.accept("startColumn"); value.accept(r.startColumn());
                field.accept("endLine"); value.accept(r.endLine());
                field.accept("endColumn"); value.accept(r.endColumn());
            }
            case SpInput.IncludeFrame r -> {
                field.accept("IncludeFrame");
                field.accept("includingFile"); value.accept(r.includingFile());
                field.accept("requestedName"); value.accept(r.requestedName());
                field.accept("includedFile"); value.accept(r.includedFile());
                field.accept("includeLine"); value.accept(r.includeLine());
            }
            case SpInput.Provenance r -> {
                field.accept("Provenance");
                field.accept("expanded"); value.accept(r.expanded());
                field.accept("original"); value.accept(r.original());
                field.accept("includeChain"); value.accept(r.includeChain());
                field.accept("exact"); value.accept(r.exact());
            }
            case SpInput.ReadinessClaim r -> {
                field.accept("ReadinessClaim");
                field.accept("status"); value.accept(r.status());
                field.accept("scope"); value.accept(r.scope());
            }
            case SpInput.Readiness r -> {
                field.accept("Readiness");
                field.accept("lowering"); value.accept(r.lowering());
                field.accept("cfg"); value.accept(r.cfg());
                field.accept("effectsDataflow"); value.accept(r.effectsDataflow());
            }
            case SpInput.DataFact r -> {
                field.accept("DataFact");
                field.accept("id"); value.accept(r.id());
                field.accept("canonicalName"); value.accept(r.canonicalName());
                field.accept("picture"); value.accept(r.picture());
                field.accept("provenance"); value.accept(r.provenance());
                field.accept("coverage"); value.accept(r.coverage());
                field.accept("readiness"); value.accept(r.readiness());
                field.accept("scalarText"); value.accept(r.scalarText());
            }
            case SpInput.Containment r -> {
                field.accept("Containment");
                field.accept("parent"); value.accept(r.parent());
                field.accept("branch"); value.accept(r.branch());
            }
            case SpInput.StatementHeader r -> {
                field.accept("StatementHeader");
                field.accept("id"); value.accept(r.id());
                field.accept("programPoint"); value.accept(r.programPoint());
                field.accept("containment"); value.accept(r.containment());
                field.accept("provenance"); value.accept(r.provenance());
                field.accept("coverage"); value.accept(r.coverage());
                field.accept("readiness"); value.accept(r.readiness());
            }
            case SpInput.ProcedureId r -> {
                field.accept("ProcedureId");
                field.accept("unit"); value.accept(r.unit());
                field.accept("handle"); value.accept(r.handle());
            }
            case SpInput.PerformTarget r -> {
                field.accept("PerformTarget");
                field.accept("id"); value.accept(r.id());
                field.accept("referenceOrigin"); value.accept(r.referenceOrigin());
                field.accept("paragraphOrigin"); value.accept(r.paragraphOrigin());
            }
            case SpInput.PerformFact r -> {
                field.accept("PerformFact");
                field.accept("header"); value.accept(r.header());
                field.accept("profile"); value.accept(r.profile());
                field.accept("target"); value.accept(r.target());
                field.accept("targetEntry"); value.accept(r.targetEntry());
                field.accept("targetStatements"); value.accept(r.targetStatements());
                field.accept("targetExit"); value.accept(r.targetExit());
                field.accept("normalContinuation"); value.accept(r.normalContinuation());
                field.accept("primaryStatements"); value.accept(r.primaryStatements());
                field.accept("gapCodes"); value.accept(r.gapCodes());
            }
            case SpInput.GobackFact r -> {
                field.accept("GobackFact");
                field.accept("header"); value.accept(r.header());
                field.accept("exit"); value.accept(r.exit());
                field.accept("localContinuation"); value.accept(r.localContinuation());
            }
            case SpInput.OperandId r -> {
                field.accept("OperandId");
                field.accept("statement"); value.accept(r.statement());
                field.accept("handle"); value.accept(r.handle());
            }
            case SpInput.ScalarText r -> {
                field.accept("ScalarText");
                field.accept("logicalDomain"); value.accept(r.logicalDomain());
                field.accept("logicalExtent"); value.accept(r.logicalExtent());
                field.accept("storageClass"); value.accept(r.storageClass());
                field.accept("declarationScope"); value.accept(r.declarationScope());
            }
            case SpInput.LogicalValue r -> {
                field.accept("LogicalValue");
                field.accept("logicalDomain"); value.accept(r.logicalDomain());
                field.accept("value"); value.accept(r.value());
                field.accept("logicalExtent"); value.accept(r.logicalExtent());
            }
            case SpInput.LiteralSource r -> {
                field.accept("LiteralSource");
                field.accept("id"); value.accept(r.id());
                field.accept("kind"); value.accept(r.kind());
                field.accept("logicalValue"); value.accept(r.logicalValue());
                field.accept("provenance"); value.accept(r.provenance());
            }
            case SpInput.Binding r -> {
                field.accept("Binding");
                field.accept("status"); value.accept(r.status());
                field.accept("candidates"); value.accept(r.candidates());
                field.accept("selected"); value.accept(r.selected());
                field.accept("reason"); value.accept(r.reason());
                field.accept("candidateNames"); value.accept(r.candidateNames());
            }
            case SpInput.WholeItemAccess r -> {
                field.accept("WholeItemAccess");
                field.accept("data"); value.accept(r.data());
            }
            case SpInput.DataReference r -> {
                field.accept("DataReference");
                field.accept("id"); value.accept(r.id());
                field.accept("role"); value.accept(r.role());
                field.accept("binding"); value.accept(r.binding());
                field.accept("wholeItemAccess"); value.accept(r.wholeItemAccess());
                field.accept("provenance"); value.accept(r.provenance());
            }
            case SpInput.NormalContinuation r -> {
                field.accept("NormalContinuation");
                field.accept("availability"); value.accept(r.availability());
                field.accept("statement"); value.accept(r.statement());
                field.accept("provenance"); value.accept(r.provenance());
            }
            case SpInput.TextAdjustment r -> {
                field.accept("TextAdjustment");
                field.accept("rule"); value.accept(r.rule());
                field.accept("receiverExtent"); value.accept(r.receiverExtent());
                field.accept("result"); value.accept(r.result());
                field.accept("provenance"); value.accept(r.provenance());
            }
            case SpInput.MoveFact r -> {
                field.accept("MoveFact");
                field.accept("header"); value.accept(r.header());
                field.accept("source"); value.accept(r.source());
                field.accept("target"); value.accept(r.target());
                field.accept("copySemantics"); value.accept(r.copySemantics());
                field.accept("normalContinuation"); value.accept(r.normalContinuation());
                field.accept("textAdjustment"); value.accept(r.textAdjustment());
            }
            case SpInput.LiteralCallTarget r -> {
                field.accept("LiteralCallTarget");
                field.accept("id"); value.accept(r.id());
                field.accept("text"); value.accept(r.text());
                field.accept("writtenText"); value.accept(r.writtenText());
                field.accept("logicalValue"); value.accept(r.logicalValue());
                field.accept("provenance"); value.accept(r.provenance());
            }
            case SpInput.DataCallTarget r -> {
                field.accept("DataCallTarget");
                field.accept("reference"); value.accept(r.reference());
            }
            case SpInput.CallSurface r -> {
                field.accept("CallSurface");
                field.accept("using"); value.accept(r.using());
                field.accept("argumentCount"); value.accept(r.argumentCount());
                field.accept("returning"); value.accept(r.returning());
                field.accept("onException"); value.accept(r.onException());
                field.accept("notOnException"); value.accept(r.notOnException());
                field.accept("onOverflow"); value.accept(r.onOverflow());
            }
            case SpInput.CallFact r -> {
                field.accept("CallFact");
                field.accept("header"); value.accept(r.header());
                field.accept("syntax"); value.accept(r.syntax());
                field.accept("target"); value.accept(r.target());
                field.accept("runtimeTarget"); value.accept(r.runtimeTarget());
                field.accept("runtimeUncertaintyCode"); value.accept(r.runtimeUncertaintyCode());
                field.accept("normalContinuation"); value.accept(r.normalContinuation());
                field.accept("surface"); value.accept(r.surface());
                field.accept("effects"); value.accept(r.effects());
                field.accept("outcomes"); value.accept(r.outcomes());
            }
            case SpInput.PredicateGuarantee r -> {
                field.accept("PredicateGuarantee");
                field.accept("availability"); value.accept(r.availability());
                field.accept("profile"); value.accept(r.profile());
                field.accept("resultDomain"); value.accept(r.resultDomain());
                field.accept("evaluation"); value.accept(r.evaluation());
                field.accept("normalCompletion"); value.accept(r.normalCompletion());
                field.accept("readsCompleteness"); value.accept(r.readsCompleteness());
                field.accept("truthValue"); value.accept(r.truthValue());
                field.accept("knownReads"); value.accept(r.knownReads());
                field.accept("provenance"); value.accept(r.provenance());
                field.accept("gapCodes"); value.accept(r.gapCodes());
            }
            case SpInput.IfArm r -> {
                field.accept("IfArm");
                field.accept("presence"); value.accept(r.presence());
                field.accept("contentAvailability"); value.accept(r.contentAvailability());
                field.accept("entry"); value.accept(r.entry());
                field.accept("provenance"); value.accept(r.provenance());
                field.accept("gapCodes"); value.accept(r.gapCodes());
            }
            case SpInput.EvaluateArm r -> {
                field.accept("EvaluateArm");
                field.accept("ordinal"); value.accept(r.ordinal());
                field.accept("selection"); value.accept(r.selection());
                field.accept("statements"); value.accept(r.statements());
                field.accept("control"); value.accept(r.control());
            }
            case SpInput.EvaluateFact r -> {
                field.accept("EvaluateFact");
                field.accept("header"); value.accept(r.header());
                field.accept("subject"); value.accept(r.subject());
                field.accept("arms"); value.accept(r.arms());
                field.accept("otherArm"); value.accept(r.otherArm());
                field.accept("otherStatements"); value.accept(r.otherStatements());
                field.accept("normalContinuation"); value.accept(r.normalContinuation());
                field.accept("gapCodes"); value.accept(r.gapCodes());
            }
            case SpInput.IfFact r -> {
                field.accept("IfFact");
                field.accept("header"); value.accept(r.header());
                field.accept("conditionShape"); value.accept(r.conditionShape());
                field.accept("predicateGuarantee"); value.accept(r.predicateGuarantee());
                field.accept("conditionReads"); value.accept(r.conditionReads());
                field.accept("conditionProvenance"); value.accept(r.conditionProvenance());
                field.accept("explicitlyTerminated"); value.accept(r.explicitlyTerminated());
                field.accept("continuation"); value.accept(r.continuation());
                field.accept("normalContinuation"); value.accept(r.normalContinuation());
                field.accept("thenArm"); value.accept(r.thenArm());
                field.accept("elseArm"); value.accept(r.elseArm());
                field.accept("profile"); value.accept(r.profile());
            }
            case SpInput.IndependentStorageSet r -> {
                field.accept("IndependentStorageSet");
                field.accept("availability"); value.accept(r.availability());
                field.accept("rule"); value.accept(r.rule());
                field.accept("authority"); value.accept(r.authority());
                field.accept("members"); value.accept(r.members());
                field.accept("provenance"); value.accept(r.provenance());
                field.accept("gapCodes"); value.accept(r.gapCodes());
            }
            case SpInput.OtherStatement r -> {
                field.accept("OtherStatement");
                field.accept("header"); value.accept(r.header());
                field.accept("variant"); value.accept(r.variant());
                field.accept("observedKind"); value.accept(r.observedKind());
                field.accept("gapCode"); value.accept(r.gapCode());
                field.accept("normalContinuation"); value.accept(r.normalContinuation());
                field.accept("knownReferences"); value.accept(r.knownReferences());
            }
            case SpInput.Gap r -> {
                field.accept("Gap");
                field.accept("statement"); value.accept(r.statement());
                field.accept("scope"); value.accept(r.scope());
                field.accept("code"); value.accept(r.code());
                field.accept("detail"); value.accept(r.detail());
                field.accept("provenance"); value.accept(r.provenance());
            }
            case SpInput.EntryGap r -> {
                field.accept("EntryGap");
                field.accept("scope"); value.accept(r.scope());
                field.accept("code"); value.accept(r.code());
                field.accept("detail"); value.accept(r.detail());
                field.accept("provenance"); value.accept(r.provenance());
            }
            case SpInput.ExecutableStart r -> {
                field.accept("ExecutableStart");
                field.accept("availability"); value.accept(r.availability());
                field.accept("statement"); value.accept(r.statement());
            }
            case SpInput.EntrySignature r -> {
                field.accept("EntrySignature");
                field.accept("availability"); value.accept(r.availability());
                field.accept("parameterCount"); value.accept(r.parameterCount());
                field.accept("returningClause"); value.accept(r.returningClause());
            }
            case SpInput.EntryFact r -> {
                field.accept("EntryFact");
                field.accept("id"); value.accept(r.id());
                field.accept("role"); value.accept(r.role());
                field.accept("availability"); value.accept(r.availability());
                field.accept("start"); value.accept(r.start());
                field.accept("signature"); value.accept(r.signature());
                field.accept("provenance"); value.accept(r.provenance());
                field.accept("coverage"); value.accept(r.coverage());
                field.accept("readiness"); value.accept(r.readiness());
                field.accept("gaps"); value.accept(r.gaps());
            }
            case SpInput.EntryInventory r -> {
                field.accept("EntryInventory");
                field.accept("status"); value.accept(r.status());
                field.accept("scope"); value.accept(r.scope());
                field.accept("entries"); value.accept(r.entries());
                field.accept("gapCodes"); value.accept(r.gapCodes());
            }
            case SpInput.BranchChildren r -> {
                field.accept("BranchChildren");
                field.accept("parent"); value.accept(r.parent());
                field.accept("branch"); value.accept(r.branch());
                field.accept("children"); value.accept(r.children());
            }
            case SpInput.Structure r -> {
                field.accept("Structure");
                field.accept("roots"); value.accept(r.roots());
                field.accept("branches"); value.accept(r.branches());
            }
            case SpInput.Coverage r -> {
                field.accept("Coverage");
                field.accept("inventoryStatus"); value.accept(r.inventoryStatus());
                field.accept("observedStatements"); value.accept(r.observedStatements());
                field.accept("modeledStatements"); value.accept(r.modeledStatements());
                field.accept("partialStatements"); value.accept(r.partialStatements());
                field.accept("unsupportedStatements"); value.accept(r.unsupportedStatements());
                field.accept("inputMissingStatements"); value.accept(r.inputMissingStatements());
                field.accept("readiness"); value.accept(r.readiness());
            }
            default -> throw new IllegalArgumentException("unsupported SP identity fact");
        }
    }
}
