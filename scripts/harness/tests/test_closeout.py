"""A closeout audits a certified parent; it never authorizes new runtime or contract changes."""
import json
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[3]
sys.path.insert(0,str(ROOT/'scripts/harness'))
import run as harness
W='WORK-LOWER-999'; E=f'docs/quality/{W}/CP0.json'; A=f'docs/work/active/{W}'
class Closeout(unittest.TestCase):
    def fixture(self, mutate=None, trailer=None):
        temp=tempfile.TemporaryDirectory(prefix='lower-closeout-test-');self.addCleanup(temp.cleanup);r=Path(temp.name)
        def git(*args):return subprocess.check_output(['git','-C',str(r),'-c','commit.gpgSign=false','-c','core.hooksPath=/dev/null',*args],stderr=subprocess.DEVNULL,text=True).strip()
        def write(p,value):
            f=r/p;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(value)
        git('init','-q','--initial-branch=baseline');git('config','user.name','Fixture');git('config','user.email','fixture@example.invalid')
        record=dict(repository='example/lower',branch='baseline',pull_request=None,checkpoint='CP0',work_item=W)
        write(E,json.dumps(record)+'\n');write(f'docs/quality/{W}/CP0-manifest.yaml','immutable manifest\n');write('core/src/main/Guard.java','class Guard {}\n')
        for f in ('work-item.yaml','spec.md','plan.md','eval.md','state.md'):write(A+'/'+f,'historical work file\n')
        write('docs/work/registry.json',json.dumps(dict(active=[dict(id=W)],history=[])))
        git('add','.');git('commit','-qm','certified parent\n\nCheckpoint-Evidence: '+E);parent=git('rev-parse','HEAD')
        shutil.rmtree(r/A);write(f'docs/work/history/{W}.md','completed; remote merge pending\n')
        write('docs/work/registry.json',json.dumps(dict(active=[],history=[dict(id=W,status='completed',manifest=f'docs/quality/{W}/CP0-manifest.yaml',evidence=E,git_branch='baseline',pull_request=9)])))
        if mutate:mutate(r,write)
        git('add','--all');git('commit','-qm','documentary closeout\n\nCheckpoint-Evidence: '+E+'\nWork-Item-Closeout: '+(trailer or W));head=git('rev-parse','HEAD')
        return r,git,parent,head,record
    def select(self,r,head):
        pr=dict(number=9,head=dict(sha=head,ref='baseline'),base=dict(ref='main',repo=dict(full_name='example/lower')))
        with patch.object(harness,'ROOT',r),patch.object(harness,'gh',return_value=[pr]):return harness.ci_target(head,dict(after=head,ref='refs/heads/baseline',repository=dict(full_name='example/lower')))
    def test_closeout_selects_certified_parent_for_historical_audit(self):
        r,g,parent,head,record=self.fixture();self.assertEqual((record,parent,'historical'),self.select(r,head),'CLOSEOUT valid documentary closeout selects certified parent')
    def test_normal_head_keeps_execution_mode(self):
        r,g,parent,head,record=self.fixture();self.assertEqual((record,parent,'execution'),self.select(r,parent))
    def test_product_change_rejected(self):
        r,g,p,h,e=self.fixture(lambda r,w:w('core/src/main/Guard.java','class Changed {}\n'))
        with self.assertRaisesRegex(RuntimeError,'CLOSEOUT forbidden change'):self.select(r,h)
    def test_certificate_change_rejected(self):
        r,g,p,h,e=self.fixture(lambda r,w:w(E,(r/E).read_text()+' '))
        with self.assertRaisesRegex(RuntimeError,'CLOSEOUT certificate changed'):self.select(r,h)
    def test_source_lock_change_rejected(self):
        r,g,p,h,e=self.fixture(lambda r,w:w('docs/sources/sources.lock.json','{}'))
        with self.assertRaisesRegex(RuntimeError,'CLOSEOUT forbidden change'):self.select(r,h)
    def test_work_still_active_rejected(self):
        r,g,p,h,e=self.fixture(lambda r,w:w(A+'/state.md','active'))
        with self.assertRaisesRegex(RuntimeError,'CLOSEOUT active residue'):self.select(r,h)
    def test_unrelated_work_rejected(self):
        r,g,p,h,e=self.fixture(trailer='WORK-LOWER-998')
        with self.assertRaisesRegex(RuntimeError,'CLOSEOUT identity'):self.select(r,h)
    def test_contract_change_rejected(self):
        r,g,p,h,e=self.fixture(lambda r,w:w(f'docs/quality/{W}/CP0-manifest.yaml','altered'))
        with self.assertRaisesRegex(RuntimeError,'CLOSEOUT forbidden change'):self.select(r,h)
    def test_arbitrary_document_change_rejected(self):
        r,g,p,h,e=self.fixture(lambda r,w:w('docs/domain/rules.md','new semantic rule'))
        with self.assertRaisesRegex(RuntimeError,'CLOSEOUT forbidden change'):self.select(r,h)
    def test_wrong_remote_pr_rejected(self):
        r,g,p,h,e=self.fixture()
        pr=dict(number=10,head=dict(sha=h,ref='baseline'),base=dict(ref='main',repo=dict(full_name='example/lower')))
        with patch.object(harness,'ROOT',r),patch.object(harness,'gh',return_value=[pr]):
            with self.assertRaisesRegex(RuntimeError,'CLOSEOUT remote PR binding'):
                harness.ci_target(h,dict(after=h,ref='refs/heads/baseline',repository=dict(full_name='example/lower')))
    def test_unrelated_registry_change_rejected(self):
        def mutate(r,w):
            d=json.loads((r/'docs/work/registry.json').read_text());d['history'].append(dict(id='WORK-LOWER-998'));w('docs/work/registry.json',json.dumps(d))
        r,g,p,h,e=self.fixture(mutate)
        with self.assertRaisesRegex(RuntimeError,'CLOSEOUT unrelated registration'):self.select(r,h)
    def test_merged_closeout_still_audits_certified_parent(self):
        r,g,p,h,e=self.fixture();g('checkout','-qb','main',p);g('merge','-q','--no-ff','-m','merge','baseline');merge=g('rev-parse','HEAD')
        pr=dict(number=9,merged_at='2026-09-10T00:00:00Z',merge_commit_sha=merge,head=dict(sha=h,ref='baseline'),base=dict(ref='main',repo=dict(full_name='example/lower')))
        with patch.object(harness,'ROOT',r),patch.object(harness,'gh',return_value=[pr]):
            actual=harness.ci_target(merge,dict(after=merge,ref='refs/heads/main',repository=dict(full_name='example/lower')))
        self.assertEqual((e,p,'historical'),actual)
if __name__=='__main__':unittest.main(verbosity=2)
