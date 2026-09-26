package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.lower.domain.StorageFacts;
import io.github.gustavo2358.air.model.Memory;
import io.github.gustavo2358.air.model.MemoryCodecs;
import io.github.gustavo2358.air.model.Types;
import io.github.gustavo2358.air.model.Values;
import java.util.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;
import static io.github.gustavo2358.lower.domain.StorageFacts.*;

/** Validate source facts without calculating COBOL layout; prepare one immutable index per admission. */
final class RegionalStorageAdmission {
    static final Memory.Codec IBM1047=new Memory.ExtensionCodec("text.ebcdic.ibm1047","1",Types.known(Types.Builtin.TEXT));
    record Index(SpInput owner,Map<NodeId,Node> nodes,Map<BaseId,Base> bases,Map<NodeId,View> views,Map<DataId,View> byData,LogicalTextIndex logical) {
        Index { nodes=Map.copyOf(nodes);bases=Map.copyOf(bases);views=Map.copyOf(views);byData=Map.copyOf(byData); }
        Optional<View> access(DataReference reference) {
            return reference.regionalAccess().map(a->{var v=views.get(a.view());return a.slice().map(s->new View(v.node(),v.base(),
                new Measure(Optional.of(s.offset()),List.of()),new Measure(Optional.of(s.extent()),List.of()),v.codec(),v.provenance())).orElse(v);});
        }
    }
    private RegionalStorageAdmission() { }
    static Index validate(SpInput input,EntryGobackAdmission.Context c) {
        try { return index(input,c); }
        catch(Invalid invalid) {
            c.require(false,Admission.Rule.PROFILE_FACT,"regional-storage",null,invalid.getMessage());
            return null;
        }
    }
    private static Index index(SpInput input,EntryGobackAdmission.Context c) {
        var nodes=new LinkedHashMap<NodeId,Node>();var bases=new LinkedHashMap<BaseId,Base>();
        var views=new LinkedHashMap<NodeId,View>();var byData=new LinkedHashMap<DataId,View>();
        if(input.storage().isEmpty()) {
            for(var statement:input.statements()) {
                c.touch();for(var ref:references(statement))require(ref.regionalAccess().isEmpty()&&ref.regionalAlternatives().isEmpty(),"regional access requires a storage inventory");
                if(statement instanceof MoveFact m)require(m.regionalMove().isEmpty(),"regional MOVE requires an explicit environment");
            }
            return new Index(input,nodes,bases,views,byData,new LogicalTextIndex(input,nodes));
        }
        require(input.compositional(),"regional facts require the compositional input profile");
        var storage=input.storage().get();boolean environment=storage.profile()==Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047;
        require(environment?storage.profileId().equals(Optional.of(PROFILE_ID))&&storage.runtimeCodec().equals(Optional.of(CODEC))
            :storage.profileId().isEmpty()&&storage.runtimeCodec().isEmpty()&&!storage.gapCodes().isEmpty(),"profile identity, runtime codec and absence reasons must agree");
        gaps(storage.gapCodes());var dataNodes=new HashSet<DataId>();var siblings=new HashMap<Optional<NodeId>,Set<Integer>>();
        for(var node:storage.nodes()) {
            c.touch();c.identity(node.id().unit(),node.id().handle(),"storage-node",node.provenance());c.provenance(node.provenance());
            require(nodes.putIfAbsent(node.id(),node)==null,"duplicate physical node");measure(node.extent());
            require(node.order()>=0&&siblings.computeIfAbsent(node.parent(),ignored->new HashSet<>()).add(node.order()),"unique nonnegative physical sibling order required");
            require(!node.filler()||node.data().isEmpty(),"FILLER cannot depend on nominal DATA");
            require(environment||node.extent().value().isEmpty(),"known physical extent requires an explicit environment");
            node.data().ifPresent(id->require(id.unit().equals(input.unit())&&c.data(id)!=null&&dataNodes.add(id),"physical DATA identity must be published and unique"));
        }
        var closed=new HashSet<NodeId>();
        for(var node:storage.nodes()) {
            var path=new HashSet<NodeId>();var current=Optional.of(node.id());
            while(current.isPresent()&&!closed.contains(current.get())) {
                c.touch();var id=current.get();var parent=nodes.get(id);
                require(parent!=null&&path.add(id),"physical parent must exist; cycles are invalid");current=parent.parent();
            }
            closed.addAll(path);
        }
        for(var base:storage.bases()) {
            c.touch();c.identity(base.id().unit(),base.id().handle(),"storage-base",base.provenance());c.provenance(base.provenance());measure(base.extent());
            require(bases.putIfAbsent(base.id(),base)==null,"duplicate physical base");
            require(environment||base.extent().value().isEmpty()&&base.allocation()==Allocation.UNPROVEN,"base precision requires an explicit environment");
        }
        var referencedBases=new HashSet<BaseId>();
        for(var view:storage.views()) {
            c.touch();measure(view.offset());measure(view.extent());c.provenance(view.provenance());
            var node=nodes.get(view.node());var base=bases.get(view.base());
            require(node!=null&&base!=null&&views.putIfAbsent(view.node(),view)==null,"view must uniquely reference an existing node and base");
            referencedBases.add(view.base());require(node.extent().equals(view.extent()),"node and view extents disagree");
            require(view.codec().isEmpty()||environment&&view.codec().get().equals(CODEC)&&view.extent().value().isPresent()
                &&node.kind()!=Kind.OPAQUE,"textual view requires known layout and declared codec");
            if(view.offset().value().isPresent()&&view.extent().value().isPresent()&&base.extent().value().isPresent())
                require(end(view).compareTo(base.extent().value().get())<=0,"view exceeds published base extent");
            node.data().ifPresent(id->byData.put(id,view));
        }
        require(views.size()==nodes.size()&&referencedBases.equals(bases.keySet()),"all physical nodes and bases require explicit view closure");
        var logical=new LogicalTextIndex(input,nodes);
        var relationIds=new HashSet<RelationId>();
        for(var relation:storage.relations()) {
            c.touch();c.identity(relation.id().unit(),relation.id().handle(),"storage-relation",relation.provenance());c.provenance(relation.provenance());gaps(relation.gapCodes());
            require(relationIds.add(relation.id()),"duplicate storage relation");
            var owner=nodes.get(relation.owner());require(owner!=null,"storage relation owner must exist");
            require(relation.status()==RelationStatus.PROVEN?relation.target().isPresent()&&relation.gapCodes().isEmpty():relation.target().isEmpty()&&!relation.gapCodes().isEmpty(),
                "proved relation requires target; unproved relation requires explicit uncertainty");
            if(relation.status()==RelationStatus.PROVEN) {
                var target=nodes.get(relation.target().orElseThrow());
                require(target!=null&&target.parent().equals(owner.parent())&&target.order()<owner.order(),"proved storage relation must select an earlier physical sibling");
                var ov=views.get(owner.id());var tv=views.get(target.id());
                require(ov.base().equals(tv.base())&&ov.offset().equals(tv.offset()),"proved storage relation must share base and start");
            }
        }
        var uncertainBases=new HashSet<BaseId>();
        boolean unboundedRelation=false;
        for(var relation:storage.relations())if(relation.status()==RelationStatus.UNPROVEN) {
            var owner=nodes.get(relation.owner());
            if(owner.parent().isEmpty())unboundedRelation=true;
            else uncertainBases.add(views.get(owner.id()).base());
        }
        if(unboundedRelation) {
            require(storage.bases().stream().noneMatch(b->b.allocation().proved()),
                "unproved root relation contradicts allocation independence");
            require(input.dataDeclarations().stream().allMatch(d->d.scalarText().isEmpty()&&d.scalarInteger().isEmpty()),
                "unproved root relation contradicts standalone scalar proof");
        }
        // The physical parent chain bounds a subordinate overlay to its record.
        // Do not infer endpoints or precise views inside that uncertain component.
        for(var base:uncertainBases)require(bases.get(base).extent().value().isEmpty(),
            "unproved subordinate relation requires unknown component extent");
        var uncertainData=new HashSet<DataId>();
        for(var view:views.values())if(uncertainBases.contains(view.base())) {
            require(view.extent().value().isEmpty()&&view.codec().isEmpty(),
                "unproved subordinate relation cannot certify component views");
            nodes.get(view.node()).data().ifPresent(uncertainData::add);
        }
        require(input.dataDeclarations().stream().filter(d->uncertainData.contains(d.id()))
            .allMatch(d->d.scalarText().isEmpty()&&d.scalarInteger().isEmpty()),
            "uncertain component contradicts scalar proof");
        for(var node:storage.nodes())node.parent().ifPresent(id->{
            var parent=nodes.get(id);var pv=views.get(id);var view=views.get(node.id());
            require(parent.kind()!=Kind.ELEMENTARY&&pv.base().equals(view.base()),"child must belong to a group on the same base");
            if(pv.offset().value().isPresent()&&pv.extent().value().isPresent()&&view.offset().value().isPresent()&&view.extent().value().isPresent())
                require(view.offset().value().get().compareTo(pv.offset().value().get())>=0&&end(view).compareTo(end(pv))<=0,"child view exceeds published parent extent");
        });
        var componentSizes=new HashMap<BaseId,Integer>();
        for(var view:views.values())componentSizes.merge(view.base(),1,Integer::sum);
        for(var declaration:input.dataDeclarations()) {
            c.touch();var view=byData.get(declaration.id());
            if(view==null||!(CallAdmission.scalar(declaration)||PerformCountAdmission.integer(declaration)))continue;
            var node=nodes.get(view.node());
            require((logical.byData.containsKey(declaration.id())&&node.kind()==Kind.ELEMENTARY&&declaration.scalarText().isPresent()
                    &&logical.byData.get(declaration.id()).length().equals(java.math.BigInteger.valueOf(declaration.scalarText().orElseThrow().logicalExtent())))
                    ||node.parent().isEmpty()&&node.kind()!=Kind.GROUP&&componentSizes.get(view.base())==1,
                "standalone scalar proof contradicts shared or nested physical storage");
            if(declaration.scalarText().isPresent()&&view.codec().isPresent())require(view.extent().value().orElseThrow()
                .equals(java.math.BigInteger.valueOf(declaration.scalarText().get().logicalExtent())),"scalar text extent contradicts physical view");
            require(declaration.scalarInteger().isEmpty()||view.codec().isEmpty(),"scalar integer proof contradicts textual physical interpretation");
        }
        input.storageIndependence().filter(p->p.availability()==Availability.KNOWN).ifPresent(proof->{
            var components=new HashSet<BaseId>();
            for(var member:proof.members()) { c.touch();var view=byData.get(member);
                if(view!=null)require(components.add(view.base()),"legacy independence proof contradicts shared physical storage");
            }
        });
        for(var r:storage.renames()) {
            c.touch();c.identity(r.id().unit(),r.id().handle(),"storage-relation",r.provenance());c.provenance(r.provenance());gaps(r.gapCodes());
        }
        RegionalRenamesAdmission.validate(input.unit(),storage,nodes,views,relationIds);
        var initialNodes=new HashSet<NodeId>();
        for(var condition:storage.entryState().conditions()) {
            c.touch();c.provenance(condition.provenance());gaps(condition.gapCodes());
            require(nodes.containsKey(condition.node())&&initialNodes.add(condition.node()),"initial condition needs unique existing node");
            require((condition.kind()==InitialKind.POSSIBLE_LOGICAL_TEXT)==condition.logicalText().isPresent(),"logical text requires its own initial kind");
            require(condition.kind()!=InitialKind.POSSIBLE_LOGICAL_TEXT||storage.entryState().possibilityDomain()==PossibilityDomain.LOGICAL_SOURCE,"logical text requires source evidence contract");
            require(condition.bytes().stream().allMatch(b->b>=0&&b<=255),"invalid initial octet");
            require(condition.kind()==InitialKind.LITERAL_BYTES||condition.kind()==InitialKind.POSSIBLE_LITERAL_BYTES||condition.bytes().isEmpty(),"only literal initial state carries bytes");
            require((condition.kind()==InitialKind.UNKNOWN||condition.kind()==InitialKind.POSSIBLE_LITERAL_BYTES||condition.kind()==InitialKind.POSSIBLE_LOGICAL_TEXT)==!condition.gapCodes().isEmpty(),"unknown initial state requires gaps");
            require(condition.kind()==InitialKind.UNKNOWN?condition.proof()==InitialProof.NONE:condition.kind()==InitialKind.PRESERVE?condition.proof()==InitialProof.EXPLICIT_PRESERVED
                :(condition.kind()==InitialKind.POSSIBLE_LITERAL_BYTES||condition.kind()==InitialKind.POSSIBLE_LOGICAL_TEXT)?condition.proof()==InitialProof.DECLARATIVE_POSSIBILITY
                :Set.of(InitialProof.EXPLICIT_INITIAL,InitialProof.PROGRAM_INITIAL,InitialProof.DECLARATIVE_INVARIANT).contains(condition.proof()),"initial kind contradicts proof");
            require(condition.kind()!=InitialKind.POSSIBLE_LITERAL_BYTES||!condition.bytes().isEmpty()&&condition.gapCodes().contains("ENTRY_STATE_NOT_PROVEN"),"possible entry requires bytes and lifecycle remainder");
            if(condition.kind()==InitialKind.POSSIBLE_LOGICAL_TEXT) {
                require(condition.provenance().exact()&&condition.gapCodes().contains("ENTRY_STATE_NOT_PROVEN"),"logical source text needs provenance and remainder");
            } else if(condition.kind()==InitialKind.POSSIBLE_LITERAL_BYTES&&storage.entryState().possibilityDomain()==PossibilityDomain.LOGICAL_SOURCE) {
                require(environment&&condition.provenance().exact(),"logical source evidence requires provenance and selected encoding profile");
            } else if(condition.kind()!=InitialKind.UNKNOWN) {
                var view=views.get(condition.node());
                require(storage.entryState().possibilityDomain()!=PossibilityDomain.LOGICAL_SOURCE||condition.kind()!=InitialKind.LITERAL_BYTES||bases.get(view.base()).allocation().proved(),"strong source initial bytes require proved allocation");
                require(condition.provenance().exact()&&view.codec().isPresent()&&view.offset().value().isPresent()&&view.extent().value().isPresent()
                    &&bases.get(view.base()).extent().value().isPresent(),"initial condition needs exact bounded supported view");
                boolean mode=condition.proof()==InitialProof.EXPLICIT_INITIAL?storage.entryState().mode()==EntryMode.INITIAL
                    :condition.proof()==InitialProof.EXPLICIT_PRESERVED?storage.entryState().mode()==EntryMode.PRESERVED:storage.entryState().mode()==EntryMode.UNKNOWN;
                require(mode&&((condition.kind()!=InitialKind.LITERAL_BYTES&&condition.kind()!=InitialKind.POSSIBLE_LITERAL_BYTES)||view.extent().value().get().equals(java.math.BigInteger.valueOf(condition.bytes().size()))),
                    "initial condition contradicts mode or extent");
                require((condition.proof()!=InitialProof.DECLARATIVE_INVARIANT&&condition.proof()!=InitialProof.DECLARATIVE_POSSIBILITY)||bases.get(view.base()).allocation().proved(),
                    "declarative invariant needs independent local storage");
            }
        }
        var index=new Index(input,nodes,bases,views,byData,logical);
        for(var statement:input.statements()) {
            c.touch();
            for(var ref:references(statement)) {
                if(!ref.regionalAlternatives().isEmpty()) {
                    require(ref.role()==OperandRole.CALL_TARGET&&ref.binding().status()==ResolutionStatus.AMBIGUOUS
                        &&ref.binding().selected().isEmpty()&&ref.regionalAccess().isEmpty()&&ref.wholeItemAccess().isEmpty(),"alternatives require an ambiguous CALL reference");
                    var selectedViews=new HashSet<NodeId>();
                    for(var access:ref.regionalAlternatives()) {
                        var node=nodes.get(access.view());var view=views.get(access.view());
                        require(node!=null&&view!=null&&node.data().isPresent()&&ref.binding().candidates().contains(node.data().get())
                            &&selectedViews.add(node.id()),"alternative must identify a distinct supported binding candidate");
                        require(access.slice().isEmpty()&&node.kind()==Kind.ELEMENTARY&&view.codec().isPresent()
                            &&view.offset().value().isPresent()&&view.extent().value().isPresent()&&view.extent().value().get().signum()>0
                            &&bases.get(view.base()).extent().value().isPresent(),"alternative requires exact canonical whole text storage");
                    }
                }
                c.touch();ref.regionalAccess().ifPresent(access->{
                    var node=nodes.get(access.view());var view=views.get(access.view());
                    require(node!=null&&view!=null&&ref.binding().status()==ResolutionStatus.RESOLVED&&ref.binding().candidates().size()==1
                        &&ref.binding().selected().isPresent()&&ref.binding().selected().equals(node.data())
                        &&ref.binding().candidates().getFirst().equals(ref.binding().selected().get()),"regional access must agree with unique nominal selection");
                    require(view.codec().isPresent()&&view.offset().value().isPresent()&&view.extent().value().isPresent()
                        &&view.extent().value().get().signum()>0&&bases.get(view.base()).extent().value().isPresent(),"exact access requires a bounded supported view");
                    require(ref.role()!=OperandRole.CALL_TARGET||access.slice().isPresent()||node.kind()==Kind.ELEMENTARY,"regional CALL target requires elementary text");
                    access.slice().ifPresent(slice->require(slice.offset().signum()>=0&&slice.extent().signum()>0
                        &&slice.offset().compareTo(view.offset().value().get())>=0
                        &&slice.offset().add(slice.extent()).compareTo(end(view))<=0,"access slice must remain inside declared view"));
                });
            }
            if(statement instanceof MoveFact move)validateMove(move,index);
            if(statement instanceof OtherStatement o&&o.effects().isPresent()) {
                var refs=new HashMap<OperandId,DataReference>();o.knownReferences().forEach(r->refs.put(r.id(),r));
                for(var id:o.effects().orElseThrow().mustOverwrite()) {
                    var ref=refs.get(id);require(ref!=null&&ref.regionalAccess().isPresent(),"effect MUST needs exact regional destination");
                    var access=ref.regionalAccess().orElseThrow();var node=nodes.get(access.view());
                    require(node!=null&&node.kind()==Kind.ELEMENTARY&&!node.filler()&&access.slice().isEmpty(),"INITIALIZE MUST is an exact whole elementary receiver");
                }
            }
        }
        return index;
    }
    private static void validateMove(MoveFact move,Index index) {
        if(move.regionalMove().isEmpty()) {
            require(references(move).stream().noneMatch(r->r.regionalAccess().isPresent()),"regional MOVE accesses require an explicit effect");return;
        }
        require(move.additionalTransfers().isEmpty()||move.copySemantics()==CopySemantics.UNAVAILABLE,"regional sequence cannot claim scalar copy");
        for(var transfer:move.transfers())validateTransfer(transfer,index);
        RegionalTransferAdmission.validate(move,index);
    }
    private static void validateTransfer(MoveTransfer transfer,Index index) {
        var effect=transfer.effect();gaps(effect.gapCodes());
        require(effect.bytes().stream().allMatch(b->b>=0&&b<=255),"literal bytes must be octets");
        require(effect.kind()==MoveKind.LITERAL_BYTES||effect.kind()==MoveKind.FITTED_LITERAL_BYTES||effect.bytes().isEmpty(),"only literal byte writes carry a payload");
        require((effect.kind()==MoveKind.MUST_UNKNOWN||effect.kind()==MoveKind.UNAVAILABLE)==!effect.gapCodes().isEmpty(),"unknown MOVE needs gaps; exact MOVE cannot carry gaps");
        if(effect.kind()==MoveKind.UNAVAILABLE)return;
        require(transfer.target().role()==OperandRole.WRITE&&transfer.target().regionalAccess().isPresent(),"mandatory regional write requires exact WRITE access");
        var dest=index.access(transfer.target()).orElseThrow();
        if(effect.kind()==MoveKind.LITERAL_BYTES||effect.kind()==MoveKind.FITTED_LITERAL_BYTES) {
            require(transfer.source() instanceof LiteralSource l&&l.kind()==LiteralKind.ALPHANUMERIC&&l.logicalValue().isPresent(),"byte write needs a proved logical literal");
            var literal=((LiteralSource)transfer.source()).logicalValue().orElseThrow();
            require(literal.logicalDomain()==LogicalDomain.TEXT&&literal.logicalExtent()==literal.value().codePointCount(0,literal.value().length()),"literal byte write requires coherent TEXT");
            require(dest.extent().value().orElseThrow().equals(java.math.BigInteger.valueOf(effect.bytes().size())),"literal vector must fill exact destination");
            String text=literal.value();
            if(effect.kind()==MoveKind.FITTED_LITERAL_BYTES) {
                int n=effect.bytes().size(),count=text.codePointCount(0,text.length());
                text=count>n?text.substring(0,text.offsetByCodePoints(0,n)):text+" ".repeat(n-count);
            }
            var encoded=MemoryCodecs.encodeText(IBM1047,new Values.TextValue(text),dest.extent().value().orElseThrow());
            require(encoded.status()==MemoryCodecs.Status.EXACT&&encoded.value().orElseThrow().octets().equals(effect.bytes()),"published bytes disagree with logical literal, declared codec or extent");
        }
        if(effect.kind()==MoveKind.LOGICAL_FIT_TEXT) {
            require(transfer.source() instanceof DataReference r&&r.role()==OperandRole.READ&&r.logicalWholeItem().isPresent()&&r.binding().selected().equals(r.logicalWholeItem())&&r.provenance().exact(),"logical copy needs canonical whole READ");
            var ref=(DataReference)transfer.source();var source=index.byData().get(ref.logicalWholeItem().orElseThrow());
            require(source!=null&&index.nodes().get(source.node()).kind()==Kind.ELEMENTARY&&source.codec().isPresent()&&source.codec().equals(dest.codec())&&source.extent().value().filter(n->n.signum()>0).isPresent(),"logical copy requires supported text shape");
            require(!source.base().equals(dest.base())&&index.bases().get(source.base()).allocation().proved()&&index.bases().get(dest.base()).allocation().proved(),"logical copy needs proved independent bases");
        }
        if(effect.kind()==MoveKind.COPY_BYTES||effect.kind()==MoveKind.FIT_TEXT) {
            require(transfer.source() instanceof DataReference r&&r.role()==OperandRole.READ&&r.regionalAccess().isPresent(),"byte copy needs exact READ access");
            var source=index.access((DataReference)transfer.source()).orElseThrow();
            require(effect.kind()!=MoveKind.COPY_BYTES||source.extent().equals(dest.extent()),"copy requires equal extents");
            boolean disjoint=source.base().equals(dest.base())?end(source).compareTo(dest.offset().value().orElseThrow())<=0
                ||end(dest).compareTo(source.offset().value().orElseThrow())<=0
                :index.bases().get(source.base()).allocation().proved()
                    &&index.bases().get(dest.base()).allocation().proved();
            require(disjoint,"COBOL copy requires proved disjoint source and destination ranges");
        }
    }
    static List<DataReference> references(StatementFact fact) {
        var result=new ArrayList<DataReference>();
        switch(fact) {
            case MoveFact m -> { if(m.source() instanceof DataReference r)result.add(r);result.add(m.target());for(var t:m.additionalTransfers()){if(t.source() instanceof DataReference r)result.add(r);result.add(t.target());} }
            case CicsFileFact cics -> {if(cics.target().orElse(null) instanceof DataCallTarget d)result.add(d.reference());cics.options().forEach(o->o.reference().ifPresent(result::add));}
            case CicsFact cics -> {if(cics.target().orElse(null) instanceof DataCallTarget d)result.add(d.reference());cics.options().forEach(o->o.reference().ifPresent(result::add));}
            case CallFact call -> { if(call.target() instanceof DataCallTarget d)result.add(d.reference()); }
            case IfFact f -> result.addAll(f.conditionReads());
            case EvaluateFact e -> e.subject().ifPresent(result::add);
            case ConditionalGoToFact g -> g.selector().ifPresent(result::add);
            case OtherStatement o -> result.addAll(o.knownReferences());
            case ProcedurePerformFact p -> {
                p.loop().ifPresent(l->result.addAll(l.conditionReads()));p.times().flatMap(PerformCount::reference).ifPresent(result::add);
                p.varying().ifPresent(v->v.controls().forEach(o->result.addAll(o.references())));
            }
            default -> { }
        }
        return result;
    }
    private static java.math.BigInteger end(View view) { return view.offset().value().orElseThrow().add(view.extent().value().orElseThrow()); }
    private static void measure(Measure measure) {
        gaps(measure.gapCodes());require(measure.value().isPresent()?measure.value().get().signum()>=0&&measure.gapCodes().isEmpty():!measure.gapCodes().isEmpty(),"measure must be known nonnegative or explicitly unknown");
    }
    private static void gaps(List<String> codes) { require(codes.stream().noneMatch(String::isBlank),"gap codes must be nonblank"); }
    private static void require(boolean condition,String reason) { if(!condition)throw new Invalid(reason); }
    static final class Invalid extends RuntimeException { private static final long serialVersionUID=1L;Invalid(String message){super(message);} }
}
