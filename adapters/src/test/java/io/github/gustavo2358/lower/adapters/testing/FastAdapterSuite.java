package io.github.gustavo2358.lower.adapters.testing;

import io.github.gustavo2358.lower.adapters.sp.ObservedShapeSuite;
import java.util.List;

/** Fixed focal wire/codec contracts, without historical or mutation campaign entrypoints. */
public final class FastAdapterSuite {
    private FastAdapterSuite() { }
    public static void main(String[] args) throws Exception {
        FileDeclarationSuite.main(new String[0]);
        FileStaticSliceSuite.main(new String[0]); FileNativeOperationSuite.main(new String[0]); FileMemoryEffectsSuite.main(new String[0]); FileControlSuite.main(new String[0]); FileSortSuite.main(new String[0]); FileAuxiliarySuite.main(new String[0]);
        ObservedShapeSuite.main(new String[0]);
        DisplayEffectsSuite.main(new String[0]); InitializeEffectsSuite.main(new String[0]);
        DeclarativeValueSuite.main(new String[0]);
        EvidencePreservingEntrySuite.main(new String[0]);
        CicsProgramControlSuite.main(new String[0]);
        int cases = 0;
        for (String name : List.of("dynamic-x8", "literal")) {
            try (var stream = FastAdapterSuite.class.getResourceAsStream("/sp/cp6/" + name + ".json")) {
                if (stream == null) throw new AssertionError("missing W1 fixture");
                boolean computed = name.equals("dynamic-x8");
                CallIntegrationSuite.positive(stream.readAllBytes(), computed ? List.of("PROGA   ") : List.of(), computed, "PROGA");
                cases++;
            }
        }
        if (cases != 2) throw new AssertionError("fixed W1 focal cases absent");
        RegionalStorageIntegrationSuite.run(); RegionalTranslationSuite.run();
        OverlayStorageIntegrationSuite.run(); ScopedStorageSuite.run(); AmbiguousTargetSuite.run();
        RenamesStorageSuite.main(new String[0]); SliceStorageSuite.main(new String[0]); MoveSequenceStorageSuite.main(new String[0]); CorrespondingStorageSuite.main(new String[0]); InitialStorageSuite.main(new String[0]); PossibleEntrySuite.main(new String[0]); MixedInitialStorageSuite.main(new String[0]); UnknownEntryBoundsSuite.main(new String[0]);
        EntryLocalizationSuite.run();
        IfIntegrationSuite.focal();
        MoveDataIntegrationSuite.run(); PerformIntegrationSuite.run(); MultiCallIntegrationSuite.run(); EvaluateIntegrationSuite.run(); GoToIntegrationSuite.run(); ConditionalGoToIntegrationSuite.run(); PerformFamilyIntegrationSuite.run(); PerformUntilIntegrationSuite.run(); PerformTimesIntegrationSuite.run(); PerformVaryingIntegrationSuite.run(); PartialIntegrationSuite.run();
        System.out.println("LOWER_FAST_ADAPTER_CASES=" + (cases + 2));
    }
}
