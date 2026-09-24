package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;

/** Supported CP6 program publication assembly; no transport dependencies or universal statement framework. */
final class PartialProgramLowerer implements LowerInput {
    @Override public LoweringResult lower(SpInput input, Options options) {
        var plan = new PartialProgramAdmission().plan(input, options.admission()); var admission = plan.admission();
        if (admission.status() != Admission.Status.ADMITTED) {
            var status = switch (admission.status()) {
                case INVALID_INPUT -> LoweringResult.Status.INVALID_INPUT;
                case BLOCKED_LOWERING -> LoweringResult.Status.BLOCKED_LOWERING;
                case UNSUPPORTED_SLICE -> LoweringResult.Status.UNSUPPORTED_SLICE;
                case IMPLEMENTATION_LIMIT -> LoweringResult.Status.IMPLEMENTATION_LIMIT;
                case ADMITTED -> throw new IllegalStateException("already handled");
            };
            return new LoweringResult(status, admission, Optional.empty(), Optional.empty(), List.of(), List.of(), List.of());
        }
        var revision = CanonicalRevision.partial(input, options.maximumIdentityCharacters());
        if (revision.isEmpty()) return new LoweringResult(LoweringResult.Status.IMPLEMENTATION_LIMIT, admission, Optional.empty(), Optional.empty(), List.of(), List.of(),
            List.of(new LoweringResult.Limitation(LoweringResult.LimitCode.IDENTITY_LIMIT, "publication", "Compact PublicationId needs 32 characters; no prefix published.")));
        var publication = new PublicationId(revision.orElseThrow()); var unit = new UnitId(publication, "unit");
        var fragment=fragment(input,plan,publication,unit,new LocalIds(),null);
        var assessment=fragment.logicalCopy()?OutputAssessment.assessForPartialAnalysis(fragment.publication(),options.validation()):OutputAssessment.assess(fragment.publication(),options.validation());
        return new LoweringResult(assessment.status(),admission,assessment.publication(),Optional.of(assessment.validation()),fragment.entries(),fragment.statements(),fragment.limitations(),fragment.data(),fragment.operands());
    }
    record Fragment(Publication publication,List<LoweringResult.EntryLink> entries,List<LoweringResult.StatementLink> statements,List<LoweringResult.Limitation> limitations,List<LoweringResult.DataLink> data,List<LoweringResult.OperandLink> operands,boolean logicalCopy,ScalarDataTranslator.Result storage,FileResourceLowering files){}
    static Fragment fragment(SpInput input,PartialProgramAdmission.Plan plan,PublicationId publication,UnitId unit,LocalIds ids,CompilationContext context){
        var origins = new SourceOrigins(publication, ids); var items = new ArrayList<Evidence.CoverageItem>(); var uncertainties = new ArrayList<Evidence.Uncertainty>();
        var statements = new ArrayList<LoweringResult.StatementLink>(); var operands = new ArrayList<LoweringResult.OperandLink>();
        var sourceEntry = input.entryInventory().entries().getFirst();
        var entryOrigin = origins.source("entry", sourceEntry.id().handle(), sourceEntry.provenance());
        var requiredData=new LinkedHashSet<SpInput.DataId>();
        input.fileInventory().declarations().forEach(declaration->requiredData.addAll(declaration.records()));
        if(context!=null)requiredData.addAll(context.requiredData(input));
        var data = RegionalDataTranslator.translate(plan.data(), plan.storage(),
            requiredData,context==null?Set.of():context.logicalText(input),context==null?Set.of():context.captureLocals(input),
            unit, ids, origins, items, uncertainties);
        if(context!=null)data=context.captures(input,data,unit,ids,origins,items,uncertainties);
        var files=new FileResourceLowering(input,data,unit,ids,origins,uncertainties);
        if(context!=null)context.importFiles(input,files);
        var assembly = input.controlTopology().isPresent()
            ? TopologyProgramAssembler.assemble(plan,data,unit,ids,origins,statements,operands,items,uncertainties,files)
            : PartialProgramAssembler.assemble(plan,data,unit,ids,origins,statements,operands,items,uncertainties,files);
        var entryId = new EntryId(unit, ids.id("entry", "primary-entry", unit.localId(), sourceEntry.id().handle()));
        Interactions.UnknownBound signatureRemainder=Interactions.NoRemainder.INSTANCE;
        if(sourceEntry.signature().availability()!=SpInput.Availability.KNOWN) {
            var reason=new UncertaintyId(publication,ids.id("uncertainty","entry-signature",unit.localId(),sourceEntry.id().handle()));
            uncertainties.add(new Evidence.Uncertainty(reason,"ENTRY_SIGNATURE_UNKNOWN",List.of(Evidence.Dimension.EFFECTS,Evidence.Dimension.CONTROL),
                new Scopes.EntityScope(List.of(entryId)),"SP does not publish a precise entry signature.",entryOrigin));
            signatureRemainder=new Interactions.UnknownRemainder(reason);
        }
        var signature = new Interactions.Signature(new Interactions.ParameterInventory(List.of(), signatureRemainder),
            new Interactions.ResultInventory(List.of(), signatureRemainder), entryOrigin);
        var entry = new Entries.Entry(entryId, Optional.of(assembly.entryLabel()), signature, RegionalEntryTranslator.translate(plan.storage(),data,entryId,ids,origins,items,uncertainties), entryOrigin);
        var entryLinks = List.of(new LoweringResult.EntryLink(sourceEntry.id(), entryId, assembly.entryLabel(), entryOrigin));
        items.add(ScalarEvidence.item(publication, "entry", sourceEntry.id().handle(), entryOrigin, List.of(entryId)));
        var unitOrigin = origins.derived(context==null?"unit-origin":ids.id("origin","unit-origin",unit.localId(),"unit"), List.of(entryOrigin, assembly.entrySequenceOrigin()), "supported-cp6-program@1/selected-unit");
        var gaps = new ArrayList<UncertaintyId>();
        for (var code : input.entryInventory().gapCodes()) {
            var gap = new UncertaintyId(publication, ids.id("uncertainty", "scalar-entry-inventory", unit.localId(), Integer.toString(gaps.size()))); gaps.add(gap);
            uncertainties.add(new Evidence.Uncertainty(gap, "cobol-lower:" + code, List.of(Evidence.Dimension.CONTROL), new Scopes.UnitScope(unit),
                "SP PRIMARY_ONLY/PARTIAL preserves this entry inventory gap.", entryOrigin));
        }
        var gapTargets=new HashMap<SpInput.StatementId,List<Id>>();
        for(var link:statements)gapTargets.computeIfAbsent(link.source(),key->new ArrayList<>()).add(link.target());
        var factsByHandle=new HashMap<String,SpInput.StatementFact>();input.statements().forEach(f->factsByHandle.put(f.header().id().handle(),f));
        if(input.controlTopology().isPresent())for(var occurrence:input.controlTopology().orElseThrow().occurrences()) {
            var source=factsByHandle.get(occurrence.statement());
            if(gapTargets.containsKey(source.header().id()))continue;
            var origin=origins.source("topology-inventory",occurrence.statement(),source.header().provenance());
            var reason=new UncertaintyId(publication,ids.id("uncertainty","topology-inventory",unit.localId(),occurrence.statement()));
            uncertainties.add(new Evidence.Uncertainty(reason,"TOPOLOGY_OCCURRENCE_NOT_IN_ENTRY_PROJECTION",List.of(Evidence.Dimension.CONTROL),new Scopes.UnitScope(unit),
                "Inventoried source occurrence in "+occurrence.region()+" has no materialized occurrence in the selected entry projection; source unreachability is not claimed",origin));
            gaps.add(reason);
            items.add(new Evidence.CoverageItem("sp-topology-inventory@1/"+occurrence.statement(),origin,Evidence.CoverageStatus.ABSTRACTED,List.of(),List.of(reason),Optional.empty()));
        }
        int gapIndex = 0;
        for (var gap : input.gaps()) {
            var key = Integer.toString(gapIndex++); var origin = origins.source("gap", key, gap.provenance());
            var id = new UncertaintyId(publication, ids.id("uncertainty", "scalar-sp-gap", unit.localId(), key)); gaps.add(id);
            uncertainties.add(new Evidence.Uncertainty(id, "cobol-sp:" + gap.code(), List.of(Evidence.Dimension.values()), gapTargets.containsKey(gap.statement())?new Scopes.EntityScope(gapTargets.get(gap.statement())):new Scopes.UnitScope(unit),
                gap.scope().name() + ": " + gap.detail(), origin));
        }
        var unitCoverage = new Evidence.Coverage(Evidence.InventoryStatus.PARTIAL, new Scopes.UnitScope(unit), items, gaps);
        var body = new Unit(unit, context==null?Optional.empty():context.parent(input.unit()), data.objects(), context==null?List.of():context.visible(input.unit()), List.of(entry), assembly.sequences(), List.of(),
            Unit.BodyAvailability.AVAILABLE, Optional.empty(), unitCoverage, unitOrigin);
        var premises = new ArrayList<>(StoragePremise.available(input, data, unit, ids, origins));
        premises.addAll(RegionalDataTranslator.premises(plan.storage(),data,unit,ids,origins));
        var required=new ArrayList<>(RegionalDataTranslator.capabilities(data).required());
        if(entry.state().conditions().stream().anyMatch(c->c.value() instanceof Entries.PossibleLiterals))required.add(
            input.storage().orElseThrow().entryState().possibilityDomain()==io.github.gustavo2358.lower.domain.StorageFacts.PossibilityDomain.LOGICAL_SOURCE
                ?Capabilities.ENTRY_POSSIBILITIES_V2:Capabilities.ENTRY_POSSIBILITIES);
        if(input.statements().stream().anyMatch(s->s instanceof SpInput.CallFact call&&call.target() instanceof SpInput.DataCallTarget d&&!d.reference().regionalAlternatives().isEmpty()))required.add(Capabilities.TARGET_POSSIBILITIES);
        if(assembly.sequences().stream().map(Sequence::terminator).filter(Operations.Invoke.class::isInstance).map(Operations.Invoke.class::cast)
                .anyMatch(i->i.target() instanceof Interactions.LiteralTarget t&&t.namespace().equals("cics.program")
                    ||i.target() instanceof Interactions.ComputedTarget c&&c.namespace().equals("cics.program")))required.add(CicsInvokeHandler.NAME);
        if(input.statements().stream().anyMatch(s->s instanceof SpInput.CicsFact||s instanceof SpInput.CicsFileFact))
            if(!required.contains(Capabilities.TARGET_POSSIBILITIES))required.add(Capabilities.TARGET_POSSIBILITIES);
        if(assembly.sequences().stream().map(Sequence::terminator)
                .anyMatch(t->t instanceof Operations.Invoke i
                    &&(i.target() instanceof Interactions.LiteralTarget l&&l.namespace().equals("cics.file")
                        ||i.target() instanceof Interactions.ComputedTarget c&&c.namespace().equals("cics.file"))))
            required.add(CicsFileInvokeHandler.NAME);
        var resources=new ArrayList<>(files.resources());
        resources.addAll(SourceResourceLowering.resources(input,unit,ids,origins,unitOrigin));
        if(files.available()||input.sourceDependencies().availability()!=SpInput.Availability.UNAVAILABLE)required.add(Capabilities.RESOURCE_BINDINGS);
        var output = new Publication(publication, SemanticVersion.AIR_2_0_0, new Capabilities.Manifest(required,List.of()), origins.artifacts(),
            List.of(body), data.storage(), resources, List.of(), origins.origins(),
            new Evidence.Coverage(Evidence.InventoryStatus.PARTIAL, new Scopes.PublicationScope(publication), items, gaps), uncertainties, premises);
        boolean logicalCopy=input.statements().stream().anyMatch(s->s instanceof SpInput.MoveFact m&&m.regionalMove().filter(e->e.kind()==io.github.gustavo2358.lower.domain.StorageFacts.MoveKind.LOGICAL_FIT_TEXT).isPresent());
        return new Fragment(output,entryLinks,List.copyOf(statements),origins.limitations(),List.copyOf(data.index().values()),List.copyOf(operands),logicalCopy,data,files);
    }
}
