package io.github.gustavo2358.lower.testing;

import io.github.gustavo2358.lower.application.*;

/** Counts derived from the frozen structural ledger; elapsed time is telemetry, never an oracle. */
public final class PerformanceSuite {
    private static int count;
    private PerformanceSuite() { }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError("CP4 performance " + message); count++; }
    public static int run() {
        for (int n : new int[] {1, 64, 128, 1024, 2048}) {
            var input = ScaleInputs.create(n); var port = new EntryGobackAdmission();
            var limits = new AdmitInput.Limits(1_000_000, 100); long started = System.nanoTime();
            var result = port.admit(input, limits); long elapsed = System.nanoTime() - started;
            check(result.status() == (n == 1 ? Admission.Status.ADMITTED : Admission.Status.UNSUPPORTED_SLICE), "plural inventory is unsupported, not a prefix");
            check(result.input().orElseThrow().equals(input) && result.input().orElseThrow().statements().size() == n, "entire observed inventory retained");
            var stats = result.statistics();
            check(stats.entitiesVisited() == 7L * n + 9, "entity ledger 7N+9");
            check(stats.referencesChecked() == n + 1L, "reference ledger N+1, no repeated scan");
            check(stats.provenanceComponents() == 3L * n + 3, "provenance ledger 3N+3");
            check(!result.diagnosticsTruncated() && result.diagnostics().size() == (n == 1 ? 0 : 1), "shape diagnostic does not hide occurrences");
            var exact = port.admit(input, new AdmitInput.Limits(7L * n + 9, 100));
            check(exact.equals(result), "exact entity boundary preserves result");
            var limited = port.admit(input, new AdmitInput.Limits(7L * n + 8, 100));
            check(limited.status() == Admission.Status.IMPLEMENTATION_LIMIT && limited.input().orElseThrow().equals(input), "entity limit cannot return partial success");
            var lowered = new EntryGobackLowerer().lower(input, LoweringSuite.OPTIONS);
            check(n == 1 ? lowered.status() == LoweringResult.Status.SUCCESS
                    : lowered.status() == LoweringResult.Status.UNSUPPORTED_SLICE && lowered.publication().isEmpty(), "scale never expands AIR profile");
            System.out.println("PERFORMANCE_CORE n=" + n + " visits=" + stats.entitiesVisited() + " references=" + stats.referencesChecked()
                    + " provenance=" + stats.provenanceComponents() + " elapsed_ns=" + elapsed + " java=" + System.getProperty("java.version")
                    + " os=" + System.getProperty("os.name") + " heap_used=" + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
        }
        return count;
    }
}
