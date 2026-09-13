package io.github.gustavo2358.lower.adapters.testing;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import java.util.*;
import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.lower.domain.StorageFacts;
import io.github.gustavo2358.lower.testing.IfInputs;
import java.math.BigInteger;
import static io.github.gustavo2358.lower.testing.CallOracle.check;

/** Physical AIR assertions are constants independent of source layout and decoder implementation. */
public final class RegionalTranslationSuite {
    private RegionalTranslationSuite() { }
    public static void main(String[] args) throws Exception { run(); }
    static LoweringResult lower(String fixture) throws Exception {
        var input=((SpJsonDecoder.Decoded)new SpJsonDecoder(CobolLower.INPUT_LIMITS)
            .decode(RegionalStorageIntegrationSuite.fixture(fixture))).input();
        return lower(input);
    }
    static LoweringResult lower(SpInput input) {
        var result=new CobolLowerer().lower(input,CobolLower.OPTIONS);
        check(result.status()==LoweringResult.Status.SUCCESS,"regional AIR must validate: "+result);
        var p=result.publication().orElseThrow();var codec=new AirJson();var wire=codec.encode(p);
        check(codec.decode(wire).equals(p)&&Arrays.equals(wire,codec.encode(codec.decode(wire))),"regional semantic and byte round-trip");
        return result;
    }
    static List<Instruction> instructions(Publication p) {
        return p.units().getFirst().sequences().stream().flatMap(s->s.instructions().stream()).toList();
    }
    public static void run() throws Exception {
        var group=lower("group-child").publication().orElseThrow();
        check(group.storage().size()==1&&group.storage().getFirst() instanceof Memory.Region,"one physical Region, never one Cell per name");
        var region=(Memory.Region)group.storage().getFirst();
        check(region.extent().equals(Optional.of(BigInteger.valueOf(8)))&&region.header().lifetime()==Memory.Lifetime.PERSISTENT,"explicit persistent eight-byte allocation");
        var objects=group.units().getFirst().objects();check(objects.size()==2,"group and child object views");
        for(var object:objects)check(object.storage() instanceof Memory.ViewBinding v&&v.region().equals(region.header().id())
            &&v.offset().equals(BigInteger.ZERO)&&v.extent().equals(BigInteger.valueOf(8))
            &&v.codec().equals(new Memory.ExtensionCodec("text.ebcdic.ibm1047","1",Types.known(Types.Builtin.TEXT))),"shared range with explicit interpretation");
        var assign=(Operations.Assign)instructions(group).getFirst();
        check(assign.destination() instanceof Places.RegionSlice s&&s.region().equals(region.header().id())&&s.codec()==Memory.IdentityBytes.INSTANCE,"byte-slice destination");
        check(assign.value() instanceof Expressions.Literal l&&l.value().equals(new Values.BytesValue(List.of(215,199,212,240,240,240,240,241))),"literal IBM bytes preserved exactly");
        var invoke=group.units().getFirst().sequences().stream().map(Sequence::terminator).filter(Operations.Invoke.class::isInstance).map(Operations.Invoke.class::cast).findFirst().orElseThrow();
        check(invoke.target() instanceof Interactions.ComputedTarget t&&t.name() instanceof Expressions.Read r&&r.place() instanceof Places.ObjectPlace,"CALL uses a view read before effects");
        check(group.capabilities().required().containsAll(List.of(Capabilities.MEMORY_REGIONS,Capabilities.IBM1047)),"capabilities explicit");
        var nested=lower("nested-filler").publication().orElseThrow();
        check(nested.storage().size()==1&&nested.units().getFirst().objects().size()==4,"FILLER has physical coverage but no nominal object");
        check(((Memory.Region)nested.storage().getFirst()).extent().equals(Optional.of(BigInteger.valueOf(14))),"filler contributes two bytes");
        check(nested.units().getFirst().objects().stream().filter(o->o.storage() instanceof Memory.ViewBinding v&&v.offset().equals(BigInteger.valueOf(6))).count()==2,"nested group and leaf start after intermediate FILLER");
        var copyResult=lower("copy-capture");var copy=copyResult.publication().orElseThrow();
        check(copy.storage().size()==2&&copy.storage().stream().allMatch(Memory.Region.class::isInstance),"standalone textual copies use physical views");
        check(instructions(copy).stream().filter(Operations.CopyBytes.class::isInstance).count()==1,"source capture emitted as CopyBytes");
        var copying=instructions(copy).stream().filter(Operations.CopyBytes.class::isInstance).map(Operations.CopyBytes.class::cast).findFirst().orElseThrow();
        var x=copyResult.data().stream().filter(d->d.source().handle().equals("data:0")).findFirst().orElseThrow();
        var y=copyResult.data().stream().filter(d->d.source().handle().equals("data:1")).findFirst().orElseThrow();
        check(copying.source().region().equals(x.storage())&&copying.destination().region().equals(y.storage())&&copying.length().equals(BigInteger.valueOf(4)),"copy source and destination preserve independent SP correlation");
        check(copy.premises().stream().anyMatch(p->p.assertion() instanceof Proofs.DisjointStorage d&&d.storage().size()==2),"copy retains source allocation disjunction proof");
        var unknown=lower("unknown-prefix").publication().orElseThrow();
        check(unknown.storage().stream().anyMatch(s->s instanceof Memory.Region r&&r.extent().isEmpty()&&r.extentUnknown().isPresent()),"unknown physical extent is retained, never zero");
        check(instructions(unknown).stream().noneMatch(Operations.Assign.class::isInstance),"unproved offset never becomes precise write");
        conservativeAndFitted(); mixedNumeric();
        for(int n:List.of(1,2,5,40)) {
            var input=io.github.gustavo2358.lower.testing.RegionalInputs.group(n);
            var result=new CobolLowerer().lower(input,CobolLower.OPTIONS);
            check(result.status()==LoweringResult.Status.SUCCESS,"independent memory input group "+n+" "+result);
            var p=result.publication().orElseThrow();var wire=new AirJson().encode(p);
            check(new AirJson().decode(wire).equals(p),"memory-input AIR wire equivalence");
            check(p.storage().size()==1&&p.units().getFirst().objects().size()==n+1,"group multiplicity without fake cells");
            check(((Memory.Region)p.storage().getFirst()).extent().orElseThrow().equals(BigInteger.valueOf(n*8L)),"memory oracle extent");
            check(p.units().getFirst().objects().stream().anyMatch(o->o.storage() instanceof Memory.ViewBinding v&&v.offset().equals(BigInteger.valueOf((n-1)*8L))),"memory oracle last-child offset");
            var reversed=new ArrayList<>(input.storage().get().nodes());Collections.reverse(reversed);
            var reversedViews=new ArrayList<>(input.storage().get().views());Collections.reverse(reversedViews);
            var physical=io.github.gustavo2358.lower.testing.IfInputs.with(io.github.gustavo2358.lower.testing.IfInputs.with(input.storage().get(),"nodes",reversed),"views",reversedViews);
            var permutation=io.github.gustavo2358.lower.testing.IfInputs.with(input,"storage",Optional.of(physical));
            check(Arrays.equals(wire,new AirJson().encode(new CobolLowerer().lower(permutation,CobolLower.OPTIONS).publication().orElseThrow())),"physical inventory permutation keeps all AIR bytes");
        }
        System.out.println("LOWER_REGIONAL_TRANSLATION_FIXTURES=4; MEMORY_MULTIPLICITIES=1,2,5,40");
    }
    private static void conservativeAndFitted() throws Exception {
        var decoder=new SpJsonDecoder(CobolLower.INPUT_LIMITS);
        var input=((SpJsonDecoder.Decoded)decoder.decode(RegionalStorageIntegrationSuite.fixture("nested-filler"))).input();
        var move=(SpInput.MoveFact)input.statements().getFirst();
        var call=(SpInput.CallFact)input.statements().get(1);var child=((SpInput.DataCallTarget)call.target()).reference();
        var target=new SpInput.DataReference(move.target().id(),SpInput.OperandRole.WRITE,child.binding(),Optional.empty(),move.target().provenance(),child.regionalAccess());
        var effect=new StorageFacts.Move(StorageFacts.MoveKind.MUST_UNKNOWN,List.of(),List.of("LENGTH_NOT_EXACT"));
        var unknownMove=new SpInput.MoveFact(move.header(),move.source(),target,SpInput.CopySemantics.UNAVAILABLE,move.normalContinuation(),Optional.empty(),Optional.of(effect));
        var statements=new ArrayList<>(input.statements());statements.set(0,unknownMove);
        var unknown=lower(IfInputs.with(input,"statements",statements)).publication().orElseThrow();
        check(instructions(unknown).getFirst() instanceof Operations.HavocMust h&&h.destination() instanceof Places.RegionSlice p
            &&p.offset() instanceof Expressions.Literal o&&o.value().equals(new Values.IntValue(BigInteger.valueOf(6)))
            &&p.length() instanceof Expressions.Literal l&&l.value().equals(new Values.IntValue(BigInteger.valueOf(8))),"unknown source still writes only the proved child interval");
        var copy=((SpJsonDecoder.Decoded)decoder.decode(RegionalStorageIntegrationSuite.fixture("copy-capture"))).input();
        var original=(SpInput.MoveFact)copy.statements().getFirst();
        for(String text:List.of("AB","€")) {
            var source=new SpInput.LiteralSource(original.source().id(),SpInput.LiteralKind.ALPHANUMERIC,
                Optional.of(new SpInput.LogicalValue(SpInput.LogicalDomain.TEXT,text,text.length())),original.source().provenance());
            var fitted=new SpInput.TextAdjustment(SpInput.TextAdjustmentRule.RIGHT_PAD_SPACE,4,
                new SpInput.LogicalValue(SpInput.LogicalDomain.TEXT,text+" ".repeat(4-text.length()),4),original.header().provenance());
            var update=new SpInput.MoveFact(original.header(),source,original.target(),SpInput.CopySemantics.FITTED_TEXT,
                original.normalContinuation(),Optional.of(fitted),Optional.of(effect));
            var changed=new ArrayList<>(copy.statements());changed.set(0,update);
            var result=lower(IfInputs.with(copy,"statements",changed));
            var op=result.statements().stream().filter(l->l.source().equals(original.header().id())).map(LoweringResult.StatementLink::target).toList();
            var write=instructions(result.publication().orElseThrow()).stream().filter(i->op.contains(i.header().id())).findFirst().orElseThrow();
            check(text.equals("AB")?write instanceof Operations.Assign a&&a.value() instanceof Expressions.Literal l&&l.value().equals(new Values.TextValue("AB  "))
                :write instanceof Operations.HavocMust,"proved scalar fitting survives when encodable; unsupported runtime character retains havoc");
            var legacyTarget=IfInputs.with(update.target(),"regionalAccess",Optional.empty());
            var legacy=IfInputs.with(IfInputs.with(update,"regionalMove",Optional.empty()),"target",legacyTarget);
            changed.set(0,legacy);var legacyResult=lower(IfInputs.with(copy,"statements",changed));
            var legacyOps=legacyResult.statements().stream().filter(l->l.source().equals(original.header().id())).map(LoweringResult.StatementLink::target).toList();
            var legacyWrite=instructions(legacyResult.publication().orElseThrow()).stream().filter(i->legacyOps.contains(i.header().id())).findFirst().orElseThrow();
            check(text.equals("AB")?legacyWrite instanceof Operations.Assign:legacyWrite instanceof Operations.HavocMust,"legacy-only access still respects explicit physical codec");
        }
    }

    private static void mixedNumeric() {
        var input=io.github.gustavo2358.lower.testing.RegionalInputs.group(1);var unit=input.unit();
        var data=new ArrayList<>(input.dataDeclarations());var template=data.getFirst();
        var numeric=new SpInput.DataFact(new SpInput.DataId(unit,"data:50"),"numeric",Optional.of("never parse this"),template.provenance(),template.coverage(),template.readiness(),Optional.empty(),Optional.of(new SpInput.ScalarInteger(9)));data.add(numeric);
        var physical=input.storage().get();var node=new StorageFacts.NodeId(unit,"storage-node:200");var base=new StorageFacts.BaseId(unit,"storage-base:777");
        var unknown=new StorageFacts.Measure(Optional.empty(),List.of("NUMERIC_REPRESENTATION_NOT_PROVED"));
        var nodes=new ArrayList<>(physical.nodes());nodes.add(new StorageFacts.Node(node,Optional.empty(),1,false,StorageFacts.Kind.OPAQUE,Optional.of(numeric.id()),unknown,numeric.provenance()));
        var bases=new ArrayList<>(physical.bases());bases.add(new StorageFacts.Base(base,unknown,StorageFacts.Allocation.INDEPENDENT_LOCAL_WORKING_STORAGE,numeric.provenance()));
        var views=new ArrayList<>(physical.views());views.add(new StorageFacts.View(node,base,io.github.gustavo2358.lower.testing.RegionalInputs.known(0),unknown,Optional.empty(),numeric.provenance()));
        var inventory=IfInputs.with(IfInputs.with(IfInputs.with(physical,"nodes",nodes),"bases",bases),"views",views);
        var result=lower(IfInputs.with(IfInputs.with(input,"storage",Optional.of(inventory)),"dataDeclarations",data));var p=result.publication().orElseThrow();
        check(p.storage().size()==2&&p.storage().stream().filter(Memory.Cell.class::isInstance).count()==1,"numeric scalar keeps one abstract Cell, without a duplicate Region");
        check(p.premises().stream().anyMatch(proof->proof.assertion() instanceof Proofs.DisjointStorage d&&d.storage().size()==2&&new HashSet<>(d.storage()).size()==2),"component proof covers region and legacy numeric Cell exactly once");
    }

}
