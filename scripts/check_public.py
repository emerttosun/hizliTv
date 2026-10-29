"""Inspect the exact export allowlist for private local data; no third-party packages."""
from pathlib import Path
import re
import os
import sys

ROOT = Path(__file__).resolve().parents[1]
TOP_LEVEL = {'README.md', 'README.en.md', 'README.tr.md', '.gitmodules', 'LICENSE', 'NOTICE', 'CONTRIBUTING.md',
             'SECURITY.md', '.gitignore', '.editorconfig', '.github', 'gradle',
             'gradlew', 'gradlew.bat', 'build.gradle', 'settings.gradle',
             'gradle.properties', 'launcher', 'player', 'tv-ui', 'media-core', 'legacy-bridge',
             'docs', 'examples', 'scripts', 'airplay'}
EXCLUDED = {'build', '.gradle', '__pycache__', '.git', '.idea', '.state', '.cache'}
PRIVATE_NAMES = {'local.properties', 'local-signing.properties', '.env'}
PRIVATE_SUFFIXES = {'.p12', '.jks', '.keystore', '.pem', '.key', '.apk', '.aab', '.log'}

def files(root=ROOT):
    for current, directories, names in os.walk(root, followlinks=False):
        current = Path(current)
        rel = current.relative_to(root)
        directories[:] = sorted(name for name in directories
                                if name not in EXCLUDED
                                and (rel.parts or name in TOP_LEVEL)
                                and not (rel == Path('airplay') and name == 'receiver'))
        for name in sorted(names):
            path = current / name
            relative = path.relative_to(root)
            if relative.parts[0] not in TOP_LEVEL or name in PRIVATE_NAMES or name.startswith('.env.'):
                continue
            yield path

def inspect(root=ROOT):
    problems = []
    patterns = [
        ('private key', re.compile(r'-----BEGIN [A-Z ]*PRIVATE KEY-----')),
        ('personal filesystem path', re.compile('/' + r'Users/[^/\s]+/|/' + r'home/[^/\s]+/')),
        ('pairing identity', re.compile(r'(?<![a-fA-F0-9])[a-fA-F0-9]{48}(?![a-fA-F0-9])')),
        ('private LAN address', re.compile(r'\b(?:192\.168\.\d{1,3}|10\.\d{1,3}\.\d{1,3}|172\.(?:1[6-9]|2\d|3[01])\.\d{1,3})\.\d{1,3}\b')),
    ]
    for path in files(root):
        rel = str(path.relative_to(root))
        if path.is_symlink() or path.suffix in PRIVATE_SUFFIXES:
            problems.append((rel, 'private or linked file')); continue
        try:
            value = path.read_text(encoding='utf-8')
        except UnicodeDecodeError:
            if rel != 'gradle/wrapper/gradle-wrapper.jar' and not (rel.startswith('docs/images/') and path.suffix == '.png'):
                problems.append((rel, 'unexpected binary'))
            continue
        for label, pattern in patterns:
            if pattern.search(value): problems.append((rel, label))
    return problems

if __name__ == '__main__':
    problems = inspect()
    for path, reason in problems: print(path + ': ' + reason)
    if problems: sys.exit(1)
    print('Public export check passed: ' + str(len(list(files()))) + ' files.')
