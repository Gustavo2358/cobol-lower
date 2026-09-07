package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.lower.domain.SpInput;
import static io.github.gustavo2358.lower.domain.SpInput.*;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/** Injective, bounded canonical revision encoding; not a digest and not a transport format. */
final class CanonicalRevision {
    private static final String PREFIX = "minimal-entry-goback@1/AIR2/SP1.1/canonical-v1/";
    private final StringBuilder text = new StringBuilder();
    private final int maximum;
    private CanonicalRevision(int maximum) { this.maximum = maximum; }

    static Optional<String> encode(SpInput input, int maximum) {
        var encoder = new CanonicalRevision(maximum);
        try { encoder.append(PREFIX); encoder.input(input); return Optional.of(encoder.text.toString()); }
        catch (LimitReached limit) { return Optional.empty(); }
    }
    static String token(String value) {
        var encoder = new CanonicalRevision(Integer.MAX_VALUE); encoder.word(value); return encoder.text.toString();
    }
    private void append(String value) {
        if (value.length() > maximum - text.length()) throw new LimitReached();
        text.append(value);
    }
    private void word(String value) {
        long required = Integer.toString(value.length()).length() + 1L + 4L * value.length();
        if (required > maximum - text.length()) throw new LimitReached();
        append(Integer.toString(value.length())); append(":");
        for (int i = 0; i < value.length(); i++) {
            int code = value.charAt(i);
            for (int shift = 12; shift >= 0; shift -= 4) text.append("0123456789abcdef".charAt((code >>> shift) & 15));
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
