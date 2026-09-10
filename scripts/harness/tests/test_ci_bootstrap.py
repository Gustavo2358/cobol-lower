"""Initial CP0 -> PR -> merge: real Git graph/trailer/bytes, controlled GitHub metadata."""
import copy
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / 'scripts/harness'))
import run as harness

REPOSITORY = 'example/lower'
BRANCH = 'fix/initial-cp0'


class MergeFixture:
    def __init__(self, root, record_changes=None):
        self.root = root
        self.git('init', '-q', '--initial-branch=main')
        self.git('config', 'user.name', 'CI fixture')
        self.git('config', 'user.email', 'ci-fixture@example.invalid')
        (root / 'README.md').write_text('Synthetic Git history for CI selection only.\n')
        self.git('add', 'README.md')
        self.git('commit', '-qm', 'base')
        self.git('checkout', '-qb', BRANCH)
        self.record = dict(repository=REPOSITORY, work_item='WORK-LOWER-999', checkpoint='CP0',
                           branch=BRANCH, pull_request=None)
        self.record.update(record_changes or {})
        self.path = 'docs/quality/WORK-LOWER-999/' + self.record['checkpoint'] + '.json'
        self.file = root / self.path
        self.file.parent.mkdir(parents=True)
        self.file.write_text(json.dumps(self.record) + '\n')
        self.git('add', self.path)
        self.message = 'checkpoint\n\nCheckpoint-Evidence: ' + self.path + '\n'
        self.git('commit', '-qm', self.message)
        self.head = self.git('rev-parse', 'HEAD').strip()
        self.git('checkout', '-q', 'main')
        self.git('merge', '-q', '--no-ff', '-m', 'merge fixture', BRANCH)
        self.merge = self.git('rev-parse', 'HEAD').strip()
        self.pr = dict(number=9, merged_at='2026-09-09T23:15:34Z', merge_commit_sha=self.merge,
                       base=dict(ref='main', repo=dict(full_name=REPOSITORY)),
                       head=dict(ref=BRANCH, sha=self.head))
        self.event = dict(after=self.merge, ref='refs/heads/main', repository=dict(full_name=REPOSITORY))

    def git(self, *args, input=None):
        result = subprocess.run(['git', '-C', str(self.root), '-c', 'commit.gpgSign=false',
                                 '-c', 'core.hooksPath=/dev/null', *args], input=input,
                                text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True)
        return result.stdout

    def select(self, pulls=None, event=None):
        with patch.object(harness, 'ROOT', self.root), patch.object(harness, 'gh', return_value=[self.pr] if pulls is None else pulls):
            return harness.ci_target(self.merge, self.event if event is None else event)


class CiBootstrap(unittest.TestCase):
    def fixture(self, record_changes=None):
        directory = tempfile.TemporaryDirectory(prefix='lower-ci-bootstrap-')
        self.addCleanup(directory.cleanup)
        return MergeFixture(Path(directory.name), record_changes)

    def rejected(self, fixture, why, pattern, **options):
        try:
            fixture.select(**options)
        except (RuntimeError, subprocess.CalledProcessError) as error:
            self.assertIn(pattern, str(error), 'CI_BOOTSTRAP wrong rejection cause: ' + why)
        else:
            self.fail('CI_BOOTSTRAP ' + why)

    def test_initial_cp0_selects_historical_head(self):
        fixture = self.fixture()
        original = fixture.file.read_bytes()
        try:
            record, certified, mode = fixture.select()
        except RuntimeError as error:
            self.fail('CI_BOOTSTRAP initial CP0 selects historical head: ' + str(error))
        self.assertEqual((fixture.head, 'historical'), (certified, mode))
        self.assertIsNone(record['pull_request'])
        self.assertEqual(fixture.record, record)
        self.assertEqual(original, fixture.file.read_bytes())

    def test_explicit_matching_pr_accepted(self):
        fixture = self.fixture({'pull_request': 9})
        self.assertEqual((fixture.head, 'historical'), fixture.select()[1:])

    def test_explicit_wrong_pr_rejected(self):
        self.rejected(self.fixture({'pull_request': 8}), 'explicit wrong PR rejected', 'merge/evidence identity')

    def test_null_later_checkpoint_rejected(self):
        self.rejected(self.fixture({'checkpoint': 'CP1'}), 'null later checkpoint rejected', 'merge/evidence identity')

    def test_known_pr_later_checkpoint_accepted(self):
        fixture = self.fixture({'checkpoint': 'CP1', 'pull_request': 9})
        self.assertEqual('historical', fixture.select()[2])

    def test_wrong_branch_rejected(self):
        self.rejected(self.fixture({'branch': 'fix/unrelated'}), 'wrong branch rejected', 'merge/evidence identity')

    def test_wrong_repository_rejected(self):
        self.rejected(self.fixture({'repository': 'example/other'}), 'wrong repository rejected', 'repository mismatch')

    def test_altered_merge_evidence_rejected(self):
        fixture = self.fixture()
        fixture.file.write_text(json.dumps(dict(fixture.record, extra='tampered')) + '\n')
        fixture.git('add', fixture.path)
        fixture.git('commit', '-qm', 'altered evidence')
        fixture.merge = fixture.git('rev-parse', 'HEAD').strip()
        fixture.event['after'] = fixture.merge
        fixture.pr['merge_commit_sha'] = fixture.merge
        self.rejected(fixture, 'altered merge evidence rejected', 'merge/evidence identity')

    def test_missing_merged_pr_rejected(self):
        self.rejected(self.fixture(), 'missing PR rejected', 'unique proven merged PR', pulls=[])

    def test_ambiguous_merged_pr_rejected(self):
        fixture = self.fixture()
        self.rejected(fixture, 'ambiguous PR rejected', 'unique proven merged PR', pulls=[fixture.pr, copy.deepcopy(fixture.pr)])

    def test_unmerged_pr_rejected(self):
        fixture = self.fixture(); fixture.pr['merged_at'] = None
        self.rejected(fixture, 'unmerged PR rejected', 'unique proven merged PR')

    def test_wrong_merge_sha_rejected(self):
        fixture = self.fixture(); fixture.pr['merge_commit_sha'] = fixture.head
        self.rejected(fixture, 'wrong merge SHA rejected', 'unique proven merged PR')

    def test_wrong_base_branch_rejected(self):
        fixture = self.fixture(); fixture.pr['base']['ref'] = 'develop'
        self.rejected(fixture, 'wrong base branch rejected', 'unique proven merged PR')

    def test_wrong_base_repository_rejected(self):
        fixture = self.fixture(); fixture.pr['base']['repo']['full_name'] = 'example/other'
        self.rejected(fixture, 'wrong base repository rejected', 'unique proven merged PR')

    def test_unrelated_head_rejected(self):
        fixture = self.fixture()
        # Valid, byte-identical certificate/trailer in an orphan commit: only
        # the actual Git ancestry proof distinguishes it from the real PR head.
        tree = fixture.git('rev-parse', fixture.head + '^{tree}').strip()
        orphan = fixture.git('commit-tree', tree, input=fixture.message).strip()
        fixture.pr['head']['sha'] = orphan
        self.rejected(fixture, 'unrelated head rejected', '--is-ancestor')

    def test_wrong_event_sha_rejected(self):
        fixture = self.fixture()
        self.rejected(fixture, 'wrong event SHA rejected', 'push SHA/ref mismatch', event=dict(fixture.event, after=fixture.head))

    def test_invalid_event_ref_rejected(self):
        fixture = self.fixture()
        self.rejected(fixture, 'invalid event ref rejected', 'push SHA/ref mismatch', event=dict(fixture.event, ref='refs/tags/main'))

    def test_branch_execution_keeps_exact_head(self):
        fixture = self.fixture()
        event = dict(fixture.event, after=fixture.head, ref='refs/heads/' + BRANCH)
        with patch.object(harness, 'ROOT', fixture.root), patch.object(harness, 'gh', side_effect=AssertionError('must not select a merged PR')):
            record, certified, mode = harness.ci_target(fixture.head, event)
        self.assertEqual((fixture.head, 'execution'), (certified, mode))
        self.assertIsNone(record['pull_request'])

    def test_merged_pr_still_rejects_execution(self):
        fixture = self.fixture()
        view = dict(number=9, state='MERGED', baseRefName='main', headRefName=BRANCH,
                    headRefOid=fixture.head, autoMergeRequest=None)
        with patch.object(harness, 'gh', return_value=[view]), self.assertRaisesRegex(RuntimeError, 'PR identity/state/head mismatch'):
            harness.check_pr(fixture.record, fixture.head, mode='execution')


if __name__ == '__main__':
    unittest.main(verbosity=2)
