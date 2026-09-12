"""Git helper for technical source identity checks."""
import subprocess

def git(root, *args):
    return subprocess.check_output(['git', '-C', str(root), *args])
