"""Narrow documentary closeout audit; original certificates and production remain immutable."""
import json
import re
from git_checks import git, evidence_path


def closeout_target(root, head):
    if not re.fullmatch(r'[0-9a-f]{40}', head):
        raise RuntimeError('CLOSEOUT explicit SHA required')
    def trailer(sha):
        return git(root,'show','-s','--format=%(trailers:key=Work-Item-Closeout,valueonly)',sha).decode().split()
    values=trailer(head)
    if not values:
        return None
    if len(values)!=1 or not re.fullmatch(r'WORK-[A-Z][A-Z0-9-]*-[0-9]{3}',values[0]):
        raise RuntimeError('CLOSEOUT identity')
    work=values[0]
    parents=git(root,'show','-s','--format=%P',head).decode().split()
    if len(parents)!=1 or trailer(parents[0]):
        raise RuntimeError('CLOSEOUT requires one direct certified parent')
    parent=parents[0];path=evidence_path(root,parent)
    raw=git(root,'show',parent+':'+path);record=json.loads(raw)
    if record.get('work_item')!=work or evidence_path(root,head)!=path:
        raise RuntimeError('CLOSEOUT identity')
    if raw!=git(root,'show',head+':'+path):
        raise RuntimeError('CLOSEOUT certificate changed')
    active=f'docs/work/active/{work}/'
    if git(root,'ls-tree','-r','--name-only',head,'--',active):
        raise RuntimeError('CLOSEOUT active residue')
    deleted={active+f for f in ('work-item.yaml','spec.md','plan.md','eval.md','state.md')}
    allowed=deleted|{'docs/work/registry.json','docs/work/index.md','docs/work/backlog.md',
                     f'docs/work/history/{work}.md',f'docs/quality/{work}/closeout.json'}
    changes=git(root,'diff','--no-renames','--name-status',parent,head).decode().splitlines()
    for line in changes:
        status,name=line.split('\t',1)
        if name not in allowed or (name in deleted and status!='D'):
            raise RuntimeError('CLOSEOUT forbidden change '+name)
    if not changes:
        raise RuntimeError('CLOSEOUT empty delta')
    registry=json.loads(git(root,'show',head+':docs/work/registry.json'))
    before=json.loads(git(root,'show',parent+':docs/work/registry.json'))
    for kind in ('active','history'):
        if [x for x in registry[kind] if x.get('id')!=work] != [x for x in before[kind] if x.get('id')!=work]:
            raise RuntimeError('CLOSEOUT unrelated registration')
    if {k:v for k,v in registry.items() if k not in ('active','history')} != {k:v for k,v in before.items() if k not in ('active','history')}:
        raise RuntimeError('CLOSEOUT unrelated registration')
    rows=[row for row in registry['history'] if row.get('id')==work]
    if (len(rows)!=1 or any(row.get('id')==work for row in registry['active'])
            or rows[0].get('status')!='completed' or rows[0].get('evidence')!=path
            or rows[0].get('git_branch')!=record['branch']
            or rows[0].get('manifest')!=f'docs/quality/{work}/{record["checkpoint"]}-manifest.yaml'
            or record['pull_request'] not in (None,rows[0].get('pull_request'))):
        raise RuntimeError('CLOSEOUT registration')
    return parent
