package io.github.gustavo2358.lower.testing;

/** Fixed reviewed fast contract profile; scale and cumulative suites are local only. */
public final class FastSuite {
    private FastSuite() { }
    public static void main(String[] args) {
        int n = InputSuite.run() + LoweringSuite.run() + ScalarSuite.run() + CallSuite.run();
        if (n <= 0) throw new AssertionError("fast suite must execute assertions");
        System.out.println("LOWER_FAST_CORE_TESTS=" + n);
    }
}
