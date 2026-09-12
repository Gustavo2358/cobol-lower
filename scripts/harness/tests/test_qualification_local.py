"""Falsify local receipt binding without running qualification or product campaigns."""
import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from ci_policy import git
from qualification_local import STAGES, CONTRACT, verify_receipt


def sha(b):return hashlib.sha256(b).hexdigest()


class ReceiptTest(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup);self.root=Path(self.temp.name)/'repo';self.root.mkdir()
        git(self.root,'init','-b','main');git(self.root,'config','user.name','receipt test');git(self.root,'config','user.email','test@example.invalid')
        contract=self.root/CONTRACT;contract.parent.mkdir(parents=True);contract.write_text('{"frozen":"test-only"}')
        (self.root/'src').mkdir();(self.root/'src/product.java').write_text('product');git(self.root,'add','.');git(self.root,'commit','-m','qualified fixture')
        self.head=git(self.root,'rev-parse','HEAD').decode().strip();self.out=Path(self.temp.name)/'receipt';self.out.mkdir();self.path=self.out/'receipt.json'
        self.receipt=dict(kind='LOCAL_QUALIFICATION',conclusion='success',execution_environment='LOCAL_ONLY',restoration='BYTE_EXACT_SECOND_GREEN',qualified_head=self.head,qualified_tree=git(self.root,'rev-parse','HEAD^{tree}').decode().strip(),contract_sha256=sha(contract.read_bytes()),stages=[],evidence_files={})
        for stage in STAGES:
            log=self.out/(stage+'.log');log.write_text('synthetic verification fixture, no qualification executed\n')
            self.receipt['stages'].append(dict(stage=stage,exit_code=0,log=log.name,log_sha256=sha(log.read_bytes())))
        self.save()
    def save(self):self.path.write_text(json.dumps(self.receipt))
    def test_valid_exact_head_relation(self):
        self.assertTrue(verify_receipt(self.root,self.path,self.head)['full_executed_on_current_head'])
    def test_cannot_turn_fast_into_qualification(self):
        self.receipt['kind']='FAST_CI';self.save()
        with self.assertRaisesRegex(RuntimeError,'not a successful'):verify_receipt(self.root,self.path,self.head)
    def test_missing_or_failed_stage(self):
        for value in (self.receipt['stages'][:-1],[dict(x,exit_code=1) for x in self.receipt['stages']]):
            self.receipt['stages']=value;self.save()
            with self.assertRaisesRegex(RuntimeError,'incomplete'):verify_receipt(self.root,self.path,self.head)
    def test_tampered_log(self):
        (self.out/'semantic.log').write_text('fake')
        with self.assertRaisesRegex(RuntimeError,'log mismatch'):verify_receipt(self.root,self.path,self.head)
    def test_tampered_tree(self):
        self.receipt['qualified_tree']='0'*40;self.save()
        with self.assertRaisesRegex(RuntimeError,'tree mismatch'):verify_receipt(self.root,self.path,self.head)
    def test_docs_successor_full_claim_is_rejected(self):
        (self.root/'docs/note.md').write_text('later receipt');git(self.root,'add','.');git(self.root,'commit','-m','docs successor')
        current=git(self.root,'rev-parse','HEAD').decode().strip();relation=verify_receipt(self.root,self.path,current)
        self.assertFalse(relation['full_executed_on_current_head']);self.assertEqual('DOCUMENTATION_ONLY_SUCCESSOR',relation['relation'])
        self.receipt['current_head']=current;self.receipt['full_executed_on_current_head']=True;self.save()
        with self.assertRaisesRegex(RuntimeError,'cannot transfer'):verify_receipt(self.root,self.path,current)
    def test_changed_source_invalidates(self):
        (self.root/'src/product.java').write_text('new product');git(self.root,'add','.');git(self.root,'commit','-m','changed source')
        current=git(self.root,'rev-parse','HEAD').decode().strip()
        with self.assertRaisesRegex(RuntimeError,'INVALIDATED'):verify_receipt(self.root,self.path,current)
    def test_no_remote_execution_claim(self):
        self.receipt['execution_environment']='GITHUB_ACTIONS';self.save()
        with self.assertRaisesRegex(RuntimeError,'execution/restoration'):verify_receipt(self.root,self.path,self.head)


if __name__=='__main__':unittest.main()
