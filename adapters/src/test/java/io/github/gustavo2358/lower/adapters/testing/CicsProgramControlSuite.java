package io.github.gustavo2358.lower.adapters.testing;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

/** Public SP2.14 producer fixtures; transport/admission/physical target contracts. */
public final class CicsProgramControlSuite {
    private CicsProgramControlSuite(){ }
    static io.github.gustavo2358.lower.domain.SpInput composition(int n) throws Exception {
        var input=PartialIntegrationSuite.fixture("compose-"+n);
        var facts=input.statements().stream().map(f->{
            if(!(f instanceof io.github.gustavo2358.lower.domain.SpInput.CallFact c))return f;
            return new io.github.gustavo2358.lower.domain.SpInput.CicsFact(c.header(),io.github.gustavo2358.lower.domain.SpInput.CicsCommand.LINK,
                "EXEC CICS LINK PROGRAM(WS-A) NOHANDLE END-EXEC",Optional.of(c.target()),List.of(new io.github.gustavo2358.lower.domain.SpInput.CicsOption("PROGRAM",Optional.of("WS-A"),15,28,Optional.empty()),new io.github.gustavo2358.lower.domain.SpInput.CicsOption("NOHANDLE",Optional.empty(),29,37,Optional.empty())),io.github.gustavo2358.lower.domain.SpInput.CicsConditions.LOCAL_CONDITION,c.normalContinuation(),c.normalContinuation(),"cics-ts.program@1",List.of("CICS_EFFECTS_SIGNATURE_PARTIAL"));
        }).toList();
        var result=io.github.gustavo2358.lower.testing.IfInputs.with(input,"statements",facts);
        var lowered=PartialIntegrationSuite.lower(result).publication().orElseThrow();
        long count=lowered.units().getFirst().sequences().stream().map(Sequence::terminator).filter(Operations.Invoke.class::isInstance).map(Operations.Invoke.class::cast)
            .filter(i->i.target() instanceof Interactions.ComputedTarget t&&t.namespace().equals("cics.program")||i.target() instanceof Interactions.LiteralTarget l&&l.namespace().equals("cics.program")).count();
        if(count<n)throw new AssertionError("CICS multiplicity/elision "+n);
        return result;
    }
    private static void contradictoryConditions() throws Exception {
        byte[] raw;try(var in=CicsProgramControlSuite.class.getResourceAsStream("/sp/cics/xctl-nohandle.json")){raw=in.readAllBytes();}
        var decoder=new SpJsonDecoder(CobolLower.INPUT_LIMITS);
        var original=((SpJsonDecoder.Decoded)decoder.decode(raw)).input();
        if(new CobolLowerer().lower(original,CobolLower.OPTIONS).status()!=LoweringResult.Status.SUCCESS)throw new AssertionError("baseline XCTL/NOHANDLE must lower successfully");
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();var doc=mapper.readTree(raw);
        for(var f:doc.path("statements"))if(f.path("variant").asText().equals("CICS_PROGRAM_CONTROL")) {
            ((com.fasterxml.jackson.databind.node.ObjectNode)f).put("conditions","DEFAULT_ENTRY_PREFIX");
        }
        var bytes=mapper.writeValueAsBytes(doc);var decoded=new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(bytes);
        if(decoded instanceof SpJsonDecoder.Decoded d&&new CobolLowerer().lower(d.input(),CobolLower.OPTIONS).status()==LoweringResult.Status.SUCCESS)
            throw new AssertionError("contradictory NOHANDLE/default profile accepted by public decoder/lower");
        var dir=java.nio.file.Files.createTempDirectory("cics-conditions-");
        try {
            var source=dir.resolve("input.json");var output=dir.resolve("air.json");java.nio.file.Files.write(source,bytes);java.nio.file.Files.writeString(output,"previous-output");
            var err=new java.io.ByteArrayOutputStream();int code=CobolLower.run(new String[]{source.toString(),output.toString()},new java.io.PrintStream(err));
            if(code!=CobolLower.INPUT&&code!=CobolLower.LOWERING)throw new AssertionError("explicit rejection: "+code+" "+err);
            if(!java.nio.file.Files.readString(output).equals("previous-output"))throw new AssertionError("rejection replaced previous output");
            System.out.println("CICS_CONDITIONS_REJECTED_OUTPUT_PRESERVED "+err.toString().strip());
        } finally {try(var files=java.nio.file.Files.list(dir)){for(var file:files.toList())java.nio.file.Files.delete(file);}java.nio.file.Files.delete(dir);}
    }
    private static void unavailableLinkReturn() throws Exception {
        byte[] raw;try(var in=CicsProgramControlSuite.class.getResourceAsStream("/sp/cics/link-paragraph.json")){raw=in.readAllBytes();}
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();var doc=mapper.readTree(raw);
        for(var f:doc.path("statements"))if(f.path("variant").asText().equals("CICS_PROGRAM_CONTROL"))for(var field:List.of("localContinuation","ordinaryContinuation")) {
            if(!f.path("conditions").asText().equals("DEFAULT_ENTRY_PREFIX")||!f.path("command").asText().equals("LINK"))throw new AssertionError("expected DEFAULT LINK fixture");
            var continuation=(com.fasterxml.jackson.databind.node.ObjectNode)f.path(field);continuation.put("availability","UNAVAILABLE");continuation.putNull("statement");
        }
        var input=((SpJsonDecoder.Decoded)new SpJsonDecoder(CobolLower.INPUT_LIMITS).decode(mapper.writeValueAsBytes(doc))).input();
        var publication=new CobolLowerer().lower(input,CobolLower.OPTIONS).publication().orElseThrow();
        var invoke=publication.units().getFirst().sequences().stream().map(Sequence::terminator).filter(Operations.Invoke.class::isInstance).map(Operations.Invoke.class::cast)
            .filter(i->i.target() instanceof Interactions.LiteralTarget t&&t.namespace().equals("cics.program")).findFirst().orElseThrow();
        if(!invoke.outcomes().known().isEmpty()||!(((Scopes.WithinControl)invoke.outcomes().remainder()).scope() instanceof Scopes.UnitControl u&&u.labels()))
            throw new AssertionError("unavailable LINK return must retain local control even with DEFAULT_ENTRY_PREFIX");
        System.out.println("CICS_LINK_UNAVAILABLE_RETURN_CONSERVATIVE");
    }
    public static void main(String[] args) throws Exception {
        if(args.length>0&&args[0].equals("conditions")){contradictoryConditions();return;}
        contradictoryConditions();
        unavailableLinkReturn();
        int cases=0;var decoder=new SpJsonDecoder(CobolLower.INPUT_LIMITS);
        for(String name:List.of("variable","qualified","short","slice","overlay","group")) {
            byte[] raw;try(var in=CicsProgramControlSuite.class.getResourceAsStream("/sp/cics/"+name+".json")){if(in==null)throw new AssertionError(name);raw=in.readAllBytes();}
            var input=((SpJsonDecoder.Decoded)decoder.decode(raw)).input();
            var result=new CobolLowerer().lower(input,CobolLower.OPTIONS);
            if(result.status()!=LoweringResult.Status.SUCCESS)throw new AssertionError(result.status()+" "+result.validation());
            var publication=result.publication().orElseThrow();var codec=new AirJson();if(!publication.equals(codec.decode(codec.encode(publication))))throw new AssertionError("roundtrip");
            var invoke=publication.units().getFirst().sequences().stream().map(Sequence::terminator).filter(Operations.Invoke.class::isInstance).map(Operations.Invoke.class::cast)
                .filter(i->i.target() instanceof Interactions.ComputedTarget t&&t.namespace().equals("cics.program")).findFirst().orElseThrow();
            var target=(Interactions.ComputedTarget)invoke.target();
            if(!(target.namePolicy() instanceof Interactions.ExtensionName e&&e.name().equals("cics-ts.program")))throw new AssertionError("platform policy");
            if(name.equals("short")!=(target.name() instanceof Expressions.Unknown))throw new AssertionError("eight-byte proof");
            if(!name.equals("short")&&!(target.name() instanceof Expressions.Read))throw new AssertionError("storage read");
            if(name.equals("slice")&&!(((Expressions.Read)target.name()).place() instanceof Places.RegionSlice))throw new AssertionError("slice identity");
            var signature=((Interactions.ExternalSignature)invoke.signature()).signature();
            if(!(signature.parameters().remainder() instanceof Interactions.UnknownRemainder)||!(signature.results().remainder() instanceof Interactions.UnknownRemainder))throw new AssertionError("partial signature");
            if(invoke.outcomes().known().stream().noneMatch(Control.Normal.class::isInstance))throw new AssertionError("LINK return");
            for(String version:List.of("2.12.0","2.13.0")) {
                var old=new String(raw,StandardCharsets.UTF_8).replace("2.14.0",version).getBytes(StandardCharsets.UTF_8);
                if(decoder.decode(old) instanceof SpJsonDecoder.Decoded)throw new AssertionError("old contract accepted new variant: "+version);
            }
            cases++;
        }
        System.out.println("CICS_LOWER_CASES="+cases);
    }
}
