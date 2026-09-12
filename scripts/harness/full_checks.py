"""Technical performance suite result check."""
import re

def performance_counts(output):
    counts = [int(n) for n in re.findall(r"^LOWER_PERFORMANCE_TESTS=([0-9]+)$", output, re.M)]
    if len(counts) != 2 or min(counts) <= 0:
        raise RuntimeError("performance tests absent/zero")
    return sum(counts)
