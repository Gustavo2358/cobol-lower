#!/usr/bin/env python3
"""Local W2B product falsification, isolated sources, byte-exact restoration and second GREEN."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import time
from local_only import require_local

ROOT = Path(__file__).resolve().parents[2]
APP = 'core/src/main/java/io/github/gustavo2358/lower/application/'
CASES = [
 (1,'swap-true-false','IfSequenceAssembler.java','predicate, thenLabel, elseLabel','predicate, elseLabel, thenLabel','TRUE/FALSE follows explicit arm entry'),
 (2,'both-to-then','IfSequenceAssembler.java','predicate, thenLabel, elseLabel','predicate, thenLabel, thenLabel','TRUE/FALSE follows explicit arm entry'),
 (3,'invent-open-else','IfSequenceAssembler.java','.orElse(merge);','.orElse(label("else", f.header().id(), unit, ids));','successful diamond'),
 (4,'remove-then-jump','IfSequenceAssembler.java','new Sequence(label, instructions, jump, origin)','new Sequence(label, instructions, arm == owner.thenArm() ? new Operations.Return(jump.header(), List.of()) : jump, origin)','arm Jump to shared CALL merge'),
 (5,'remove-else-jump','IfSequenceAssembler.java','new Sequence(label, instructions, jump, origin)','new Sequence(label, instructions, arm == owner.elseArm() ? new Operations.Return(jump.header(), List.of()) : jump, origin)','arm Jump to shared CALL merge'),
 (6,'physical-label-authority','IfSequenceAssembler.java','label("root", f.header().id(), unit, ids)','label("root", plan.admission().input().orElseThrow().statements().getFirst().header().id(), unit, ids)','physical input inventory permutation byte identity'),
 (7,'first-sequence-entry','IfLowerer.java','assembly.entryLabel()','assembly.sequences().getFirst().label()','public entry correlation follows IF identity'),
 (8,'statement-id-control','IfSequenceAssembler.java','for (var move : moves)','for (var move : moves.stream().sorted(Comparator.comparing(m -> m.header().id().handle())).toList())','MOVE order from canonical children, not StatementId'),
 (9,'merge-is-return','IfSequenceAssembler.java','label("call", f.normalContinuation().statement().orElseThrow(), unit, ids)','label("return", call.normalContinuation().statement().orElseThrow(), unit, ids)','successful diamond'),
 (12,'evaluate-false','IfSequenceAssembler.java','predicate, thenLabel, elseLabel','predicate, elseLabel, elseLabel','TRUE/FALSE follows explicit arm entry'),
 (13,'literal-bool','IfSequenceAssembler.java','predicate, thenLabel, elseLabel','new Expressions.Literal(predicate.header(), new Values.BoolValue(false)), thenLabel, elseLabel','predicate stays Unknown'),
 (14,'erase-predicate-read','IfPredicate.java','dependencies.add(read);','/* known Read erased */','all and only knownReads'),
 (15,'wrong-predicate-object','IfPredicate.java','reference.wholeItemAccess().orElseThrow().data()','data.index().keySet().iterator().next()','dependency maps published FLAG identity'),
 (16,'open-remaining-reads','IfPredicate.java','dependencies, Scopes.NoMemory.INSTANCE, reason','dependencies, new Scopes.WithinMemory(new Scopes.AllMemory(operation.unit().publication(), true)), reason','COMPLETE reads -> NoMemory remainder'),
 (17,'infer-disjoint-from-ids','StoragePremise.java','for (var member : proof.members()) members.add(data.index().get(member).storage());','for (var mapping : data.index().values()) members.add(mapping.storage());','exact ordered proof members, including extra unused members'),
 (18,'drop-proof-member','StoragePremise.java','proof.members())','proof.members().subList(0, proof.members().size()>2 ? proof.members().size()-1 : proof.members().size()))','exact ordered proof members, including extra unused members'),
 (19,'sort-proof-members','StoragePremise.java','proof.members())','proof.members().stream().sorted(Comparator.comparing(SpInput.DataId::handle)).toList())','exact ordered proof members, including extra unused members'),
 (20,'only-two-proof-members','StoragePremise.java','proof.members())','proof.members().subList(0, 2))','exact ordered proof members, including extra unused members'),
 (21,'invent-authority','StoragePremise.java','proof.authority(), "SP rule: "','"cobol-lower invented", "SP rule: "','published authority and descriptive rule'),
 (22,'lose-premise-origin','StoragePremise.java','proof.provenance().orElseThrow()','new SpInput.Provenance(new SpInput.Location("wrong", 1, 0, 1, 1), new SpInput.Location("wrong", 1, 0, 1, 1), List.of(), true)','premise source origin'),
 (23,'fake-open-havoc','IfSequenceAssembler.java','new Sequence(merge, List.of(), invoke, mergeOrigin)','new Sequence(merge, f.elseArm().presence() == SpInput.ClausePresence.ABSENT ? List.of(new Operations.HavocMay(new Operations.Header(new OperationId(unit, "invented-havoc"), mergeOrigin, Evidence.CoverageStatus.ABSTRACTED, invoke.header().precision(), invoke.header().uncertainties()), new Scopes.AllMemory(unit.publication(), true), invoke.header().uncertainties().get(2))) : List.of(), invoke, mergeOrigin)','merge contains Invoke terminator only'),
 (24,'flatten-nested-profile','IfAdmission.java','f.profile()==IfProfile.SIMPLE_TEXT_EQUALITY','true','reject nested profile'),
 (25,'lose-fitting','MoveHandler.java','move.textAdjustment().map(a -> a.result().value()).orElseGet(() -> move.source().logicalValue().orElseThrow().value())','move.source().logicalValue().orElseThrow().value()','fitted text from published MOVE'),
 (26,'break-w1-computed','InvokeHandler.java','new Interactions.ComputedTarget("program", "cobol.program", read, namePolicy, targetOrigin)','new Interactions.LiteralTarget("program", "cobol.program", "PROGA", namePolicy, targetOrigin)','dynamic target remains computed'),
]


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def main():
    require_local()
    build=Path(os.environ['LOWER_BUILD_ROOT']);logs=build/('if-challenge-'+str(time.time_ns()));logs.mkdir(parents=True)
    dependency=':'.join(str(build/'m2'/j) for j in ['io/github/gustavo2358/air-java/0.1.0-SNAPSHOT/air-java-0.1.0-SNAPSHOT.jar','com/dynatrace/hash4j/hash4j/0.30.0/hash4j-0.30.0.jar'])
    receipt=dict(kind='LOCAL_W2B_CHALLENGE',source_head=subprocess.check_output(['git','-C',str(ROOT),'rev-parse','HEAD'],text=True).strip(),cases=[])
    with tempfile.TemporaryDirectory(prefix='lower-if-challenge-') as temp:
        root=Path(temp);shutil.copytree(ROOT/'core/src',root/'core/src');classes=root/'classes'
        sources=sorted(root.glob('core/src/**/*.java'));before={str(p.relative_to(root)):sha(p.read_bytes()) for p in sources}
        receipt['source_sha256']=before
        def execute(name,expected=None,main_class='io.github.gustavo2358.lower.testing.IfSuite'):
            if classes.exists():shutil.rmtree(classes)
            classes.mkdir()
            command=['javac','--release','21','-cp',dependency,'-d',str(classes),*map(str,sources)]
            compiled=subprocess.run(command,stdout=subprocess.PIPE,stderr=subprocess.STDOUT);(logs/(name+'-compile.log')).write_bytes(compiled.stdout)
            if compiled.returncode:raise RuntimeError(name+': compile error is not a semantic kill: '+compiled.stdout.decode()[:1800])
            command=['java','-ea','-Xmx2g','-cp',str(classes)+':'+dependency,main_class]
            result=subprocess.run(command,stdout=subprocess.PIPE,stderr=subprocess.STDOUT);(logs/(name+'.log')).write_bytes(result.stdout)
            output=result.stdout.decode()
            if expected is None and result.returncode:raise RuntimeError(name+': GREEN failed '+output[:1800])
            if expected is not None and (not result.returncode or not any('java.lang.AssertionError: '+prefix+expected in output for prefix in ('W2B ','CP6 '))):
                raise RuntimeError(name+': wrong detection '+output[:1800])
            return dict(command=command,exit_code=result.returncode,log=name+'.log',log_sha256=sha(result.stdout),compile_exit_code=compiled.returncode,compile_log_sha256=sha(compiled.stdout))
        execute('first-green')
        for number,name,file,old,new,expected in CASES:
            path=root/APP/file;raw=path.read_bytes();source=raw.decode();count=source.count(old)
            if count != (2 if number==7 else 1):raise RuntimeError(name+': unreviewed mutation site count '+str(count))
            changed=source.replace(old,new)
            edits={path:changed}
            # Deleted expression operands must not leave coverage references masking the semantic oracle.
            if number in (13,14):
                p=root/APP/'IfPredicate.java';s=changed if number==14 else p.read_text()
                s='\n'.join(line for line in s.split('\n') if 'links.add(new LoweringResult.OperandLink(known' not in line and 'items.add(ScalarEvidence.item(operation.unit().publication(), "operand"' not in line)
                edits[p]=s
            if number==26:
                edits[path]='\n'.join(line for line in changed.split('\n') if 'links.add(new LoweringResult.OperandLink(reference.id(),' not in line and 'items.add(ScalarEvidence.item(unit.publication(), "operand", reference.id()' not in line)
            originals={p:p.read_bytes() for p in edits}
            try:
                for p,s in edits.items():p.write_text(s)
                red=execute(name,expected,'io.github.gustavo2358.lower.testing.CallSuite' if number==26 else 'io.github.gustavo2358.lower.testing.IfSuite')
            finally:
                for p,raw_bytes in originals.items():p.write_bytes(raw_bytes)
            if before!={str(p.relative_to(root)):sha(p.read_bytes()) for p in sources}:raise RuntimeError('whole-source restoration mismatch')
            green=execute(name+'-restored')
            receipt['cases'].append(dict(number=number,id=name,mutation=[file,old,new],expected_oracle=expected,red=red,restoration='BYTE_EXACT',second_green=green))
            (logs/'receipt.json').write_text(json.dumps(receipt,indent=2)+'\n')
            print(name+': semantic RED, byte-exact restoration, second GREEN',flush=True)
        execute('second-green')
    from focal import classpath
    command=['java','-ea','-cp',classpath(ROOT,build),'io.github.gustavo2358.lower.adapters.testing.IfPlacementChallenge']
    result=subprocess.run(command,stdout=subprocess.PIPE,stderr=subprocess.STDOUT);(logs/'placement.log').write_bytes(result.stdout)
    if result.returncode or result.stdout.count(b'DETECTED=I-04 RESTORED_BYTE_EXACT SECOND_GREEN')!=2:raise RuntimeError('placement challenge failed '+result.stdout.decode())
    receipt['placement']=dict(numbers=[10,11],kind='MALFORMED_AIR_WIRE',reason='Java sealed Instruction/Terminator types exclude these placements; real W2C decoder must reject malformed wire with I-04.',command=command,exit_code=result.returncode,log_sha256=sha(result.stdout))
    receipt['conclusion']='PASS';(logs/'receipt.json').write_text(json.dumps(receipt,indent=2)+'\n')
    print('PASS IF CHALLENGE: 24 compiled product mutations + 2 malformed placement challenges; '+str(logs/'receipt.json'),flush=True)


if __name__=='__main__':main()
