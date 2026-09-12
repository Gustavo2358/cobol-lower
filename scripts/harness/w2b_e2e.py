#!/usr/bin/env python3
"""LOCAL_QUALIFICATION only: exact W1A regression and W2A real producers, each twice."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
from local_only import require_local
from focal import classpath
from w1c_e2e import W1A

ROOT=Path(__file__).resolve().parents[2]
W2A='4ffabded1aad39316b8a6f337f732976fdb3ca3e'
W2A_TREE='5880e174b33c85ba3f3cdbc70bd2d8dc7b1d567b'
JARS=['org/antlr/antlr4-runtime/4.13.2/antlr4-runtime-4.13.2.jar','org/slf4j/slf4j-api/2.0.18/slf4j-api-2.0.18.jar','ch/qos/logback/logback-core/1.6.3/logback-core-1.6.3.jar','ch/qos/logback/logback-classic/1.6.3/logback-classic-1.6.3.jar','com/fasterxml/jackson/core/jackson-core/2.22.2/jackson-core-2.22.2.jar','com/fasterxml/jackson/core/jackson-databind/2.22.2/jackson-databind-2.22.2.jar','com/fasterxml/jackson/core/jackson-annotations/2.22/jackson-annotations-2.22.jar']


def sha(raw):return hashlib.sha256(raw).hexdigest()


def checked(command,cwd,log):
    result=subprocess.run(command,cwd=cwd,stdout=subprocess.PIPE,stderr=subprocess.STDOUT);log.write_bytes(result.stdout)
    if result.returncode:raise RuntimeError(str(log)+': exit '+str(result.returncode)+' '+result.stdout.decode()[-1500:])
    return dict(command=command,exit_code=result.returncode,log=str(log),log_sha256=sha(result.stdout))


def main():
    require_local()
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--out',type=Path,required=True);args=parser.parse_args()
    out=args.out.resolve();out.mkdir(parents=True);build=Path(os.environ['LOWER_BUILD_ROOT']).resolve();m2=build/'m2'
    sources=json.loads((ROOT/'docs/sources/sources.lock.json').read_text())['sources'];sp=next(s for s in sources if s['id']=='SRC-SP')
    if sp['commit']!=W2A or sp['tree']!=W2A_TREE:raise RuntimeError('W2A product authority mismatch')
    def producer(name,commit):
        checkout=build/name
        if not checkout.exists():
            checked(['git','clone','--no-hardlinks','--no-checkout',str(ROOT.parent/'proleap-poc'),str(checkout)],ROOT,out/(name+'-clone.log'))
            checked(['git','-C',str(checkout),'checkout','--detach',commit],ROOT,out/(name+'-checkout.log'))
        head=subprocess.check_output(['git','-C',str(checkout),'rev-parse','HEAD'],text=True).strip()
        if head!=commit or subprocess.check_output(['git','-C',str(checkout),'status','--porcelain']):raise RuntimeError('producer checkout differs')
        if commit==W2A:
            tree=subprocess.check_output(['git','-C',str(checkout),'rev-parse','HEAD^{tree}'],text=True).strip()
            if tree!=W2A_TREE:raise RuntimeError('W2A tree mismatch')
            for path,expected in sp['blob_sha256'].items():
                if sha((checkout/path).read_bytes())!=expected:raise RuntimeError('SP authority blob changed '+path)
        checked(['mvn','-B','-ntp','-Dmaven.repo.local='+str(m2),'clean','package'],checkout,out/(name+'-build.log'))
        return checkout
    w1=producer('producer-w1a',W1A)
    checked(['python3','scripts/harness/w1c_e2e.py','--producer',str(w1),'--m2',str(m2),'--out',str(out/'w1'),'--lower'],ROOT,out/'w1-integration.log')
    w2=producer('producer-w2a',W2A)
    producer_cp=':'.join([str(w2/'target/classes')]+[str(m2/j) for j in JARS]);lower_cp=classpath(ROOT,build)
    receipt=dict(kind='LOCAL_W2B_REAL_PRODUCER',producer_head=W2A,producer_tree=W2A_TREE,source_lock_sha256=sha((ROOT/'docs/sources/sources.lock.json').read_bytes()),cases=[])
    execution=out/'execution';execution.mkdir();web=execution/'src/main/resources/web';web.parent.mkdir(parents=True);web.symlink_to(w2/'src/main/resources/web',target_is_directory=True)
    for name in ('closed','open'):
        fixture=ROOT/'adapters/src/test/resources/sp/w2b'/(name+'.cbl');shutil.copyfile(fixture,execution/fixture.name);runs=[]
        for number in (1,2):
            destination=out/(name+'-'+str(number));command=['java','-cp',producer_cp,'io.github.gustavo2358.cobolexplorer.ExplorerMain','--source',fixture.name,'--copybooks',str(w2/'corpus/cpy'),'--output',str(destination)]
            run=checked(command,execution,out/(name+'-'+str(number)+'-producer.log'));wire=destination/'cobol-semantic-product.json';raw=wire.read_bytes()
            if json.loads(raw)['contractVersion']!='1.4.0':raise RuntimeError('producer must publish real SP1.4')
            run['sp_sha256']=sha(raw)
            for label,main_class in [('cli','io.github.gustavo2358.lower.adapters.cli.CobolLower'),('oracle','io.github.gustavo2358.lower.adapters.testing.IfIntegrationSuite')]:
                output=destination/(label+'.air.json');cmd=['java','-ea','-cp',lower_cp,main_class,str(wire),str(output)]
                run[label]=checked(cmd,ROOT,out/(name+'-'+str(number)+'-'+label+'.log'));run[label]['air_sha256']=sha(output.read_bytes())
            if (destination/'cli.air.json').read_bytes()!=(destination/'oracle.air.json').read_bytes():raise RuntimeError('CLI/oracle output differs')
            runs.append(run)
        for file in ('cobol-semantic-product.json','cli.air.json','oracle.air.json'):
            if (out/(name+'-1')/file).read_bytes()!=(out/(name+'-2')/file).read_bytes():raise RuntimeError('determinism failed '+name+'/'+file)
        receipt['cases'].append(dict(name=name,source_sha256=sha(fixture.read_bytes()),runs=runs,deterministic=True))
        (out/'receipt.json').write_text(json.dumps(receipt,indent=2)+'\n');print(name+': exact W2A producer twice, SP/AIR deterministic, public oracle and codec PASS',flush=True)
    receipt['conclusion']='PASS';(out/'receipt.json').write_text(json.dumps(receipt,indent=2)+'\n')


if __name__=='__main__':main()
