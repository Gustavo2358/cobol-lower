package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.lower.domain.SpInput;
import static io.github.gustavo2358.lower.domain.SpInput.*;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import com.dynatrace.hash4j.hashing.Hashing;
import com.dynatrace.hash4j.hashing.HashStream128;
import com.dynatrace.hash4j.hashing.HashValues;

/** Domain-separated, noncryptographic XXH3-128 of admitted canonical facts. */
final class CanonicalRevision {
    private static final String PREFIX = "minimal-entry-goback@1/AIR2/SP1.1/xxh3-128-v1/" + LocalIds.POLICY + "/";
    private final HashStream128 stream = Hashing.xxh3_128(0L).hashStream();
    private final byte[] buffer = new byte[256];
    private int used;
    private CanonicalRevision() { }

    static Optional<String> encode(SpInput input, int maximum) {
        // This bound applies only to the final ID, never to the streamed canonical facts.
        if (maximum < 32) return Optional.empty();
        var encoder = new CanonicalRevision();
        encoder.append(PREFIX); encoder.input(input);
        return Optional.of(encoder.finish());
    }
    static Optional<String> scalar(SpInput input, List<DataFact> data, List<MoveFact> moves, GobackFact terminal, int maximum) {
        if (maximum < 32) return Optional.empty();
        var e = new CanonicalRevision();
        e.append("scalar-text-move@1/AIR2/SP1.2/xxh3-128-v2/"); e.append(LocalIds.POLICY); e.append("/");
        e.unit(input.unit());
        e.list(data, d -> {
            e.word(d.id().handle()); e.word(d.canonicalName()); e.provenance(d.provenance()); e.symbol(d.coverage());
            var t = d.scalarText().orElseThrow();
            e.symbol(t.logicalDomain()); e.number(t.logicalExtent()); e.symbol(t.storageClass()); e.symbol(t.declarationScope());
        });
        e.list(moves, m -> {
            e.word("MOVE"); e.scalarHeader(m.header());
            var source = m.source(); e.word(source.id().handle());
            if (source instanceof LiteralSource literal) {
                e.symbol(literal.kind()); e.provenance(source.provenance());
                var value = literal.logicalValue().orElseThrow(); e.symbol(value.logicalDomain()); e.word(value.value()); e.number(value.logicalExtent());
            } else {
                var read = (DataReference) source; e.word("DATA"); e.symbol(read.role()); e.provenance(read.provenance());
                e.symbol(read.binding().status()); e.word(read.binding().selected().orElseThrow().handle());
                e.word(read.wholeItemAccess().orElseThrow().data().handle());
            }
            var target = m.target(); e.word(target.id().handle()); e.symbol(target.role()); e.provenance(target.provenance());
            e.symbol(target.binding().status()); e.word(target.binding().selected().orElseThrow().handle());
            e.word(target.wholeItemAccess().orElseThrow().data().handle()); e.symbol(m.copySemantics());
            e.symbol(m.normalContinuation().availability()); e.statementId(m.normalContinuation().statement().orElseThrow());
            e.provenance(m.normalContinuation().provenance());
            m.textAdjustment().ifPresent(a -> {
                e.word("FITTED_TEXT"); e.symbol(a.rule()); e.number(a.receiverExtent());
                e.word(a.result().value()); e.number(a.result().logicalExtent()); e.provenance(a.provenance());
            });
        });
        input.storageIndependence().filter(p -> p.availability() == Availability.KNOWN).ifPresent(proof -> {
            e.word("STORAGE_INDEPENDENCE"); e.symbol(proof.rule()); e.word(proof.authority());
            e.list(proof.members(), id -> e.word(id.handle())); e.provenance(proof.provenance().orElseThrow());
        });
        e.word("GOBACK"); e.scalarHeader(terminal.header()); e.symbol(terminal.exit()); e.symbol(terminal.localContinuation());
        var entry = input.entryInventory().entries().getFirst();
        e.word(entry.id().handle()); e.symbol(entry.role()); e.symbol(entry.availability()); e.provenance(entry.provenance()); e.symbol(entry.coverage());
        e.symbol(entry.start().availability()); e.statementId(entry.start().statement().orElseThrow());
        e.symbol(entry.signature().availability()); e.number(entry.signature().parameterCount().orElseThrow()); e.symbol(entry.signature().returningClause());
        e.symbol(input.entryInventory().status()); e.symbol(input.entryInventory().scope());
        e.list(input.entryInventory().gapCodes(), e::word);
        e.list(input.gaps(), g -> { e.statementId(g.statement()); e.symbol(g.scope()); e.word(g.code()); e.word(g.detail()); e.provenance(g.provenance()); });
        return Optional.of(e.finish());
    }
    static Optional<String> call(SpInput input, CallAdmission.Plan plan, int maximum) {
        if (maximum < 32) return Optional.empty();
        var e = new CanonicalRevision();
        e.word("cp6-call@1/AIR2/SP1.3/xxh3-128-v1/"); e.word(LocalIds.POLICY);
        // Domain-separated fixed-size fingerprint of the shared DATA/MOVE/entry/return facts.
        e.word(scalar(input, plan.data(), plan.moves(), plan.terminal().orElseThrow(), maximum).orElseThrow());
        callFacts(e, plan.call().orElseThrow());
        return Optional.of(e.finish());
    }
    static Optional<String> perform(SpInput input, PerformAdmission.Plan plan, int maximum) {
        if (maximum < 32) return Optional.empty();
        var e = new CanonicalRevision(); e.word("perform-basic@1/AIR2/SP1.6/xxh3-128-v1"); e.word(LocalIds.POLICY);
        e.word(call(input, new CallAdmission.Plan(plan.admission(), plan.data(), plan.moves(), plan.call(), plan.terminal()), maximum).orElseThrow());
        performFacts(e, plan.perform().orElseThrow());
        return Optional.of(e.finish());
    }
    static Optional<String> simpleIf(SpInput input, IfAdmission.Plan plan, int maximum) {
        if (maximum < 32) return Optional.empty();
        var e = new CanonicalRevision(); e.word("simple-if@1/AIR2/SP1.4/xxh3-128-v1"); e.word(LocalIds.POLICY);
        e.word(call(input, new CallAdmission.Plan(plan.admission(), plan.data(), plan.moves(), plan.call(), plan.terminal()), maximum).orElseThrow());
        ifFacts(e, plan.branch().orElseThrow(), plan.thenMoves(), plan.elseMoves());
        var proof = input.storageIndependence().orElseThrow(); e.symbol(proof.availability()); e.symbol(proof.rule()); e.word(proof.authority());
        e.list(proof.members(), id -> e.word(id.handle())); e.provenance(proof.provenance().orElseThrow()); e.list(proof.gapCodes(), e::word);
        return Optional.of(e.finish());
    }
    static Optional<String> supportedProgram(SpInput input, SupportedProgramAdmission.Plan plan, int maximum) {
        if (maximum < 32) return Optional.empty();
        var e = new CanonicalRevision(); e.word("supported-cp6-program@1/AIR2/SP1.7/xxh3-128-v1"); e.word(LocalIds.POLICY);
        e.word(scalar(input, plan.data(), plan.moves(), plan.terminal(), maximum).orElseThrow());
        for (var statement : plan.primary()) {
            e.scalarHeader(statement.header());
            if (statement instanceof CallFact call) callFacts(e, call);
            if (statement instanceof IfFact f) { var d = plan.diamonds().get(f.header().id()); ifFacts(e, f, d.thenMoves(), d.elseMoves()); }
            if (statement instanceof PerformFact p) performFacts(e, p);
        }
        return Optional.of(e.finish());
    }
    static Optional<String> partial(SpInput input, int maximum) {
        if (maximum < 32) return Optional.empty();
        var e = new CanonicalRevision(); e.word("compositional-program@1/AIR2/SP1.8/explicit-fields-v1");
        var canonical=new SpInput(input.unit(),input.policy(),ScalarDataOrder.canonical(input.dataDeclarations()),
            input.statements().stream().sorted(java.util.Comparator.comparingInt(s->s.header().programPoint())).toList(),
            input.structure(),input.gaps(),input.coverage(),input.entryInventory(),input.storageIndependence(),input.compositional());
        e.recordFact(canonical); return Optional.of(e.finish());
    }
    /** Identity only: structurally encode all immutable record components; never infer semantics from text. */
    private void recordFact(Object value) {
        if (value instanceof String s) { word("text"); word(s); }
        else if (value instanceof Enum<?> e) { word("enum"); word(e.getDeclaringClass().getName()); word(e.name()); }
        else if (value instanceof Integer n) { word("integer"); number(n); }
        else if (value instanceof Boolean b) { word("boolean"); flag(b); }
        else if (value instanceof Optional<?> o) { word("optional"); flag(o.isPresent()); o.ifPresent(this::recordFact); }
        else if (value instanceof List<?> l) { word("list"); number(l.size()); l.forEach(this::recordFact); }
        else PartialIdentityFacts.write(value,this::word,this::recordFact);
    }

    private static void callFacts(CanonicalRevision e, CallFact call) {
        e.scalarHeader(call.header()); e.symbol(call.syntax());
        e.word(call.target().id().handle()); e.provenance(call.target().provenance());
        switch (call.target()) {
            case LiteralCallTarget l -> {
                e.word("LITERAL"); e.word(l.text()); e.word(l.writtenText());
                var v = l.logicalValue().orElseThrow(); e.symbol(v.logicalDomain()); e.word(v.value()); e.number(v.logicalExtent());
            }
            case DataCallTarget d -> {
                e.word("DATA"); var r = d.reference(); e.symbol(r.role()); var b = r.binding(); e.symbol(b.status());
                e.list(b.candidates(), id -> e.word(id.handle())); e.optional(b.selected(), id -> e.word(id.handle()));
                e.optional(b.reason(), e::symbol); e.list(b.candidateNames(), e::word);
                e.word(r.wholeItemAccess().orElseThrow().data().handle());
            }
        }
        var next = call.normalContinuation(); e.symbol(next.availability()); e.statementId(next.statement().orElseThrow()); e.provenance(next.provenance());
        var surface = call.surface(); e.symbol(surface.using()); e.optional(surface.argumentCount(), e::number); e.symbol(surface.returning());
        e.symbol(surface.onException()); e.symbol(surface.notOnException()); e.symbol(surface.onOverflow());
        e.symbol(call.runtimeTarget()); e.word(call.runtimeUncertaintyCode()); e.symbol(call.effects()); e.symbol(call.outcomes());
    }
    private static void performFacts(CanonicalRevision e, PerformFact p) {
        e.scalarHeader(p.header()); e.symbol(p.profile());
        var t = p.target().orElseThrow(); e.word(t.id().handle()); e.provenance(t.referenceOrigin()); e.provenance(t.paragraphOrigin());
        e.statementId(p.targetEntry().orElseThrow()); e.list(p.targetStatements(), e::statementId); e.statementId(p.targetExit().orElseThrow());
        e.statementId(p.normalContinuation().statement().orElseThrow()); e.provenance(p.normalContinuation().provenance());
        e.list(p.primaryStatements(), e::statementId); e.list(p.gapCodes(), e::word);
    }
    private static void ifFacts(CanonicalRevision e, IfFact f, List<MoveFact> thenMoves, List<MoveFact> elseMoves) {
        e.scalarHeader(f.header()); e.word(f.conditionShape()); e.flag(f.explicitlyTerminated()); e.symbol(f.profile());
        var p = f.predicateGuarantee(); e.symbol(p.availability()); e.symbol(p.profile()); e.symbol(p.resultDomain());
        e.symbol(p.evaluation()); e.symbol(p.normalCompletion()); e.symbol(p.readsCompleteness()); e.symbol(p.truthValue());
        e.list(p.knownReads(), id -> e.word(id.handle())); e.provenance(p.provenance()); e.list(p.gapCodes(), e::word);
        e.provenance(f.conditionProvenance());
        e.list(f.conditionReads(), read -> {
            e.word(read.id().handle()); e.symbol(read.role()); e.provenance(read.provenance());
            e.word(read.wholeItemAccess().orElseThrow().data().handle());
        });
        e.ifArm(f.thenArm()); e.ifArm(f.elseArm());
        e.list(thenMoves, m -> e.statementId(m.header().id())); e.list(elseMoves, m -> e.statementId(m.header().id()));
        e.symbol(f.normalContinuation().availability()); e.statementId(f.normalContinuation().statement().orElseThrow()); e.provenance(f.normalContinuation().provenance());
    }
    private void ifArm(IfArm arm) {
        symbol(arm.presence()); symbol(arm.contentAvailability()); symbol(arm.entry().availability());
        optional(arm.entry().statement(), this::statementId); provenance(arm.provenance()); list(arm.gapCodes(), this::word);
    }
    private void scalarHeader(StatementHeader h) { statementId(h.id()); provenance(h.provenance()); symbol(h.coverage()); }
    private String finish() {
        flush();
        // Numeric high64 then low64, fixed 32 lowercase hex digits (not little-endian bytes).
        return HashValues.toHexString(stream.get());
    }
    static String local(String namespace, String role, String owner, String key) {
        var encoder = new CanonicalRevision();
        encoder.localFields(namespace, role, owner, key);
        return encoder.finish();
    }
    private void localFields(String namespace, String role, String owner, String key) {
        append("minimal-entry-goback@1/AIR2/"); append(LocalIds.POLICY); append("/");
        word(namespace); word(role); word(owner); word(key);
    }
    static String token(String value) {
        // SourceKeys retain canonical-v1 tokens. Derived AIR local IDs use local() instead.
        long required = Integer.toString(value.length()).length() + 1L + 4L * value.length();
        if (required > Integer.MAX_VALUE) throw new LimitReached();
        var text = new StringBuilder(); text.append(value.length()).append(':');
        for (int i = 0; i < value.length(); i++) {
            int code = value.charAt(i);
            for (int shift = 12; shift >= 0; shift -= 4) text.append("0123456789abcdef".charAt((code >>> shift) & 15));
        }
        return text.toString();
    }
    private void flush() {
        stream.putBytes(buffer, 0, used); used = 0;
    }
    private void ascii(char value) {
        buffer[used++] = (byte) value;
        if (used == buffer.length) flush();
    }
    private void append(String value) {
        for (int i = 0; i < value.length(); i++) ascii(value.charAt(i));
    }
    private void word(String value) {
        append(Integer.toString(value.length())); append(":");
        for (int i = 0; i < value.length(); i++) {
            int code = value.charAt(i);
            for (int shift = 12; shift >= 0; shift -= 4) ascii("0123456789abcdef".charAt((code >>> shift) & 15));
        }
    }
    private void number(int value) { word(Integer.toString(value)); }
    private void flag(boolean value) { word(Boolean.toString(value)); }
    private void symbol(Enum<?> value) { word(value.name()); }
    private <T> void list(List<T> values, Consumer<T> item) { number(values.size()); for (T value : values) item.accept(value); }
    private <T> void optional(Optional<T> value, Consumer<T> item) { flag(value.isPresent()); value.ifPresent(item); }
    private void unit(UnitKey value) { word(value.compilationUnitId()); list(value.structuralPath(), this::number); word(value.canonicalProgramName()); }
    private void statementId(StatementId value) { unit(value.unit()); word(value.handle()); }
    private void location(Location value) { word(value.file()); number(value.startLine()); number(value.startColumn()); number(value.endLine()); number(value.endColumn()); }
    private void provenance(Provenance value) {
        location(value.expanded()); location(value.original());
        list(value.includeChain(), frame -> { word(frame.includingFile()); word(frame.requestedName()); word(frame.includedFile()); number(frame.includeLine()); });
        flag(value.exact());
    }
    private void readiness(Readiness value) { claim(value.lowering()); claim(value.cfg()); claim(value.effectsDataflow()); }
    private void claim(ReadinessClaim value) { symbol(value.status()); word(value.scope()); }
    private void entryGap(EntryGap gap) { symbol(gap.scope()); word(gap.code()); word(gap.detail()); provenance(gap.provenance()); }
    private void input(SpInput input) {
        unit(input.unit()); var policy = input.policy();
        word(policy.policyId()); word(policy.version()); symbol(policy.qualifyMode()); symbol(policy.pgmnameMode()); symbol(policy.dynamMode()); symbol(policy.dllMode());
        list(input.dataDeclarations(), data -> {
            unit(data.id().unit()); word(data.id().handle()); word(data.canonicalName()); optional(data.picture(), this::word);
            provenance(data.provenance()); symbol(data.coverage()); readiness(data.readiness());
        });
        list(input.statements(), statement -> {
            var h = statement.header(); statementId(h.id()); number(h.programPoint());
            optional(h.containment().parent(), this::statementId); symbol(h.containment().branch());
            provenance(h.provenance()); symbol(h.coverage()); readiness(h.readiness());
            if (!(statement instanceof GobackFact goback)) throw new IllegalArgumentException("canonical revision requires admitted GOBACK input");
            word("GOBACK"); symbol(goback.exit()); symbol(goback.localContinuation());
        });
        list(input.structure().roots(), this::statementId);
        list(input.structure().branches(), branch -> { statementId(branch.parent()); symbol(branch.branch()); list(branch.children(), this::statementId); });
        list(input.gaps(), gap -> { statementId(gap.statement()); symbol(gap.scope()); word(gap.code()); word(gap.detail()); provenance(gap.provenance()); });
        var coverage = input.coverage(); symbol(coverage.inventoryStatus()); number(coverage.observedStatements()); number(coverage.modeledStatements());
        number(coverage.partialStatements()); number(coverage.unsupportedStatements()); number(coverage.inputMissingStatements()); readiness(coverage.readiness());
        var inventory = input.entryInventory(); symbol(inventory.status()); symbol(inventory.scope());
        list(inventory.entries(), entry -> {
            unit(entry.id().unit()); word(entry.id().handle()); symbol(entry.role()); symbol(entry.availability());
            symbol(entry.start().availability()); optional(entry.start().statement(), this::statementId);
            symbol(entry.signature().availability()); optional(entry.signature().parameterCount(), this::number); symbol(entry.signature().returningClause());
            provenance(entry.provenance()); symbol(entry.coverage()); readiness(entry.readiness()); list(entry.gaps(), this::entryGap);
        });
        list(inventory.gapCodes(), this::word);
    }
    private static final class LimitReached extends RuntimeException { private static final long serialVersionUID = 1L; }
}
