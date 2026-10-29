"""Build a reviewed source archive using the same allowlist as check_public.py."""
from pathlib import Path
import argparse
import zipfile
from check_public import ROOT, files, inspect

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('output', type=Path, help='Output ZIP path outside this project')
    args = parser.parse_args()
    issues = inspect()
    if issues: raise SystemExit('Export blocked: ' + repr(issues))
    output = args.output.resolve()
    if output == ROOT or ROOT in output.parents:
        raise SystemExit('Choose a ZIP path outside the source project.')
    output.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(output, 'w', zipfile.ZIP_DEFLATED) as archive:
        for path in files(): archive.write(path, Path('hizlitv') / path.relative_to(ROOT))
    print(output)

if __name__ == '__main__': main()
