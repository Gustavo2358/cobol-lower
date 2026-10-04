package io.github.gustavo2358.lower.adapters.sp;
import io.github.gustavo2358.lower.domain.SpInput;
import java.math.BigInteger;
import java.util.*;
final class IntegerTransfersWire {
    record Transfer(String target,String value) { }
    static List<SpInput.IntegerTransfer> materialize(List<Transfer> transfers,SpInput.StatementId statement) {
        return transfers.stream().map(t->new SpInput.IntegerTransfer(new SpInput.OperandId(statement,t.target()),Optional.ofNullable(t.value()).map(BigInteger::new))).toList();
    }
}
