package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.lower.domain.FileFacts;
import java.util.function.Consumer;

/** Explicit SP identity fields. No transport, reflection, or semantic reconstruction. */
final class PartialIdentityFacts {
    private PartialIdentityFacts() { }
    static void write(Object fact,Consumer<String> field,Consumer<Object> value) {
        switch(fact) {
            case SpInput.CicsHandlerFact r -> {field.accept("CicsHandlerFact");value.accept(r.header());value.accept(r.handlerKind());value.accept(r.action());value.accept(r.targetKind());value.accept(r.targetSyntax());value.accept(r.labelBindingStatus());value.accept(r.labelTarget());value.accept(r.targetEntry());value.accept(r.entryOrigin());value.accept(r.targetOrigin());value.accept(r.programTarget());value.accept(r.scope());value.accept(r.rawText());value.accept(r.options());value.accept(r.gapCodes());}
            case SpInput.CicsHandlerScope r -> {field.accept("CicsHandlerScope");value.accept(r.kind());value.accept(r.runtimeIdentity());value.accept(r.provenance());}
            case SpInput.CicsHandlerLabelTarget r -> {field.accept("CicsHandlerLabelTarget");value.accept(r.id());value.accept(r.declarationOrigin());}
            case SpInput.CicsAbendFact r -> {field.accept("CicsAbendFact");value.accept(r.header());value.accept(r.eventKind());value.accept(r.dispatchEligibility());value.accept(r.rawText());value.accept(r.options());value.accept(r.gapCodes());}
            case SpInput.CicsCommandFact r -> {field.accept("CicsCommandFact");value.accept(r.header());value.accept(r.commandKind());value.accept(r.syntaxStatus());value.accept(r.rawText());value.accept(r.options());value.accept(r.gapCodes());if(r.length().isPresent())value.accept(r.length());}
            case SpInput.OperandExpression r -> {field.accept("OperandExpression");value.accept(r.kind());value.accept(r.integer());value.accept(r.reference());value.accept(r.provenance());}
            case io.github.gustavo2358.lower.domain.FactDependencies r -> {field.accept("FactDependencies");value.accept(r.authority());value.accept(r.inputs());value.accept(r.proofs());value.accept(r.regions());value.accept(r.facts());value.accept(r.bindings());}
            case io.github.gustavo2358.lower.domain.FactDependencies.Input r -> {field.accept("FactDependencies.Input");value.accept(r.id());value.accept(r.kind());value.accept(r.available());value.accept(r.contextScopes());value.accept(r.closureScopes());value.accept(r.declarationScopes());value.accept(r.provenance());}
            case io.github.gustavo2358.lower.domain.FactDependencies.Proof r -> {field.accept("FactDependencies.Proof");value.accept(r.id());value.accept(r.kind());value.accept(r.scope());value.accept(r.subject());value.accept(r.localPremise());value.accept(r.dependencies());value.accept(r.inputs());value.accept(r.rule());value.accept(r.provenance());}
            case io.github.gustavo2358.lower.domain.FactDependencies.Region r -> {field.accept("FactDependencies.Region");value.accept(r.id());value.accept(r.members());value.accept(r.provenance());}
            case io.github.gustavo2358.lower.domain.FactDependencies.Fact r -> {field.accept("FactDependencies.Fact");value.accept(r.id());value.accept(r.kind());value.accept(r.subject());value.accept(r.region());value.accept(r.dependencies());}
            case io.github.gustavo2358.lower.domain.FactDependencies.Binding r -> {field.accept("FactDependencies.Binding");value.accept(r.node());value.accept(r.region());value.accept(r.exactCell());value.accept(r.cells());value.accept(r.regions());value.accept(r.dependencies());}

            case io.github.gustavo2358.lower.domain.ControlTopology r -> {field.accept("ControlTopology");value.accept(r.authority());value.accept(r.occurrences());value.accept(r.regions());value.accept(r.boundaries());value.accept(r.outcomes());value.accept(r.bindings());value.accept(r.proofs());}
            case io.github.gustavo2358.lower.domain.ControlTopology.Occurrence r -> {field.accept("ControlTopology.Occurrence");value.accept(r.statement());value.accept(r.region());value.accept(r.outcomes());value.accept(r.proofs());}
            case io.github.gustavo2358.lower.domain.ControlTopology.Region r -> {field.accept("ControlTopology.Region");value.accept(r.id());value.accept(r.kind());value.accept(r.parent());value.accept(r.entry());value.accept(r.members());value.accept(r.regions());value.accept(r.boundary());value.accept(r.proofs());}
            case io.github.gustavo2358.lower.domain.ControlTopology.Boundary r -> {field.accept("ControlTopology.Boundary");value.accept(r.id());value.accept(r.region());value.accept(r.ordinaryDefault());value.accept(r.proofs());}
            case io.github.gustavo2358.lower.domain.ControlTopology.Outcome r -> {field.accept("ControlTopology.Outcome");value.accept(r.id());value.accept(r.statement());value.accept(r.kind());value.accept(r.role());value.accept(r.target());value.accept(r.binding());value.accept(r.proofs());}
            case io.github.gustavo2358.lower.domain.ControlTopology.Binding r -> {field.accept("ControlTopology.Binding");value.accept(r.id());value.accept(r.caller());value.accept(r.region());value.accept(r.endpoint());value.accept(r.resume());value.accept(r.entryPhase());value.accept(r.completionPhase());value.accept(r.phases());value.accept(r.proofs());}
            case io.github.gustavo2358.lower.domain.ControlTopology.Target r -> {field.accept("ControlTopology.Target");value.accept(r.kind());value.accept(r.reference());value.accept(r.proofs());}
            case io.github.gustavo2358.lower.domain.ControlTopology.Phase r -> {field.accept("ControlTopology.Phase");value.accept(r.id());value.accept(r.kind());value.accept(r.operation());value.accept(r.edges());value.accept(r.proofs());}
            case io.github.gustavo2358.lower.domain.ControlTopology.PhaseEdge r -> {field.accept("ControlTopology.PhaseEdge");value.accept(r.role());value.accept(r.target());}
            case io.github.gustavo2358.lower.domain.ControlTopology.Proof r -> {field.accept("ControlTopology.Proof");value.accept(r.id());value.accept(r.kind());value.accept(r.rule());value.accept(r.provenance());value.accept(r.dependencies());}
            case io.github.gustavo2358.lower.domain.SourceFacts.Inventory r -> {field.accept("SourceInventory");value.accept(r.availability());value.accept(r.occurrences());value.accept(r.gapCodes());}
            case io.github.gustavo2358.lower.domain.SourceFacts.Occurrence r -> {field.accept("SourceOccurrence");value.accept(r.id());value.accept(r.kind());value.accept(r.name());value.accept(r.qualification());value.accept(r.resolution());value.accept(r.artifact());value.accept(r.authority());value.accept(r.provenance());value.accept(r.operation());value.accept(r.access());}
            case SpInput.CicsFileFact r -> {field.accept("CicsFileFact");value.accept(r.header());value.accept(r.command());value.accept(r.rawText());value.accept(r.targetMode());value.accept(r.target());value.accept(r.options());value.accept(r.conditions());value.accept(r.localContinuation());value.accept(r.ordinaryContinuation());value.accept(r.nameProfile());value.accept(r.gapCodes());}
            case SpInput.CicsFileOption r -> {field.accept("CicsFileOption");value.accept(r.name());value.accept(r.canonicalName());value.accept(r.operand());value.accept(r.start());value.accept(r.end());value.accept(r.role());value.accept(r.reference());value.accept(r.literal());value.accept(r.integer());}
            case SpInput.CicsFact r -> {
                field.accept("CicsFact");value.accept(r.header());value.accept(r.command());value.accept(r.rawText());value.accept(r.target());
                value.accept(r.options());value.accept(r.conditions());value.accept(r.localContinuation());value.accept(r.ordinaryContinuation());value.accept(r.nameProfile());value.accept(r.gapCodes());
            }
            case SpInput.CicsOption r -> {field.accept("CicsOption");value.accept(r.name());value.accept(r.operand());value.accept(r.start());value.accept(r.end());value.accept(r.reference());}

            case FileFacts.Inventory r -> {r.sorts().ifPresent(s->{field.accept("FileSortInventory");value.accept(s);});field.accept("FileInventory");value.accept(r.availability());value.accept(r.declarations());value.accept(r.gapCodes());value.accept(r.operations());if(!r.declaratives().isEmpty()){field.accept("FileDeclaratives");value.accept(r.declaratives());}}
            case FileFacts.Declaration r -> {field.accept("FileDeclaration");value.accept(r.id());value.accept(r.owner());value.accept(r.logicalFile());value.accept(r.kind());value.accept(r.optional());value.accept(r.assignment());value.accept(r.organization());value.accept(r.accessMode());value.accept(r.visibility());value.accept(r.records());value.accept(r.references());value.accept(r.origins());value.accept(r.gapCodes());}
            case FileFacts.Assignment r -> {field.accept("FileAssignment");value.accept(r.availability());value.accept(r.profile());value.accept(r.original());value.accept(r.sourceKind());value.accept(r.externalFileName());value.accept(r.gapCodes());}
            case FileFacts.Reference r -> {field.accept("FileReference");value.accept(r.role());value.accept(r.binding());value.accept(r.duplicates());value.accept(r.provenance());}
            case FileFacts.Operations r -> {field.accept("FileOperations");value.accept(r.availability());value.accept(r.uses());value.accept(r.gapCodes());}
            case FileFacts.Use r -> {if(r.role()!=FileFacts.Role.DIRECT){field.accept("FileRole");value.accept(r.role());}field.accept("FileUse");value.accept(r.statement());value.accept(r.ordinal());value.accept(r.command());value.accept(r.mode());value.accept(r.profile());value.accept(r.bindingStatus());value.accept(r.candidates());value.accept(r.provenance());value.accept(r.gapCodes());if(r.surface().isPresent()){field.accept("FileSurface");value.accept(r.surface().orElseThrow());}if(r.effects().isPresent()){field.accept("FileEffects");value.accept(r.effects().orElseThrow());}if(r.control().isPresent()){field.accept("FileControl");value.accept(r.control().orElseThrow());}}
            case FileFacts.SortInventory r -> {value.accept(r.availability());value.accept(r.plans());}
            case FileFacts.SortPlan r -> {field.accept("FileSortPlan");value.accept(r.statement());value.accept(r.availability());value.accept(r.work());value.accept(r.inputs());value.accept(r.outputs());value.accept(r.procedures());value.accept(r.gapCodes());}
            case FileFacts.ProcedurePlan r -> {field.accept("FileProcedurePlan");value.accept(r.phase());value.accept(r.start());value.accept(r.end());value.accept(r.roots());value.accept(r.entry());value.accept(r.completions());value.accept(r.links());value.accept(r.gapCodes());}
            case FileFacts.ProcedureLink r -> {value.accept(r.from());value.accept(r.to());}
            case FileFacts.Declarative r -> {field.accept("FileDeclarative");value.accept(r.id());value.accept(r.owner());value.accept(r.kind());value.accept(r.global());value.accept(r.mode());value.accept(r.files());value.accept(r.roots());value.accept(r.entry());value.accept(r.completions());value.accept(r.gapCodes());value.accept(r.provenance());}
            case FileFacts.ControlPlan r -> {field.accept("FileControlPlan");value.accept(r.availability());value.accept(r.continuation());value.accept(r.routes());value.accept(r.gapCodes());}
            case FileFacts.ControlRoute r -> {field.accept("FileControlRoute");value.accept(r.event());value.accept(r.effects());value.accept(r.destinations());value.accept(r.criticalExit());}
            case FileFacts.Destination r -> {field.accept("FileDestination");value.accept(r.kind());value.accept(r.handler());value.accept(r.declarative());}
            case FileFacts.EffectPlan r -> {field.accept("FileEffectPlan");value.accept(r.availability());value.accept(r.ioReads());value.accept(r.before());value.accept(r.outcomes());value.accept(r.unknownReadBound());value.accept(r.unknownWriteBound());value.accept(r.gapCodes());}
            case FileFacts.OutcomeEffects r -> {field.accept("FileOutcomeEffects");value.accept(r.outcome());value.accept(r.steps());}
            case FileFacts.MemoryStep r -> {field.accept("FileMemoryStep");value.accept(r.role());value.accept(r.kind());value.accept(r.destination());value.accept(r.source());value.accept(r.gapCodes());value.accept(r.provenance());}
            case FileFacts.MemoryTarget r -> {field.accept("FileMemoryTarget");value.accept(r.data());value.accept(r.regional());value.accept(r.wholeBase());value.accept(r.reference());value.accept(r.provenance());}
            case FileFacts.Surface r -> {field.accept("FileSurface");value.accept(r.operands());value.accept(r.options());value.accept(r.keyRelation());value.accept(r.explicitTerminator());value.accept(r.handlers());}
            case FileFacts.Operand r -> {field.accept("FileOperand");value.accept(r.role());value.accept(r.form());value.accept(r.references());value.accept(r.writtenValue());value.accept(r.provenance());value.accept(r.gapCodes());}
            case FileFacts.Handler r -> {field.accept("FileHandler");value.accept(r.kind());value.accept(r.statements());value.accept(r.provenance());}
            case FileFacts.Candidate r -> {field.accept("FileCandidate");value.accept(r.id());value.accept(r.owner());}
            case SpInput r -> {
                field.accept("SpInput");
                if(r.sourceDependencies().availability()!=SpInput.Availability.UNAVAILABLE){field.accept("sourceDependencies@1");value.accept(r.sourceDependencies());}
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
                if(r.fileInventory().availability()!=SpInput.Availability.UNAVAILABLE){field.accept("files@1");value.accept(r.fileInventory());}
                if(r.storage().isPresent()){field.accept("storage@1");value.accept(r.storage().get());}
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
                if(r.scalarInteger().isPresent()){field.accept("scalarInteger");value.accept(r.scalarInteger());}
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
            case SpInput.PerformParagraph r -> {
                field.accept("PerformParagraph"); value.accept(r.id()); value.accept(r.entry()); value.accept(r.statements()); value.accept(r.completions()); value.accept(r.provenance());
            }
            case SpInput.ScalarInteger r -> {field.accept("ScalarInteger");value.accept(r.digits());}
            case SpInput.VaryingOperand r -> {field.accept("VaryingOperand");value.accept(r.level());value.accept(r.role());value.accept(r.integer());value.accept(r.references());value.accept(r.provenance());}
            case SpInput.PerformVarying r -> {field.accept("PerformVarying");value.accept(r.levels());value.accept(r.controls());}
            case SpInput.PerformCount r -> {field.accept("PerformCount");value.accept(r.profile());value.accept(r.integer());value.accept(r.reference());value.accept(r.provenance());}
            case SpInput.PerformLoop r -> {
                field.accept("PerformLoop"); value.accept(r.testMode()); value.accept(r.conditionShape()); value.accept(r.predicate()); value.accept(r.conditionReads()); value.accept(r.provenance());
            }
            case SpInput.ProcedurePerformFact r -> {
                field.accept("ProcedurePerformFact"); if(r.publicationKind()!=SpInput.PerformPublicationKind.LEGACY_PROFILE){value.accept(r.publicationKind().name());value.accept(r.targetEntry());} value.accept(r.header()); value.accept(r.start()); value.accept(r.end()); value.accept(r.procedures()); value.accept(r.normalContinuation());
                if(r.loop().isPresent()){field.accept("loop"); value.accept(r.loop());}
                if(r.times().isPresent()){field.accept("times");value.accept(r.times());}
                if(r.varying().isPresent()){field.accept("varying");value.accept(r.varying());}
                value.accept(r.gapCodes());
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
                if(r.logicalWholeItem().isPresent()){field.accept("logicalWholeItem@2.19");value.accept(r.logicalWholeItem().get());}
                field.accept("provenance"); value.accept(r.provenance());
                if(r.regionalAccess().isPresent()){field.accept("regionalAccess@1");value.accept(r.regionalAccess().get());}
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
            case SpInput.MoveTransfer r -> {
                field.accept("MoveTransfer");field.accept("source");value.accept(r.source());field.accept("target");value.accept(r.target());field.accept("effect");value.accept(r.effect());
            }
            case SpInput.LogicalTransfer r -> {
                field.accept("LogicalTransfer");field.accept("target");value.accept(r.target());field.accept("value");value.accept(r.value());
            }
            case SpInput.MoveFact r -> {
                field.accept("MoveFact");
                field.accept("header"); value.accept(r.header());
                field.accept("source"); value.accept(r.source());
                field.accept("target"); value.accept(r.target());
                field.accept("copySemantics"); value.accept(r.copySemantics());
                field.accept("normalContinuation"); value.accept(r.normalContinuation());
                field.accept("textAdjustment"); value.accept(r.textAdjustment());
                if(r.regionalMove().isPresent()){field.accept("regionalMove@1");value.accept(r.regionalMove().get());}
                if(!r.additionalTransfers().isEmpty()){field.accept("additionalTransfers@1");value.accept(r.additionalTransfers());}
                if(!r.logicalTransfers().isEmpty()){field.accept("logicalTransfers@2.38");value.accept(r.logicalTransfers());}
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
            case SpInput.GoToTarget r -> {
                field.accept("GoToTarget");
                field.accept("id"); value.accept(r.id());
                field.accept("paragraphOrigin"); value.accept(r.paragraphOrigin());
            }
            case SpInput.GoToDestination r -> {
                field.accept("GoToDestination");
                field.accept("ordinal"); value.accept(r.ordinal());
                field.accept("target"); value.accept(r.target());
                field.accept("procedureOrigin"); value.accept(r.procedureOrigin());
                field.accept("referenceOrigin"); value.accept(r.referenceOrigin());
                field.accept("targetEntry"); value.accept(r.targetEntry());
                field.accept("entryOrigin"); value.accept(r.entryOrigin());
                field.accept("gapCodes"); value.accept(r.gapCodes());
            }
            case SpInput.ConditionalGoToFact r -> {
                field.accept("ConditionalGoToFact");
                field.accept("header"); value.accept(r.header());
                field.accept("selector"); value.accept(r.selector());
                field.accept("selectorInteger"); value.accept(r.selectorInteger());
                field.accept("selectorOrigin"); value.accept(r.selectorOrigin());
                field.accept("destinations"); value.accept(r.destinations());
                field.accept("normalContinuation"); value.accept(r.normalContinuation());
                field.accept("gapCodes"); value.accept(r.gapCodes());
            }
            case SpInput.GoToFact r -> {
                field.accept("GoToFact");
                field.accept("header"); value.accept(r.header());
                field.accept("target"); value.accept(r.target());
                field.accept("referenceOrigin"); value.accept(r.referenceOrigin());
                field.accept("targetEntry"); value.accept(r.targetEntry());
                field.accept("entryOrigin"); value.accept(r.entryOrigin());
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
                field.accept("observedShape"); value.accept(r.observedShape());
                field.accept("gapCode"); value.accept(r.gapCode());
                field.accept("normalContinuation"); value.accept(r.normalContinuation());
                field.accept("knownReferences"); value.accept(r.knownReferences());
                if(r.effects().isPresent()){field.accept("effects");value.accept(r.effects());}
            }
            case SpInput.EffectSummary r -> {
                field.accept("EffectSummary");
                value.accept(r.knownReads());value.accept(r.mayWrites());value.accept(r.mustOverwrite());value.accept(r.exposedRegions());
                value.accept(r.unknownReadBound());value.accept(r.unknownWriteBound());value.accept(r.unknownExposureBound());
                value.accept(r.environment());value.accept(r.values());value.accept(r.proof());
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
            default -> StorageIdentityFacts.write(fact,field,value);
        }
    }
}
