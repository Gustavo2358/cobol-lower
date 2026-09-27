package io.github.gustavo2358.lower.adapters.testing;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.air.model.*;
public final class CicsCompletionSuite {
    public static void main(String[] args)throws Exception {
        var json=new ObjectMapper();var decoder=new SpJsonDecoder(CobolLower.INPUT_LIMITS);
        for(var name:java.util.List.of("rollback", "rollback-NOHANDLE", "rollback-RESP_RC_", "rollback-RESP2_RC2_")) {
            try(var stream=CicsCompletionSuite.class.getResourceAsStream("/sp/cics-completion/"+name+".json")) {
                var tree=(ObjectNode)json.readTree(stream);var decoded=decoder.decode(json.writeValueAsBytes(tree));
                if(!(decoded instanceof SpJsonDecoder.Decoded d))throw new AssertionError(decoded);
                var result=new CobolLowerer().lower(d.input(),CobolLower.OPTIONS);
                if(result.publication().isEmpty()||!result.validation().orElseThrow().isStructurallyValid())throw new AssertionError(result.status());
                var commands=result.publication().orElseThrow().units().stream().flatMap(u->u.sequences().stream()).map(Sequence::terminator)
                    .filter(t->t instanceof Operations.Opaque o&&o.observedKind().equals("cics-host-command/SYNCPOINT_ROLLBACK")).map(t->(Operations.Opaque)t).toList();
                if(commands.size()!=1)throw new AssertionError("distinct rollback operation");
                var command=commands.getFirst();var memory=command.envelope().memory();
                if(!memory.mustOverwrite().isEmpty()||!memory.knownReads().isEmpty()||memory.knownWrites().size()!=(name.contains("RESP")?1:0))throw new AssertionError("rollback does not revert application host memory");
                if(command.envelope().control().known().size()!=(name.equals("rollback")||name.contains("RESP2")?2:1))throw new AssertionError("ordinary and exceptional controls remain independent");
                tree.put("contractVersion","2.49.0");if(!(decoder.decode(json.writeValueAsBytes(tree)) instanceof SpJsonDecoder.Rejected))throw new AssertionError("new command under old contract");
            }
        }
        System.out.println("CICS_COMPLETION=PASS");
    }
}
