"""Reconcile observed merge without inventing review or rewriting frozen evidence."""
from pathlib import Path
import sys
import unittest
from unittest.mock import patch
ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / 'scripts/harness'))
from checks import historical_link, history_errors, read_data
from run import reconcile_errors

class OutputLifecycle(unittest.TestCase):
    def record(self):
        registry = read_data(ROOT / 'docs/work/registry.json')
        return registry, next(r for r in registry['history'] if r['id'] == 'WORK-LOWER-002')

    def test_actual_merge_without_recorded_review(self):
        registry, item = self.record()
        self.assertEqual([], history_errors(ROOT, registry)[0])
        self.assertIsNone(read_data(ROOT / item['manifest'])['git']['pull_request'])
        self.assertEqual(3, read_data(ROOT / item['evidence'])['pull_request'])

    def test_absent_review_needs_complete_honest_observation(self):
        for field, value in [('merge_commit', None), ('head_sha', 'bad'), ('review_url', 'https://invented.invalid'), ('reviews', [{}]), ('review_commit', 'a'*40)]:
            registry, item = self.record()
            item['remote_observation'][field] = value
            self.assertTrue(history_errors(ROOT, registry)[0], field)

    def test_remote_observation_is_checked_against_actual_metadata(self):
        _, item = self.record()
        pr = dict(state='MERGED', headRefOid=item['remote_observation']['head_sha'], mergeCommit={'oid':item['remote_observation']['merge_commit']})
        self.assertEqual([], reconcile_errors(item, pr, []))
        self.assertTrue(reconcile_errors(item, pr, [dict(state='COMMENTED')]))
        pr['mergeCommit']['oid'] = 'a'*40
        self.assertTrue(reconcile_errors(item, pr, []))

class HistoricalLinks(unittest.TestCase):
    def test_missing_current_link_resolves_only_unchanged_historical_evidence(self):
        review = ROOT / 'docs/quality/WORK-LOWER-002/CP0-review.md'
        target = ROOT / 'docs/work/active/WORK-LOWER-002/spec.md'
        self.assertFalse(target.exists())
        self.assertTrue(historical_link(ROOT, review, target, ''))
        self.assertFalse(historical_link(ROOT, review, target, 'nonexistent-anchor'))
        self.assertFalse(historical_link(ROOT, ROOT / 'README.md', target, ''))
        with patch('git_checks.git', return_value=b'changed historical evidence'):
            self.assertFalse(historical_link(ROOT, review, target, ''))

if __name__ == '__main__': unittest.main()
