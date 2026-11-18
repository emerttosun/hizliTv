"""Turn a failed CI build into GitHub annotations, which anyone can read without opening the log.

Reports, at most ten in all: the Gradle "What went wrong" block, javac errors, failed JUnit tests
and lint errors. It reads only build outputs and the Gradle console copy; it prints no secrets.
"""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
LIMIT = 10


def escape(text):
    return text.replace('%', '%25').replace('\r', '').replace('\n', '%0A')


def emit(found, title, message, file=None, line=None):
    if len(found) >= LIMIT:
        return
    where = ''
    if file:
        where = f' file={file}' + (f',line={line}' if line else '') + ','
    found.append(f'::error{where} title={title}::{escape(message.strip()[:3000])}')


def gradle(found):
    log = ROOT / 'build' / 'ci' / 'gradle.txt'
    if not log.exists():
        return
    text = log.read_text(errors='replace')
    block = re.search(r'\* What went wrong:\n(.*?)(?:\n\* Try:|\Z)', text, re.S)
    if block:
        emit(found, 'Gradle', block.group(1))
    for match in re.finditer(r'^(\S+\.java):(\d+): error: (.*)$', text, re.M):
        emit(found, 'javac', match.group(3), str(Path(match.group(1)).relative_to(ROOT)) if match.group(1).startswith(str(ROOT)) else match.group(1), match.group(2))


def tests(found):
    for report in sorted(ROOT.glob('*/build/test-results/**/*.xml')):
        for case in ET.parse(report).getroot().iter('testcase'):
            for failure in list(case.findall('failure')) + list(case.findall('error')):
                body = (failure.get('message') or '') + '\n' + '\n'.join((failure.text or '').splitlines()[:15])
                emit(found, 'Test failed', f"{case.get('classname')}.{case.get('name')}\n{body}")


def lint(found):
    for report in sorted(ROOT.glob('*/build/reports/lint-results-debug.xml')):
        for issue in ET.parse(report).getroot().iter('issue'):
            if issue.get('severity') not in ('Error', 'Fatal'):
                continue
            location = issue.find('location')
            file = location.get('file') if location is not None else None
            if file and file.startswith(str(ROOT)):
                file = str(Path(file).relative_to(ROOT))
            emit(found, f"Lint {issue.get('id')}", issue.get('message') or '', file, location.get('line') if location is not None else None)


if __name__ == '__main__':
    found = []
    gradle(found)
    tests(found)
    lint(found)
    if not found:
        found.append('::error title=CI::The build failed but no Gradle, test or lint error was recognised.')
    print('\n'.join(found))
