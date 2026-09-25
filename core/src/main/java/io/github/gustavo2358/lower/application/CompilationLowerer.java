package io.github.gustavo2358.lower.application;
import java.util.*;
import io.github.gustavo2358.lower.domain.*;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
/** Atomic, transport-independent composition of independently lowered source units. */
public final class CompilationLowerer {
    public LoweringResult lower(SpCompilation input,LowerInput.Options options){
        var invalid=CompilationAdmission.validate(input);
        if(invalid.isPresent())return reject(Admission.Status.INVALID_INPUT,invalid.orElseThrow());
        var ordered=input.units().stream().sorted(Comparator.comparing(u->u.product().unit(),CompilationAdmission.ORDER)).toList();
        // These input facts have no executable identity/publication contract yet.
        for(var unit:ordered)if(unit.product().statements().stream().anyMatch(s->
                s instanceof SpInput.CicsHandlerFact||s instanceof SpInput.CicsAbendFact||s instanceof SpInput.CicsCommandFact))
            return failure(new PartialProgramAdmission().plan(unit.product(),options.admission()).admission());

        var canonical=new SpCompilation(input.inventoryStatus(),input.unitInventory().stream().sorted(CompilationAdmission.ORDER).toList(),ordered);
        var revision=CanonicalRevision.compilation(canonical,options.maximumIdentityCharacters());
        if(revision.isEmpty())return reject(Admission.Status.IMPLEMENTATION_LIMIT,"publication identity budget below 32 characters");
        var publication=new PublicationId(revision.orElseThrow());var ids=new LocalIds();var context=new CompilationContext();
        var plans=new LinkedHashMap<SpInput.UnitKey,PartialProgramAdmission.Plan>();
        for(var product:ordered){
            var plan=new PartialProgramAdmission().plan(product.product(),options.admission());
            if(plan.admission().status()!=Admission.Status.ADMITTED)return failure(plan.admission());
            var key=product.product().unit();plans.put(key,plan);context.products.put(key,product);
            context.units.put(key,new UnitId(publication,ids.id("unit","compilation","compilation",key.toString())));
        }
        for(var product:ordered){
            var used=RegionalDataTranslator.sourceText(plans.get(product.product().unit()).storage());
            for(var capture:product.dataCaptures())if(used.contains(capture.localData()))
                context.capturedLogicalText.computeIfAbsent(capture.sourceData().unit(),ignored->new LinkedHashSet<>()).add(capture.sourceData());
        }
        for(var product:ordered){var key=product.product().unit();var unit=context.units.get(key);
            context.fragments.put(key,PartialProgramLowerer.fragment(product.product(),plans.get(key),publication,unit,ids.activation(unit.localId()),context));}
        var units=new ArrayList<Unit>();var storage=new ArrayList<Memory.Storage>();var resources=new ArrayList<Interactions.Resource>();
        var artifacts=new ArrayList<Origins.Artifact>();var origins=new ArrayList<Origins.Origin>();var uncertainties=new ArrayList<Evidence.Uncertainty>();var premises=new ArrayList<Proofs.Premise>();
        var items=new ArrayList<Evidence.CoverageItem>();var gaps=new ArrayList<UncertaintyId>();var capabilities=new LinkedHashSet<Capabilities.Capability>();
        var entries=new ArrayList<LoweringResult.EntryLink>();var statements=new ArrayList<LoweringResult.StatementLink>();var data=new ArrayList<LoweringResult.DataLink>();var operands=new ArrayList<LoweringResult.OperandLink>();var limitations=new ArrayList<LoweringResult.Limitation>();
        var associations=new HashMap<ResourceId,List<Interactions.ResourceUse>>();boolean logical=false;
        for(var entry:context.fragments.entrySet()){
            var f=entry.getValue();var p=f.publication();var u=p.units().getFirst();
            var localItems=p.coverage().items().stream().map(i->new Evidence.CoverageItem(u.id().localId()+"/"+i.sourceKey(),i.origin(),i.status(),i.outputs(),i.uncertainties(),i.elimination())).toList();
            units.add(new Unit(u.id(),u.containingUnit(),u.objects(),u.visibleObjects(),u.entries(),u.sequences(),u.completionPorts(),u.body(),u.bodyUnavailable(),new Evidence.Coverage(u.coverage().inventory(),u.coverage().scope(),localItems,u.coverage().uncertainties()),u.origin()));
            storage.addAll(p.storage());resources.addAll(p.resources());artifacts.addAll(p.artifacts());origins.addAll(p.origins());uncertainties.addAll(p.uncertainties());premises.addAll(p.premises());items.addAll(localItems);gaps.addAll(p.coverage().uncertainties());capabilities.addAll(p.capabilities().required());
            entries.addAll(f.entries());statements.addAll(f.statements());data.addAll(f.data());operands.addAll(f.operands());limitations.addAll(f.limitations());logical|=f.logicalCopy();
            for(var use:f.files().associations().entrySet())if(!use.getKey().owner().equals(entry.getKey())){
                var resource=context.fragments.get(use.getKey().owner()).files().resourceId(use.getKey().id());associations.computeIfAbsent(resource,k->new ArrayList<>()).addAll(use.getValue());
            }
        }
        for(int i=0;i<resources.size();i++){
            var r=resources.get(i);var extra=associations.get(r.id());if(extra==null)continue;var d=r.declaration().orElseThrow();var uses=new ArrayList<>(d.uses());uses.addAll(extra);
            resources.set(i,new Interactions.Resource(r.id(),r.description(),r.origin(),Optional.of(new Interactions.ResourceDeclaration(d.owner(),d.name(),d.classification(),d.nameSource(),d.objects(),uses))));
        }
        if(input.inventoryStatus()!=SpInput.InventoryStatus.COMPLETE){
            var reason=new UncertaintyId(publication,ids.id("uncertainty","compilation-inventory","compilation","source"));gaps.add(reason);
            uncertainties.add(new Evidence.Uncertainty(reason,"COMPILATION_UNIT_INVENTORY_"+input.inventoryStatus().name(),List.of(Evidence.Dimension.values()),new Scopes.PublicationScope(publication),"Observed units are retained; source input does not prove a complete compilation inventory.",units.getFirst().origin()));
        }
        var output=new Publication(publication,SemanticVersion.AIR_2_0_0,new Capabilities.Manifest(List.copyOf(capabilities),List.of()),artifacts,units,storage,resources,List.of(),origins,new Evidence.Coverage(Evidence.InventoryStatus.PARTIAL,new Scopes.PublicationScope(publication),items,gaps),uncertainties,premises);
        var assessment=logical?OutputAssessment.assessForPartialAnalysis(output,options.validation()):OutputAssessment.assess(output,options.validation());
        var admission=new Admission(Admission.Status.ADMITTED,Optional.empty(),List.of(),new Admission.Statistics(ordered.size(),0,0),false);
        return new LoweringResult(assessment.status(),admission,assessment.publication(),Optional.of(assessment.validation()),entries,statements,limitations,data,operands);
    }
    private static LoweringResult reject(Admission.Status status,String reason){return failure(new Admission(status,Optional.empty(),List.of(new Admission.Diagnostic(Admission.Rule.STRUCTURE,Admission.Phase.INPUT_VALIDATION,Admission.Severity.ERROR,Optional.empty(),"compilation",Optional.empty(),reason)),new Admission.Statistics(0,0,0),false));}
    private static LoweringResult failure(Admission a){return new LoweringResult(LoweringResult.Status.valueOf(a.status().name()),a,Optional.empty(),Optional.empty(),List.of(),List.of(),List.of());}
}
