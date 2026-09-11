package io.github.gustavo2358.lower.testing;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.air.validation.*;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.SpInput;
import java.util.*;

/** Independent relational expected: does not call translation/identity/evidence helpers. */
public final class CallOracle {
    private CallOracle() { }
    private static int assertions;
    public static int assertions() { return assertions; }
    public static void check(boolean condition, String rule) { if (!condition) throw new AssertionError("CP6 " + rule); assertions++; }
    public static Operations.Invoke inspect(SpInput input, LoweringResult result, List<String> values, boolean computed, String name) {
        check(result.status() == LoweringResult.Status.SUCCESS, "SUCCESS: " + result.status() + " " + result.admission().diagnostics() + " " + result.validation());
        var publication = result.publication().orElseThrow(); check(publication.units().size() == 1, "one caller unit");
        var unit = publication.units().getFirst();
        check(unit.sequences().size() == 2, "CALL splits exactly two sequences");
        var first = unit.sequences().getFirst(); var next = unit.sequences().getLast();
        check(first.terminator() instanceof Operations.Invoke, "Invoke is the terminator");
        check(first.instructions().stream().allMatch(Operations.Assign.class::isInstance) && first.instructions().size() == values.size(), "only expected Assign instructions");
        check(next.instructions().isEmpty() && next.terminator() instanceof Operations.Return r && r.values().isEmpty(), "unique real GOBACK Return");
        check(unit.entries().getFirst().initialLabel().equals(Optional.of(first.label())), "entry starts at CALL/pre-CALL sequence");
        var call = input.statements().stream().filter(SpInput.CallFact.class::isInstance).map(SpInput.CallFact.class::cast).findFirst().orElseThrow();
        var invoke = (Operations.Invoke) first.terminator();
        check(invoke.action().equals("call"), "call action");
        check(invoke.outcomes().known().equals(List.of(new Control.Normal(next.label()))), "explicit normal destination");
        check(invoke.outcomes().remainder().equals(new Scopes.WithinControl(new Scopes.AllControl(publication.id()))), "open all-control remainder");
        check(invoke.arguments().isEmpty() && invoke.results().isEmpty() && invoke.effectOperands().isEmpty(), "zero operands, no invented target effect operand");
        check(invoke.signature() instanceof Interactions.ExternalSignature, "external signature");
        var signature = ((Interactions.ExternalSignature) invoke.signature()).signature();
        check(signature.parameters().known().isEmpty() && signature.results().known().isEmpty()
            && signature.parameters().remainder() == Interactions.NoRemainder.INSTANCE && signature.results().remainder() == Interactions.NoRemainder.INSTANCE, "known empty signature");
        var memory = new Scopes.WithinMemory(new Scopes.AllMemory(publication.id(), true));
        check(invoke.effectBound().otherwise().reads().equals(memory) && invoke.effectBound().otherwise().writes().equals(memory), "conservative memory effects");
        check(invoke.effectBound().otherwise().mustOverwrite().isEmpty() && invoke.effectBound().perOutcome().isEmpty(), "no must-write claim");
        check(invoke.contract() instanceof Interactions.UnknownContract, "unknown contract");
        var origins = new HashMap<OriginId, Origins.Origin>(); publication.origins().forEach(o -> origins.put(o.id(), o));
        OriginId targetOrigin; Interactions.NamePolicy policy;
        if (computed) {
            check(invoke.target() instanceof Interactions.ComputedTarget, "dynamic target remains computed");
            var target = (Interactions.ComputedTarget) invoke.target(); policy = target.namePolicy(); targetOrigin = target.origin();
            check(target.category().equals("program") && target.namespace().equals("cobol.program"), "source namespace");
            check(target.name() instanceof Expressions.Read, "computed name is Read");
            var read = (Expressions.Read) target.name(); check(read.place() instanceof Places.ObjectPlace, "read whole object");
            var place = (Places.ObjectPlace) read.place();
            var source = ((SpInput.DataCallTarget) call.target()).reference();
            var data = result.data().stream().filter(d -> d.source().equals(source.binding().selected().orElseThrow())).findFirst().orElseThrow();
            check(place.object().equals(data.object()), "target ObjectId follows selected DATA");
            check(read.header().role() == Operand.Role.CALL_TARGET && place.header().role() == Operand.Role.VALUE_READ, "target/read roles");
            check(!targetOrigin.equals(read.header().origin()) && !read.header().origin().equals(place.header().origin()), "distinct target/read/place origins");
            check(hasLocation(origins, read.header().origin(), source.provenance().original()) && hasLocation(origins, place.header().origin(), source.provenance().original()), "target source location preserved");
            for (var id : List.of(read.header().id(), place.header().id())) check(result.operands().stream().anyMatch(l -> l.source().equals(source.id()) && l.target().equals(id)), "DATA target operand correlation");
        } else {
            check(invoke.target() instanceof Interactions.LiteralTarget, "literal target retained");
            var target = (Interactions.LiteralTarget) invoke.target(); policy = target.namePolicy(); targetOrigin = target.origin();
            check(target.name().equals(name) && target.category().equals("program") && target.namespace().equals("cobol.program"), "literal raw logical text and namespace");
        }
        check(policy instanceof Interactions.UnknownName, "unknown runtime name policy, no canonicalization");
        check(!targetOrigin.equals(invoke.header().origin()), "separate invoke/target origin");
        check(invoke.header().precision().control().status() == Evidence.PrecisionStatus.OPEN
            && invoke.header().precision().effects().status() == Evidence.PrecisionStatus.OPEN
            && invoke.header().precision().dependencies().status() == Evidence.PrecisionStatus.OPEN, "open dimensional claims");
        check(publication.uncertainties().stream().filter(u -> invoke.header().uncertainties().contains(u.id())).count() >= 5, "separate explicit uncertainties");
        check(result.statements().size() == input.statements().size(), "no lost statement correlation");
        check(result.statements().stream().anyMatch(l -> l.source().equals(call.header().id()) && l.target().equals(invoke.header().id()) && l.label().equals(first.label()) && l.origin().equals(invoke.header().origin())), "CALL StatementLink");
        check(result.statements().stream().anyMatch(l -> l.source().equals(call.normalContinuation().statement().orElseThrow()) && l.target().equals(next.terminator().header().id()) && l.label().equals(next.label())), "GOBACK identity is explicit normal successor");
        check(hasLocation(origins, next.origin(), call.normalContinuation().provenance().original()), "normal continuation evidence");
        for (int i = 0; i < values.size(); i++) {
            var assign = (Operations.Assign) first.instructions().get(i);
            check(assign.value() instanceof Expressions.Literal l && l.value().equals(new Values.TextValue(values.get(i))), "raw fitted/identity assigned text");
            var link = result.statements().stream().filter(s -> s.target().equals(assign.header().id())).findFirst().orElseThrow();
            var m = (SpInput.MoveFact) input.statements().stream().filter(s -> s.header().id().equals(link.source())).findFirst().orElseThrow();
            var data = result.data().stream().filter(d -> d.source().equals(m.target().binding().selected().orElseThrow())).findFirst().orElseThrow();
            check(assign.destination() instanceof Places.ObjectPlace p && p.object().equals(data.object()), "Assign destination follows its DataId");
            if (m.textAdjustment().isPresent()) {
                check(origins.get(assign.header().origin()) instanceof Origins.Derived, "fitted Assign derived, not written");
                check(origins.get(assign.value().header().origin()) instanceof Origins.Derived d && d.rule().contains("FITTED_TEXT"), "adjusted literal derivation");
                for (var p : List.of(m.header().provenance(), m.source().provenance(), m.target().provenance(), m.textAdjustment().orElseThrow().provenance()))
                    check(hasLocation(origins, assign.value().header().origin(), p.original()), "all fitted-value source evidence");
            }
        }
        check(unit.objects().size() == input.dataDeclarations().size() && publication.storage().size() == unit.objects().size(), "all DATA once; no fake object");
        check(publication.coverage().inventory() == Evidence.InventoryStatus.PARTIAL && unit.coverage().inventory() == Evidence.InventoryStatus.PARTIAL, "partial global coverage");
        check(publication.resources().isEmpty() && publication.artifactRelations().isEmpty(), "no dependency product");
        var validation = AirValidator.validate(publication);
        check(validation.status() == ValidationResult.Status.STRUCTURALLY_VALID, "STRUCTURALLY_VALID");
        check(validation.issues().stream().anyMatch(v -> v.kind() == ValidationIssue.Kind.SEMANTIC_OBLIGATION && v.rule().equals("I-56")), "I-56 retained");
        return invoke;
    }
    private static boolean hasLocation(Map<OriginId, Origins.Origin> origins, OriginId id, SpInput.Location location) {
        var todo = new ArrayDeque<OriginId>(); todo.add(id); var seen = new HashSet<OriginId>();
        while (!todo.isEmpty()) {
            var current = todo.removeFirst(); if (!seen.add(current)) continue;
            var origin = origins.get(current);
            if (origin instanceof Origins.Derived d) todo.addAll(d.inputs());
            if (origin instanceof Origins.Written w && w.location().orElse(null) instanceof Origins.LineColumns lc) {
                var span = lc.span();
                if (span.start().line().intValueExact() == location.startLine() && span.start().column().intValueExact() == location.startColumn()
                    && span.end().line().intValueExact() == location.endLine() && span.end().column().intValueExact() == location.endColumn()) return true;
            }
        }
        return false;
    }
}
