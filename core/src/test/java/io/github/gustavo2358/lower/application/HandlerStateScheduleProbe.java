package io.github.gustavo2358.lower.application;
import io.github.gustavo2358.lower.domain.SpInput;
/** Test-only schedule variation. Input has already passed admission's factual validation. */
public final class HandlerStateScheduleProbe {
    private HandlerStateScheduleProbe() { }
    public static HandlerStateAnalysis reverse(SpInput input) { return new HandlerStateAnalyzer(input,true).analyze(); }
}
