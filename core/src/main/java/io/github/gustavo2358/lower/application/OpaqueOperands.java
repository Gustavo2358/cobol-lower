package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;

/** Preserve proved operands; nominal binding alone is never an exact address/effect proof. */
final class OpaqueOperands {
    record Known(List<Operand> operands,List<OperandId> reads,List<OperandId> writes,List<OriginId> origins,
                 Map<SpInput.OperandId,OperandId> occurrences) {
        Known(List<Operand> operands,List<OperandId> reads,List<OperandId> writes,List<OriginId> origins) {
            this(operands,reads,writes,origins,Map.of());
        }
    }
    private OpaqueOperands() { }
    static Known translate(SpInput.StatementFact fact,OperationId operation,ScalarDataTranslator.Result data,
            LocalIds ids,SourceOrigins origins,List<LoweringResult.OperandLink> links,List<Evidence.Uncertainty> uncertainties) {
        var refs=new ArrayList<SpInput.DataReference>();
        if(fact instanceof SpInput.ConditionalGoToFact g)g.selector().ifPresent(refs::add);
        if(fact instanceof SpInput.OtherStatement o)refs.addAll(o.knownReferences());
        if(fact instanceof SpInput.EvaluateFact e)e.subject().ifPresent(refs::add);
        if(fact instanceof SpInput.IfFact f)refs.addAll(f.conditionReads());
        if(fact instanceof SpInput.ProcedurePerformFact p){p.loop().ifPresent(l->refs.addAll(l.conditionReads()));p.times().flatMap(SpInput.PerformCount::reference).ifPresent(refs::add);p.varying().ifPresent(v->v.controls().forEach(o->refs.addAll(o.references()))); }
        if(fact instanceof SpInput.MoveFact m) {refs.add(m.target());if(m.source() instanceof SpInput.DataReference r)refs.add(r);}
        var operands=new ArrayList<Operand>();var reads=new ArrayList<OperandId>();var writes=new ArrayList<OperandId>();var evidence=new ArrayList<OriginId>();
        var occurrences=new LinkedHashMap<SpInput.OperandId,OperandId>();
        for(var ref:refs) {
            var origin=origins.source("operand",ref.id().handle(),ref.provenance());evidence.add(origin);
            var mapping=ref.wholeItemAccess().map(SpInput.WholeItemAccess::data).map(data.index()::get);
            if(mapping.isEmpty()&&ref.regionalAccess().isPresent())mapping=ref.binding().selected().map(data.index()::get);
            if(mapping.isEmpty()) {
                var gap=new UncertaintyId(operation.publication(),ids.id("uncertainty","opaque-reference",operation.localId(),ref.id().handle()));
                uncertainties.add(new Evidence.Uncertainty(gap,"NOMINAL_REFERENCE_WITHOUT_ADDRESS_PROOF",List.of(Evidence.Dimension.STORAGE),
                    new Scopes.EntityScope(List.of(operation)),"SP operand "+ref.id().handle()+" has "+ref.binding().status()+" nominal candidates "+ref.binding().candidates().stream().map(SpInput.DataId::handle).toList()+"; no executable address is published",origin));
                continue;
            }
            var id=new OperandId(new OperationOwner(operation),ids.id("operand","opaque-reference",operation.localId(),ref.id().handle()));
            var role=ref.role()==SpInput.OperandRole.WRITE?Operand.Role.VALUE_WRITE:Operand.Role.VALUE_READ;
            var place=RegionalPlaces.place(ref,mapping.orElseThrow(),new Operand.Header(id,role,origin),ids);
            operands.add(place);if(role==Operand.Role.VALUE_WRITE)writes.add(id);else reads.add(id);
            occurrences.put(ref.id(),id);
            links.add(new LoweringResult.OperandLink(ref.id(),id,origin));
        }
        return new Known(List.copyOf(operands),List.copyOf(reads),List.copyOf(writes),List.copyOf(evidence),Map.copyOf(occurrences));
    }

    static Envelopes.MemoryEnvelope memory(SpInput.StatementFact fact,Known known,PublicationId publication) {
        Scopes.MemoryBound all=new Scopes.WithinMemory(new Scopes.AllMemory(publication,true));
        if(!(fact instanceof SpInput.OtherStatement o)) {
            // Operand identity survives a structural fallback. A receiver operand
            // alone does not prove that an omitted transformation writes it.
            return new Envelopes.MemoryEnvelope(known.reads(),Scopes.NoMemory.INSTANCE,List.of(),Scopes.NoMemory.INSTANCE,List.of());
        }
        if(o.effects().isEmpty()) {
            return new Envelopes.MemoryEnvelope(List.of(),Scopes.NoMemory.INSTANCE,List.of(),Scopes.NoMemory.INSTANCE,List.of());
        }
        var e=o.effects().orElseThrow();
        java.util.function.Function<List<SpInput.OperandId>,List<OperandId>> mapped=xs->xs.stream().map(known.occurrences()::get).filter(Objects::nonNull).toList();
        var reads=mapped.apply(e.knownReads());var writes=mapped.apply(e.mayWrites());
        var otherReads=e.unknownReadBound()==SpInput.EffectBound.NONE&&reads.size()==e.knownReads().size()?Scopes.NoMemory.INSTANCE:all;
        // Exposure records a possible escape; it is not evidence of a write.
        var otherWrites=e.unknownWriteBound()==SpInput.EffectBound.NONE&&writes.size()==e.mayWrites().size()?Scopes.NoMemory.INSTANCE:all;
        return new Envelopes.MemoryEnvelope(reads,otherReads,writes,otherWrites,mapped.apply(e.mustOverwrite()));
    }
}
