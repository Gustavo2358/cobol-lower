package io.github.gustavo2358.lower.adapters.testing;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.math.BigDecimal;
import java.util.*;
/** IBM's 123451 oracle, evaluated after both literal and DATA lowering. */
public final class TruncSuite {
    private static final ObjectMapper JSON=new ObjectMapper();
    private static final SpJsonDecoder DECODER=new SpJsonDecoder(CobolLower.INPUT_LIMITS);
    public static void main(String[] args) throws Exception {
        for(var mode:List.of("std","bin")) {
            var fixture=(ObjectNode)JSON.readTree(TruncSuite.class.getResourceAsStream("/sp/trunc/"+mode+".json"));
            var decoded=DECODER.decode(JSON.writeValueAsBytes(fixture));
            if(!(decoded instanceof SpJsonDecoder.Decoded ok))throw new AssertionError(decoded);
            var result=new CobolLowerer().lower(ok.input(),CobolLower.POSITIVE_OPTIONS);
            var p=result.publication().orElseThrow(()->new AssertionError(result.admission().diagnostics()));
            if(!result.validation().orElseThrow().isStructurallyValid())throw new AssertionError(result.validation());
            var memory=new HashMap<ObjectId,BigDecimal>();var actual=new ArrayList<BigDecimal>();
            for(var s:ok.input().statements())if(s instanceof SpInput.MoveFact) {
                var labels=new HashSet<LabelId>();result.statements().stream().filter(l->l.source().equals(s.header().id())).forEach(l->labels.add(l.label()));
                for(var u:p.units())for(var q:u.sequences())if(labels.contains(q.label()))for(var i:q.instructions()) {
                    if(!(i instanceof Operations.Assign a)||!(a.destination() instanceof Places.ObjectPlace dest))throw new AssertionError(i);
                    var value=NumericConversionSuite.eval(a.value(),memory);memory.put(dest.object(),value);actual.add(value);
                }
            }
            var receiving=new BigDecimal(mode.equals("std")?"51":"-7621");
            if(!actual.equals(List.of(new BigDecimal("123451"),receiving,receiving)))throw new AssertionError(mode+actual);
            for(var invalid:List.of("UNSPECIFIED","OPT","INVALID",mode.equals("std")?"BIN":"STD")) {
                var copy=fixture.deepCopy();((ObjectNode)copy.path("dataDeclarations").get(1).path("scalarNumber")).put("trunc",invalid);
                var d=DECODER.decode(JSON.writeValueAsBytes(copy));
                if(d instanceof SpJsonDecoder.Decoded v&&new CobolLowerer().lower(v.input(),CobolLower.POSITIVE_OPTIONS).publication().isPresent())throw new AssertionError("inconsistent TRUNC admitted "+invalid);
            }
        }
        System.out.println("TRUNC=PASS: STD/BIN literal and DATA oracle; missing/OPT/forged certificates rejected");
    }
}
