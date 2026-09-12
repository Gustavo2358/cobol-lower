import sys
from pathlib import Path
import unittest
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from full_checks import performance_counts

class PerformanceResult(unittest.TestCase):
    def test_nonzero_complete_suites(self):
        self.assertEqual(40, performance_counts('LOWER_PERFORMANCE_TESTS=17\nLOWER_PERFORMANCE_TESTS=23\n'))
    def test_missing_zero_or_incomplete_suites(self):
        for output in ('BUILD SUCCESS', 'LOWER_PERFORMANCE_TESTS=0\nLOWER_PERFORMANCE_TESTS=2\n', 'LOWER_PERFORMANCE_TESTS=1\n'):
            with self.assertRaises(RuntimeError):
                performance_counts(output)
