package io.github.gustavo2358.lower.adapters.sp;
import io.github.gustavo2358.lower.domain.SpInput;
import java.math.BigDecimal;
import java.util.*;
final class NumericTransfersWire {
    record Transfer(String target,String value) { }
    static List<SpInput.NumericTransfer> materialize(List<Transfer> transfers,SpInput.StatementId statement) {
        return transfers.stream().map(t->new SpInput.NumericTransfer(new SpInput.OperandId(statement,t.target()),Optional.ofNullable(t.value()).map(BigDecimal::new))).toList();
    }
}
