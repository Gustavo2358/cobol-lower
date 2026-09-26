package io.github.gustavo2358.lower.adapters.cli;

import java.io.*;
import java.nio.file.*;
import io.github.gustavo2358.lower.adapters.sp.*;
import io.github.gustavo2358.lower.adapters.source.*;
import io.github.gustavo2358.lower.application.*;

/** Normal dependency product input: the existing AIR, R9 certificate and typed statement links. */
public final class CobolDependencyInput {
    private CobolDependencyInput() { }
    public static void main(String[] args){System.exit(run(args,System.err));}
    public static int run(String[] args,PrintStream err) {
        if(args.length!=2||args[0].isBlank()||args[1].isBlank()){err.println("Usage: cobol-dependency-input <semantic-product.json> <dependency-input.json>");return CobolLower.USAGE;}
        try {
            var input=Path.of(args[0]).toAbsolutePath().normalize();var destination=Path.of(args[1]).toAbsolutePath().normalize();
            if(input.equals(destination))return CobolLower.USAGE;
            var physical=new FileLowering(new SpFileInput(CobolLower.INPUT_LIMITS),new CobolLowerer()).lower(input,CobolLower.POSITIVE_OPTIONS,true);
            if(physical instanceof FileLowering.PhysicalFailure failure){err.println("SP "+failure.diagnostic().code());return CobolLower.INPUT;}
            var lowered=(FileLowering.Lowered)physical;var result=lowered.result();
            if(result.publication().isEmpty()){err.println("Lowering "+result.status());return CobolLower.LOWERING;}
            new DependencyInputFileOutput().write(lowered,CobolLower.POSITIVE_OPTIONS,destination);return CobolLower.SUCCESS;
        } catch(IOException|IllegalArgumentException failure){err.println("DEPENDENCY_INPUT_OUTPUT: "+failure.getMessage());return CobolLower.OUTPUT;}
    }
}
