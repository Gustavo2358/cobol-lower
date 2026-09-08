package io.github.gustavo2358.lower.adapters.cli;

import io.github.gustavo2358.air.validation.ValidationOptions;
import io.github.gustavo2358.air.json.AirJsonException;
import io.github.gustavo2358.lower.adapters.air.AirFileOutput;
import io.github.gustavo2358.lower.adapters.sp.FileLowering;
import io.github.gustavo2358.lower.adapters.sp.SpFileInput;
import io.github.gustavo2358.lower.adapters.sp.SpJsonDecoder;
import io.github.gustavo2358.lower.application.AdmitInput;
import io.github.gustavo2358.lower.application.CobolLowerer;
import io.github.gustavo2358.lower.application.LowerInput;
import io.github.gustavo2358.lower.application.LoweringResult;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/** File composition for the admitted CP3 and scalar MOVE profiles. Exit codes are part of the CLI contract. */
public final class CobolLower {
    public static final int SUCCESS = 0, USAGE = 2, INPUT = 3, LOWERING = 4, CODEC = 5, OUTPUT = 6;
    public static final SpJsonDecoder.Limits INPUT_LIMITS = new SpJsonDecoder.Limits(ProductionLimits.SP_BYTES, ProductionLimits.SP_DEPTH, ProductionLimits.SP_NODES);
    public static final LowerInput.Options OPTIONS = new LowerInput.Options(new AdmitInput.Limits(ProductionLimits.ADMISSION_ENTITIES, 100),
            1_000_000, ValidationOptions.defaults());
    private CobolLower() { }
    public static void main(String[] args) { System.exit(run(args, System.err)); }
    public static int run(String[] args, PrintStream err) {
        return run(args, err, new FileLowering(new SpFileInput(INPUT_LIMITS), new CobolLowerer()), OPTIONS, new AirFileOutput());
    }
    /** Composition seam; no JVM exit, alternate semantic decoder or lowering. */
    public static int run(String[] args, PrintStream err, FileLowering lowering, LowerInput.Options options, AirFileOutput output) {
        if (args.length != 2 || args[0].isEmpty() || args[1].isEmpty()) return usage(err);
        Path input, destination;
        try { input = Path.of(args[0]); destination = Path.of(args[1]); }
        catch (InvalidPathException ex) { return usage(err); }
        var physical = lowering.lower(input, options);
        if (physical instanceof FileLowering.PhysicalFailure failure) {
            var diagnostic = failure.diagnostic();
            err.println("SP " + diagnostic.code() + " " + diagnostic.phase() + " " + diagnostic.location());
            return INPUT;
        }
        var result = ((FileLowering.Lowered) physical).result();
        if (result.status() != LoweringResult.Status.SUCCESS || result.publication().isEmpty()) {
            err.println("Lowering " + result.status());
            for (var diagnostic : result.admission().diagnostics())
                err.println(diagnostic.rule() + " " + diagnostic.subject() + ": " + diagnostic.requirement());
            return LOWERING;
        }
        try {
            output.write(result.publication().orElseThrow(), destination);
        } catch (AirJsonException ex) {
            err.println("AIR codec " + ex.code() + " " + ex.path() + ": " + ex.getMessage());
            return CODEC;
        } catch (IOException ex) {
            err.println("AIR output " + destination + ": " + ex.getMessage());
            return OUTPUT;
        }
        return SUCCESS;
    }
    private static int usage(PrintStream err) {
        err.println("Usage: cobol-lower <semantic-product.json> <air.json>");
        return USAGE;
    }
}
