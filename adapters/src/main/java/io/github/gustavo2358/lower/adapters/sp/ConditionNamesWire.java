package io.github.gustavo2358.lower.adapters.sp;

import io.github.gustavo2358.lower.domain.ConditionNames;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;

/** SP 2.64 transport, with strict physical shape and typed semantic validation. */
final class ConditionNamesWire {
    record Range(ConditionNames.Value first,@Wire.Nullable ConditionNames.Value last) { }
    record Definition(String id,String parent,boolean anonymous,ConditionNames.VariableDomain domain,List<Range> ranges,@Wire.Nullable ConditionNames.Value falseValue,Wire.ProvenanceDocument variableProvenance,Wire.ProvenanceDocument provenance) { }
    record Use(String id,String statement,String definition,String operand,ConditionNames.Access access,List<ConditionNames.Index> indices,Wire.ProvenanceDocument provenance) { }
    record Document(List<Definition> definitions,List<Use> uses,List<ConditionNames.Assignment> assignments,List<ConditionNames.Predicate> predicates) { }
    static ConditionNames materialize(Document d,SpInput.UnitKey unit) {
        return new ConditionNames(d.definitions().stream().map(x->new ConditionNames.Definition(x.id(),x.parent(),x.anonymous(),x.domain(),x.ranges().stream().map(r->new ConditionNames.Range(r.first(),Optional.ofNullable(r.last()))).toList(),Optional.ofNullable(x.falseValue()),Materialize.provenance(x.variableProvenance(),unit),Materialize.provenance(x.provenance(),unit))).toList(),
            d.uses().stream().map(u->new ConditionNames.Use(u.id(),u.statement(),u.definition(),u.operand(),u.access(),u.indices(),Materialize.provenance(u.provenance(),unit))).toList(),d.assignments(),d.predicates());
    }
}
