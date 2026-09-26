package io.github.gustavo2358.lower.adapters.testing;

import java.util.*;
import java.nio.file.*;
import java.io.*;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.*;
import io.github.gustavo2358.lower.adapters.source.*;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.source.QualifiedSourceDependencies.*;

/** Independent fixture expectations; values/control are never inferred by R9. */
public final class QualifiedSourceSuite {
    static int checks;
    static void need(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static byte[] fixture(String name)throws Exception{try(var in=QualifiedSourceSuite.class.getResourceAsStream("/qualified-source-r9/"+name+".sp.json")){return Objects.requireNonNull(in).readAllBytes();}}
    public static void main(String[] args)throws Exception {
        var expected=Map.ofEntries(Map.entry("ordinary",Set.of("ORDPGM")),Map.entry("explicit",Set.of("HANDPGM")),Map.entry("conditional",Set.of("CONDPGM","X")),Map.entry("canceled",Set.of()),Map.entry("gap",Set.of("KEEPPGM")),Map.entry("alter",Set.of("KEEPPGM","TEXTUAL")),Map.entry("replacement",Set.of("NEWPGM")),Map.entry("deactivated",Set.of()),Map.entry("computed",Set.of()),Map.entry("alternatives",Set.of("BOTH","X")),Map.entry("registration",Set.of()));
        for(var name:expected.keySet().stream().sorted().toList()) {
            var bytes=fixture(name);var input=((SpJsonDecoder.Decoded)new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes)).input();
            var lowered=new CobolLowerer().lower(input,CobolLower.POSITIVE_OPTIONS);var before=lowered.admission().handlerState();
            var evidence=QualifiedSourceProjection.project(input,lowered.admission());
            need(evidence.equals(QualifiedSourceProjection.project(input,lowered.admission())),"deterministic projection");
            need(before.equals(lowered.admission().handlerState()),"R7 state immutable");
            var values=new TreeSet<String>();for(var o:evidence.occurrences())if(!o.qualifications().isEmpty())for(var v:o.values())values.add(v.value());
            need(values.equals(expected.get(name)),"qualified literal values "+name+" "+values);
            if(name.equals("computed")){var o=evidence.occurrences().getFirst();need(!o.qualifications().isEmpty()&&o.values().isEmpty()&&o.valueRemainder(),"reachability is not computed value proof");}
            if(name.equals("conditional")){var s=evidence.selections().getFirst();need(s.guards().size()==2&&s.stateOnEntry().getFirst().kind().equals("DEACTIVATED"),"conditional entry qualification");}
            if(name.equals("alternatives")){var o=evidence.occurrences().stream().filter(x->x.values().stream().anyMatch(v->v.value().equals("BOTH"))).findFirst().orElseThrow();need(o.qualifications().size()>=2,"ordinary and conditional alternatives independent");}
            if(name.equals("canceled")||name.equals("deactivated"))need(evidence.selections().stream().anyMatch(s->s.target().isEmpty()&&s.outerLevelRemainder()),"bounded outer remainder");
            var dir=Files.createTempDirectory("r9-source-");try {
                var sp=dir.resolve("input.json");var air=dir.resolve("air.json");var ordinary=dir.resolve("ordinary.json");var side=dir.resolve("source.json");Files.write(sp,bytes);
                var errors=new ByteArrayOutputStream();var err=new PrintStream(errors);
                need(CobolLower.run(new String[]{sp.toString(),ordinary.toString()},err)==0,"legacy CLI");
                need(CobolLower.run(new String[]{sp.toString(),air.toString(),"--source-evidence",side.toString()},err)==0,"source CLI "+errors);
                need(Arrays.equals(Files.readAllBytes(air),Files.readAllBytes(ordinary)),"zero executable AIR delta "+name);
                var manifest=dir.resolve("dependency-input.json");
                need(io.github.gustavo2358.lower.adapters.cli.CobolDependencyInput.run(new String[]{sp.toString(),manifest.toString()},err)==0,"explicit dependency input "+errors);
                var bundle=new com.fasterxml.jackson.databind.ObjectMapper().readTree(Files.readAllBytes(manifest));
                need(bundle.path("schema").asText().equals("dependency-input"),"input schema");
                need(Arrays.equals(Files.readAllBytes(air),Files.readAllBytes(dir.resolve(bundle.path("air").path("path").asText()))),"bundle preserves AIR bytes");
                var first=Files.readAllBytes(manifest);
                need(io.github.gustavo2358.lower.adapters.cli.CobolDependencyInput.run(new String[]{sp.toString(),manifest.toString()},err)==0,"repeat bundle");
                need(Arrays.equals(first,Files.readAllBytes(manifest)),"deterministic bundle");
                var snapshotPath=dir.resolve(bundle.path("air").path("path").asText());var snapshot=Files.readAllBytes(snapshotPath);
                need(io.github.gustavo2358.lower.adapters.cli.CobolDependencyInput.run(new String[]{sp.toString(),snapshotPath.toString()},err)==CobolLower.OUTPUT,"manifest cannot overwrite its AIR snapshot");
                need(Arrays.equals(snapshot,Files.readAllBytes(snapshotPath)),"failed publication preserves existing snapshot");
                var codec=new QualifiedSourceJson();var contract=codec.decode(Files.readAllBytes(side));need(contract.units().equals(List.of(evidence)),"memory and file port parity");
                need(codec.decode(codec.encode(contract)).equals(contract),"producer roundtrip");
            } finally {try(var paths=Files.walk(dir)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}}
        }
        // Existing R7 adversarial family is projected intact, including RESET and PERFORM.
        for(var name:List.of("replacement","deactivated","reset","nested-perform","handler-branch","completion-bound")) {
            var r=ExceptionalHandlerSuite.run(name);var q=QualifiedSourceProjection.project(r.input(),r.result().admission());
            need(q.nodes().size()==r.a().nodes().size()&&q.derivations().size()==r.a().derivations().size()&&q.selections().size()==r.a().selections().size(),"complete R7 certificate "+name);
        }
        var multi=Files.createTempDirectory("r9-multi-");try {
            var sp=multi.resolve("sp.json");try(var in=QualifiedSourceSuite.class.getResourceAsStream("/sp/file-scope/nested.json")){Files.write(sp,Objects.requireNonNull(in).readAllBytes());}
            var err=new PrintStream(new ByteArrayOutputStream());var air=multi.resolve("air.json");var source=multi.resolve("source.json");
            need(CobolLower.run(new String[]{sp.toString(),air.toString(),"--source-evidence",source.toString()},err)==0,"multi-unit CLI source evidence");
            var q=new QualifiedSourceJson().decode(Files.readAllBytes(source));need(q.units().size()>1,"all compilation units preserved");
        } finally {try(var paths=Files.walk(multi)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}}
        System.out.println("R9_QUALIFIED_SOURCE_CHECKS="+checks);
    }
}
