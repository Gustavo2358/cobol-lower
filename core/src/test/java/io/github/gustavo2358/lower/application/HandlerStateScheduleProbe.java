package io.github.gustavo2358.lower.application;
import io.github.gustavo2358.lower.domain.SpInput;
/** Test-only schedule variation. Input has already passed admission's factual validation. */
public final class HandlerStateScheduleProbe {
    private HandlerStateScheduleProbe() { }
    public static boolean sourceOrderInvariant(SpInput input) { return new HandlerStateAnalyzer(input,false,true).analyze().equals(new HandlerStateAnalyzer(input,true,true).analyze()); }
    public static HandlerStateAnalysis reverse(SpInput input) { return new HandlerStateAnalyzer(input,true).analyze(); }
}
