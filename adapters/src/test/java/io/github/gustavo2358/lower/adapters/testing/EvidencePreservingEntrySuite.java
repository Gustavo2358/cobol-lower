package io.github.gustavo2358.lower.adapters.testing;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.*;
import java.util.*;
import static io.github.gustavo2358.lower.testing.CallOracle.check;

/** Actual producer fixtures: logical evidence must survive missing physical location. */
public final class EvidencePreservingEntrySuite {
    private EvidencePreservingEntrySuite() { }
    public static void main(String[] args) throws Exception {
        fileContractVersionsPreserveLogicalEvidence();
        for(var name:List.of("unknown-offset","unknown-base","unknown-allocation","unknown-profile","preserved-profile","preserved-logical","initial-allocation","strong-invariant","exact-overwrite")) {
            var bytes=Objects.requireNonNull(EvidencePreservingEntrySuite.class.getResourceAsStream("/sp/evidence/"+name+".json")).readAllBytes();
            var decoded=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes);
            check(decoded instanceof SpJsonDecoder.Decoded,"source evidence contract: "+name+" "+decoded);
            var result=RegionalTranslationSuite.lower(((SpJsonDecoder.Decoded)decoded).input());
            check(result.publication().isPresent(),"source evidence must lower: "+name+" "+result.status()+" "+result.admission());
            var p=result.publication().orElseThrow();var unit=p.units().getFirst();
            check(!unit.entries().getFirst().state().conditions().isEmpty(),"supported entry fact was lost: "+name);
            var condition=unit.entries().getFirst().state().conditions().getFirst();
            boolean strong=name.equals("strong-invariant");
            check(strong?condition.value() instanceof Entries.LiteralInitial:condition.value() instanceof Entries.PossibleLiterals,"only proved invariant may be strong: "+name);
            var literals=strong?List.of(((Entries.LiteralInitial)condition.value()).value().value()):((Entries.PossibleLiterals)condition.value()).candidates().stream().map(Expressions.Literal::value).toList();
            check(literals.equals(List.of(new Values.TextValue("PROGA   ")))||literals.equals(List.of(new Values.BytesValue(List.of(215,217,214,199,193,64,64,64)))),"source candidate conserved: "+literals);
            if(!strong)check(p.capabilities().required().contains(new Capabilities.Capability("entry.possibilities","2")),"negotiated v2");
            var invoke=(Operations.Invoke)unit.sequences().stream().map(Sequence::terminator).filter(Operations.Invoke.class::isInstance).findFirst().orElseThrow();
            check(invoke.target() instanceof Interactions.ComputedTarget t&&t.name() instanceof Expressions.Read,"logical CALL read remains representable");
            if(List.of("unknown-offset","unknown-base","unknown-profile","preserved-logical").contains(name)) {
                check(condition.place() instanceof Places.ObjectPlace,"unknown physical view uses logical object");
                var object=((Places.ObjectPlace)condition.place()).object();
                var binding=unit.objects().stream().filter(o->o.id().equals(object)).map(Memory.ObjectDeclaration::storage).findFirst().orElseThrow();
                if(List.of("unknown-offset","unknown-base").contains(name)) {
                    check(binding instanceof Memory.UnknownBinding,"unproved partial relation remains bounded location uncertainty: "+name);
                    check(result.data().stream().filter(d->d.object().equals(object)).allMatch(d->d.storage().isEmpty()),"no invented physical identity: "+name);
                } else {
                    check(binding instanceof Memory.CellBinding,"supported logical value has positive Cell despite physical gap: "+name+" "+binding);
                    var cell=((Memory.CellBinding)binding).storage();
                    check(p.storage().stream().anyMatch(s->s instanceof Memory.Cell c&&c.header().id().equals(cell)),"logical Cell published: "+name);
                    check(result.data().stream().filter(d->d.object().equals(object)).allMatch(d->d.storage().equals(Optional.of(cell))),"DataLink retains logical storage identity: "+name);
                }
            }
            var codec=new AirJson();check(p.equals(codec.decode(codec.encode(p))),"AIR codec preserves entry and target");
            if(name.equals("unknown-allocation")) {
                // A selected profile does not force a producer to claim physical bytes.
                var mapper=new ObjectMapper();var logical=mapper.readTree(bytes);
                var conditionNode=(ObjectNode)logical.path("storage").path("entryState").path("conditions").get(0);
                conditionNode.put("kind","POSSIBLE_LOGICAL_TEXT");conditionNode.put("logicalText","PROGA   ");conditionNode.set("bytes",mapper.createArrayNode());
                var decodedLogical=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(mapper.writeValueAsBytes(logical));
                check(decodedLogical instanceof SpJsonDecoder.Decoded,"logical support can coexist with an encoding profile");
                var logicalResult=RegionalTranslationSuite.lower(((SpJsonDecoder.Decoded)decodedLogical).input());
                check(logicalResult.publication().isPresent(),"logical support cannot become a bytes-domain literal");
                var lp=logicalResult.publication().orElseThrow();check(lp.equals(codec.decode(codec.encode(lp))),"logical domain roundtrip");
            }
            if(name.equals("initial-allocation")) {
                var mapper=new ObjectMapper();var changed=mapper.readTree(bytes);
                var conditionNode=(ObjectNode)changed.path("storage").path("entryState").path("conditions").get(0);
                conditionNode.put("kind","LITERAL_BYTES");conditionNode.put("proof","EXPLICIT_INITIAL");conditionNode.set("gapCodes",mapper.createArrayNode());
                var invalid=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(mapper.writeValueAsBytes(changed));
                if(invalid instanceof SpJsonDecoder.Decoded v)check(new CobolLowerer().lower(v.input(),CobolLower.OPTIONS).status()==LoweringResult.Status.INVALID_INPUT,"initial mode cannot authorize unproved allocation");
                else check(invalid instanceof SpJsonDecoder.Rejected,"explicit rejection of unsupported strong initialization");
            }
            if(name.equals("unknown-profile")) {
                var mapper=new ObjectMapper();var tree=mapper.readTree(bytes);
                for(var bad:List.of("missing-text","invented-bytes","closed","strong-proof","old-version")) {
                    var changed=tree.deepCopy();var conditionNode=(ObjectNode)changed.path("storage").path("entryState").path("conditions").get(0);
                    switch(bad) {
                        case "missing-text"->conditionNode.remove("logicalText");
                        case "invented-bytes"->conditionNode.set("bytes",mapper.createArrayNode().add(65));
                        case "closed"->conditionNode.set("gapCodes",mapper.createArrayNode());
                        case "strong-proof"->conditionNode.put("proof","DECLARATIVE_INVARIANT");
                        case "old-version"->{((ObjectNode)changed).put("contractVersion","2.18.0");((ObjectNode)changed.path("storage")).put("version","1.5.0");}
                    }
                    var invalid=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(mapper.writeValueAsBytes(changed));
                    if(invalid instanceof SpJsonDecoder.Decoded v)check(new CobolLowerer().lower(v.input(),CobolLower.OPTIONS).status()==LoweringResult.Status.INVALID_INPUT,"typed rejection: "+bad);
                    else check(invalid instanceof SpJsonDecoder.Rejected,"wire rejection: "+bad);
                }
            }
        }
        logicalCopy();
        System.out.println("EP_ENTRY_LOWER=PASS source/entry/target/codec/no invented allocation");
    }
    private static void fileContractVersionsPreserveLogicalEvidence() throws Exception {
        var mapper=new ObjectMapper();
        var source=mapper.readTree(Objects.requireNonNull(EvidencePreservingEntrySuite.class.getResourceAsStream("/sp/evidence/unknown-base.json")).readAllBytes());
        var decoder=new SpJsonDecoder(CobolLower.INPUT_LIMITS);
        for(int minor=21;minor<=28;minor++) {
            var document=source.deepCopy();((ObjectNode)document).put("contractVersion","2."+minor+".0");
            ((ObjectNode)document.path("storage")).put("version",minor>=24?"1.8.0":"1.7.0");
            var files=((ObjectNode)document).putObject("fileInventory");
            files.put("version","1."+Math.min(minor-21,6)+".0");files.put("availability","KNOWN");files.putArray("declarations");files.putArray("gapCodes");
            if(minor>=22){var operations=files.putObject("operations");operations.put("availability","KNOWN");operations.putArray("uses");operations.putArray("gapCodes");}
            if(minor>=25)files.putArray("declaratives");
            if(minor>=26){files.put("sortAvailability","KNOWN");files.putArray("sortPlans");}
            if(minor>=27){var auxiliary=files.putObject("auxiliary");auxiliary.put("availability","KNOWN");auxiliary.putArray("clauses");auxiliary.putArray("gapCodes");}
            var decoded=decoder.decode(mapper.writeValueAsBytes(document));
            check(decoded instanceof SpJsonDecoder.Decoded,"current file contract must decode: "+minor+" "+decoded);
            var input=((SpJsonDecoder.Decoded)decoded).input();
            check(input.storage().orElseThrow().entryState().possibilityDomain()==io.github.gustavo2358.lower.domain.StorageFacts.PossibilityDomain.LOGICAL_SOURCE,"SP2."+minor+" retains source evidence independently of physical view");
            var result=RegionalTranslationSuite.lower(input);check(result.publication().isPresent(),"current contract keeps unknown-base candidate");
            var condition=result.publication().orElseThrow().units().getFirst().entries().getFirst().state().conditions().getFirst();
            check(condition.place() instanceof Places.ObjectPlace&&condition.value() instanceof Entries.PossibleLiterals,"no physical binding or strong value invented");
        }
        var legacy=source.deepCopy();((ObjectNode)legacy).put("contractVersion","2.18.0");((ObjectNode)legacy.path("storage")).put("version","1.5.0");
        for(var reference:legacy.findParents("logicalWholeItem"))((ObjectNode)reference).remove("logicalWholeItem");
        for(var condition:legacy.path("storage").path("entryState").path("conditions"))((ObjectNode)condition).remove("logicalText");
        var old=decoder.decode(mapper.writeValueAsBytes(legacy));check(old instanceof SpJsonDecoder.Decoded,"legacy bounded-physical shape remains readable");
        var input=((SpJsonDecoder.Decoded)old).input();
        check(input.storage().orElseThrow().entryState().possibilityDomain()==io.github.gustavo2358.lower.domain.StorageFacts.PossibilityDomain.BOUNDED_PHYSICAL,"legacy domain is not promoted");
        check(new CobolLowerer().lower(input,CobolLower.OPTIONS).status()==LoweringResult.Status.INVALID_INPUT,"legacy requires its physical view proof");
    }
    private static void logicalCopy() throws Exception {
        var bytes=Objects.requireNonNull(EvidencePreservingEntrySuite.class.getResourceAsStream("/sp/evidence/logical-copy.json")).readAllBytes();
        var decoder=new SpJsonDecoder(CobolLower.INPUT_LIMITS);var decoded=decoder.decode(bytes);
        check(decoded instanceof SpJsonDecoder.Decoded,"SP 2.20 logical copy contract");
        var result=new CobolLowerer().lower(((SpJsonDecoder.Decoded)decoded).input(),CobolLower.OPTIONS);
        check(result.status()==LoweringResult.Status.PARTIAL,"open repertoire cannot become success: "+result.status());
        var p=result.publication().orElseThrow();var validation=result.validation().orElseThrow();
        check(!validation.isStructurallyValid()&&validation.unprovedOperationPreconditions().isPresent(),"actual incomplete report retained");
        var assign=p.units().getFirst().sequences().stream().flatMap(s->s.instructions().stream()).filter(Operations.Assign.class::isInstance).map(Operations.Assign.class::cast).findFirst().orElseThrow();
        check(assign.value() instanceof Expressions.FitText fit&&fit.value() instanceof Expressions.Read read&&read.place() instanceof Places.ObjectPlace,"existing typed copy relation");
        var source=(Places.ObjectPlace)((Expressions.Read)((Expressions.FitText)assign.value()).value()).place();
        check(p.units().getFirst().objects().stream().filter(o->o.id().equals(source.object())).allMatch(o->o.storage() instanceof Memory.UnknownBinding),"source binding stays unknown");
        var codec=new AirJson();
        try {codec.encode(p);throw new AssertionError("strict encoder accepted incomplete output");}
        catch(io.github.gustavo2358.air.json.AirJsonException expected){check(expected.code()==io.github.gustavo2358.air.json.AirJsonException.Code.INCOMPLETE_VALIDATION,"strict incomplete report");}
        var partial=codec.encodeForPartialAnalysis(p);check(codec.decodeForPartialAnalysis(partial.bytes()).publication().equals(p),"partial bilateral roundtrip");
        var directory=Files.createTempDirectory("ep-logical-copy-");var input=directory.resolve("sp.json");var output=directory.resolve("air.json");Files.write(input,bytes);
        var log=new java.io.ByteArrayOutputStream();check(CobolLower.run(new String[]{input.toString(),output.toString()},new java.io.PrintStream(log))==0,"partial CLI output");
        check(log.toString(java.nio.charset.StandardCharsets.UTF_8).contains("PARTIAL")&&codec.decodeForPartialAnalysis(Files.readAllBytes(output)).validation().unprovedOperationPreconditions().isPresent(),"partial result is explicit");
        var mapper=new ObjectMapper();var tree=mapper.readTree(bytes);
        var withoutValue=tree.deepCopy();((ObjectNode)withoutValue.path("storage").path("entryState")).set("conditions",mapper.createArrayNode());
        var noValue=(SpJsonDecoder.Decoded)decoder.decode(mapper.writeValueAsBytes(withoutValue));
        var noValueResult=new CobolLowerer().lower(noValue.input(),CobolLower.OPTIONS);
        check(noValueResult.status()==LoweringResult.Status.PARTIAL,"logical identity cannot depend on having VALUE");
        check(noValueResult.publication().orElseThrow().units().getFirst().entries().getFirst().state().conditions().isEmpty(),"absence of VALUE cannot manufacture an entry candidate");
        for(var mutation:List.of("old-version","missing-identity","unproved-alias","inexact-source")) {
            var changed=tree.deepCopy();var ref=(ObjectNode)changed.path("statements").get(0).path("source").path("reference");
            switch(mutation) {
                case "old-version"->{((ObjectNode)changed).put("contractVersion","2.19.0");((ObjectNode)changed.path("storage")).put("version","1.6.0");}
                case "missing-identity"->ref.putNull("logicalWholeItem");
                case "unproved-alias"->((ObjectNode)changed.path("storage").path("bases").get(0)).put("allocation","UNKNOWN");
                case "inexact-source"->((ObjectNode)ref.path("provenance")).put("exact",false);
            }
            var bad=decoder.decode(mapper.writeValueAsBytes(changed));
            if(bad instanceof SpJsonDecoder.Decoded d)check(new CobolLowerer().lower(d.input(),CobolLower.OPTIONS).status()==LoweringResult.Status.INVALID_INPUT,"typed contradiction refused: "+mutation);
            else check(bad instanceof SpJsonDecoder.Rejected,"wire contradiction refused: "+mutation);
        }
    }

}
