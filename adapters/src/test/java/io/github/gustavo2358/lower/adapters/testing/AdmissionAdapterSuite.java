package io.github.gustavo2358.lower.adapters.testing;

import io.github.gustavo2358.lower.adapters.sp.FileAdmission;
import io.github.gustavo2358.lower.adapters.sp.SpFileInput;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.Admission;
import io.github.gustavo2358.lower.application.AdmitInput;
import io.github.gustavo2358.lower.application.EntryGobackAdmission;
import io.github.gustavo2358.lower.testing.SpFixtures;
import io.github.gustavo2358.lower.domain.SpInput;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;
import static io.github.gustavo2358.lower.testing.SpFixtures.*;

public final class AdmissionAdapterSuite {
    private static int count;
    private AdmissionAdapterSuite() { }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); count++; }
    public static int run(byte[] golden) throws Exception {
        var reader = new SpFileInput(new SpJsonDecoder.Limits(100_000, 64, 50_000));
        var port = new EntryGobackAdmission(); var limits = new AdmitInput.Limits(100_000, 100);
        int[] calls = {0};
        var driver = new FileAdmission(reader, (input, operationalLimits) -> { calls[0]++; return port.admit(input, operationalLimits); });
        var file = Files.createTempFile("lower-admission-", ".json");
        try {
            Files.write(file, golden);
            var fromFile = driver.admit(file, limits);
            check(fromFile instanceof FileAdmission.Checked, "file reaches typed port");
            var actual = ((FileAdmission.Checked)fromFile).admission();
            var independent = SpFixtures.minimal();
            check(actual.input().orElseThrow().equals(independent), "golden and independent handwritten memory input agree in all facts");
            check(actual.equals(port.admit(independent, limits)) && actual.status() == Admission.Status.ADMITTED, "same admission observation for file and independent memory");
            Files.writeString(file, new String(golden, StandardCharsets.UTF_8).replace("\"statement\":\"statement:0\"", "\"statement\":\"statement:999\""));
            var entry = independent.entryInventory().entries().get(0);
            var bad = entry(independent, entryFacts(entry, new SpInput.ExecutableStart(SpInput.Availability.KNOWN, Optional.of(new SpInput.StatementId(independent.unit(), "statement:999"))), entry.signature(), entry.coverage(), entry.readiness(), entry.gaps()));
            var rejected = ((FileAdmission.Checked)driver.admit(file, limits)).admission();
            check(rejected.equals(port.admit(bad, limits)) && rejected.status() == Admission.Status.INVALID_INPUT, "file decoder does not repair dangling start; same semantic diagnostics");
            check(calls[0] == 2, "both physical successes call same inner port once");
            Files.writeString(file, "{");
            check(driver.admit(file, limits) instanceof FileAdmission.PhysicalFailure && calls[0] == 2, "physical failure never calls semantic port");
            Files.write(file, golden);
            var limited = new SpFileInput(new SpJsonDecoder.Limits(golden.length - 1, 64, 50_000)).read(file);
            check(limited instanceof SpJsonDecoder.Rejected failure && failure.diagnostic().code() == SpJsonDecoder.Code.IMPLEMENTATION_LIMIT, "oversized file never truncates to success");
            check(new SpFileInput(new SpJsonDecoder.Limits(golden.length, 64, 50_000)).read(file) instanceof SpJsonDecoder.Decoded, "exact byte boundary accepted");
            Files.delete(file);
            check(reader.read(file) instanceof SpJsonDecoder.Rejected, "I/O failure explicit");
            check(actual.input().orElseThrow().equals(independent), "retained snapshot independent of closed/deleted resource");
        } finally { Files.deleteIfExists(file); }
        return count;
    }
}
