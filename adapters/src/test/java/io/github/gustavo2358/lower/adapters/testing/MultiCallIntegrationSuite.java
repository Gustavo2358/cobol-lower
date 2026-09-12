package io.github.gustavo2358.lower.adapters.testing;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.SpInput;
import io.github.gustavo2358.lower.testing.IfInputs;
import java.util.*;
import java.util.stream.Collectors;
import static io.github.gustavo2358.lower.testing.CallOracle.check;

/** Real SP plus an independent relational oracle over every source statement. */
public final class MultiCallIntegrationSuite {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final SpJsonDecoder DECODER = new SpJsonDecoder(CobolLower.INPUT_LIMITS);
    public static ObjectNode fixture(int n) throws Exception {
        try (var in = MultiCallIntegrationSuite.class.getResourceAsStream("/sp/multi-call/fixture-" + n + ".json")) {
            return (ObjectNode) JSON.readTree(Objects.requireNonNull(in));
        }
    }
    private static SpInput input(ObjectNode wire) throws Exception {
        var d = DECODER.decode(JSON.writeValueAsBytes(wire)); check(d instanceof SpJsonDecoder.Decoded, "SP1.7 decode: " + d);
        return ((SpJsonDecoder.Decoded) d).input();
    }
    static LoweringResult oracle(SpInput input) {
        var result = new CobolLowerer().lower(input, CobolLower.OPTIONS);
        check(result.status() == LoweringResult.Status.SUCCESS, "supported program: " + result.status() + result.admission().diagnostics());
        var unit = result.publication().orElseThrow().units().getFirst();
        var links = result.statements().stream().collect(Collectors.toMap(LoweringResult.StatementLink::source, l -> l));
        var seq = unit.sequences().stream().collect(Collectors.toMap(Sequence::label, s -> s));
        check(links.size() == input.statements().size(), "every observed statement has one distinct operation link");
        check(result.statements().stream().map(LoweringResult.StatementLink::target).distinct().count() == links.size(), "distinct OperationIds");
        check(unit.entries().getFirst().initialLabel().equals(Optional.of(links.get(input.entryInventory().entries().getFirst().start().statement().orElseThrow()).label())), "explicit entry");
        int calls = 0;
        for (var s : input.statements()) {
            var link = links.get(s.header().id()); var sequence = seq.get(link.label());
            var ops = new ArrayList<Operation>(sequence.instructions()); ops.add(sequence.terminator());
            var op = ops.stream().filter(o -> o.header().id().equals(link.target())).findFirst().orElseThrow();
            if (s instanceof SpInput.MoveFact m) {
                check(op instanceof Operations.Assign && ops.indexOf(op) < sequence.instructions().size(), "MOVE remains instruction");
                var next = links.get(m.normalContinuation().statement().orElseThrow());
                if (next.label().equals(link.label())) check(ops.get(ops.indexOf(op) + 1).header().id().equals(next.target()), "MOVE explicit next operation");
                else check(sequence.terminator() instanceof Operations.Jump j && j.destination().equals(next.label()), "arm/body MOVE closes at explicit join/resume");
            } else if (s instanceof SpInput.CallFact c) {
                calls++; check(op instanceof Operations.Invoke && sequence.terminator() == op, "CALL is its sequence terminator");
                var i = (Operations.Invoke) op;
                check(i.outcomes().known().equals(List.of(new Control.Normal(links.get(c.normalContinuation().statement().orElseThrow()).label()))), "CALL explicit continuation");
                if (c.target() instanceof SpInput.LiteralCallTarget l) check(i.target() instanceof Interactions.LiteralTarget t && t.name().equals(l.logicalValue().orElseThrow().value()), "literal retained");
                else {
                    var id = ((SpInput.DataCallTarget)c.target()).reference().binding().selected().orElseThrow();
                    var object = result.data().stream().filter(d -> d.source().equals(id)).findFirst().orElseThrow().object();
                    check(i.target() instanceof Interactions.ComputedTarget t && t.name() instanceof Expressions.Read read && read.place() instanceof Places.ObjectPlace place && place.object().equals(object), "site-specific target object");
                }
            } else if (s instanceof SpInput.IfFact f) {
                check(op instanceof Operations.Branch, "IF branch"); var b = (Operations.Branch) op;
                check(b.trueDestination().equals(links.get(f.thenArm().entry().statement().orElseThrow()).label()), "true arm identity");
                var falseId = f.elseArm().entry().statement().orElse(f.normalContinuation().statement().orElseThrow());
                check(b.falseDestination().equals(links.get(falseId).label()), "false arm identity");
            } else if (s instanceof SpInput.PerformFact p) {
                check(op instanceof Operations.Jump j && j.destination().equals(links.get(p.targetEntry().orElseThrow()).label()), "PERFORM target visited");
                var target = seq.get(links.get(p.targetEntry().orElseThrow()).label());
                check(target.instructions().stream().map(i -> i.header().id()).toList().equals(p.targetStatements().stream().map(id -> links.get(id).target()).toList()), "complete MOVE target body");
                check(target.terminator() instanceof Operations.Jump j && j.destination().equals(links.get(p.normalContinuation().statement().orElseThrow()).label()), "unique arbitrary supported resume");
            } else check(op instanceof Operations.Return, "GOBACK return");
        }
        check(calls == unit.sequences().stream().filter(s -> s.terminator() instanceof Operations.Invoke).count(), "all CALLs, no PERFORM Invoke");
        return result;
    }
    private static void reject(ObjectNode wire, String why) throws Exception {
        var d = DECODER.decode(JSON.writeValueAsBytes(wire));
        check(d instanceof SpJsonDecoder.Rejected || new CobolLowerer().lower(((SpJsonDecoder.Decoded)d).input(), CobolLower.OPTIONS).publication().isEmpty(), "refuse " + why);
    }
    private static void unboundedCallComposition() {
        long previous = 0;
        for (int count : List.of(64, 128)) {
            var base = io.github.gustavo2358.lower.testing.CallInputs.create(0, 0, 5, "PROGA", true);
            var call = (SpInput.CallFact) base.statements().getFirst();
            var terminal = (SpInput.GobackFact) base.statements().getLast();
            var facts = new ArrayList<SpInput.StatementFact>();
            for (int n = 0; n < count; n++) {
                var id = new SpInput.StatementId(base.unit(), "statement:" + n);
                var header = IfInputs.with(IfInputs.with(call.header(), "id", id), "programPoint", n);
                var target = IfInputs.with((SpInput.LiteralCallTarget)call.target(), "id", new SpInput.OperandId(id, "operand:" + n + ":0"));
                var next = IfInputs.with(call.normalContinuation(), "statement", Optional.of(new SpInput.StatementId(base.unit(), "statement:" + (n + 1))));
                facts.add(IfInputs.with(IfInputs.with(IfInputs.with(call, "header", header), "target", target), "normalContinuation", next));
            }
            var terminalId = new SpInput.StatementId(base.unit(), "statement:" + count);
            facts.add(IfInputs.with(terminal, "header", IfInputs.with(IfInputs.with(terminal.header(), "id", terminalId), "programPoint", count)));
            var input = io.github.gustavo2358.lower.testing.ScalarInputs.replace(base, base.dataDeclarations(), facts);
            var result = oracle(input); long visits = result.admission().statistics().entitiesVisited();
            if (previous != 0) check(visits <= previous * 2 + 20, "linear admission work for twice as many CALL sites");
            previous = visits;
        }
    }
    public static void run() throws Exception {
        var codec = new AirJson();
        for (int n = 1; n <= 7; n++) {
            var wire = fixture(n); var input = input(wire); var result = oracle(input);
            var statements = new ArrayList<>(input.statements()); Collections.reverse(statements);
            var reversed = IfInputs.with(input, "statements", statements);
            check(Arrays.equals(codec.encode(result.publication().orElseThrow()), codec.encode(oracle(reversed).publication().orElseThrow())), "physical SP inventory cannot select flow");
            if (n >= 4 && n <= 6) reject(wire.deepCopy().put("contractVersion", "1.6.0"), "new composition cannot be relabeled SP1.6");
        }
        for (String mutation : List.of("second-perform", "wrong-resume", "body-is-primary", "cycle", "missing-call", "arm-entry", "unknown-statement")) {
            var wire = fixture(5); var facts = (ArrayNode) wire.path("statements");
            ObjectNode perform = null, call = null, branch = null;
            for (var s : facts) { if (s.path("variant").asText().equals("PERFORM")) perform=(ObjectNode)s; if (s.path("variant").asText().equals("CALL")) call=(ObjectNode)s; if(s.path("variant").asText().equals("IF"))branch=(ObjectNode)s; }
            switch (mutation) {
                case "second-perform" -> facts.add(Objects.requireNonNull(perform).deepCopy());
                case "wrong-resume" -> ((ObjectNode)perform.path("normalContinuation")).put("statement", perform.path("targetEntry").asText());
                case "body-is-primary" -> ((ArrayNode)perform.path("targetStatements")).add(call.path("header").path("id").asText());
                case "cycle" -> ((ObjectNode)call.path("normalContinuation")).put("statement", call.path("header").path("id").asText());
                case "missing-call" -> { for (int i=0;i<facts.size();i++) if (facts.get(i)==call) { facts.remove(i); break; } }
                case "arm-entry" -> ((ObjectNode)branch.at("/thenArm/entry")).put("statement", call.path("header").path("id").asText());
                case "unknown-statement" -> call.put("variant", "EVALUATE");
            }
            reject(wire, mutation);
        }
        unboundedCallComposition();
        System.out.println("LOWER_MULTI_CALL_INTEGRATION=PASS");
    }
    public static void main(String[] args) throws Exception { run(); }
}
