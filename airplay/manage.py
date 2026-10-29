#!/usr/bin/env python3
"""Pinned AirPlay download and reversible receiver profiles. Standard library only.
Original integration tooling: MIT. Receiver source and binary: upstream GPL-3.0.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shlex
import subprocess
import tempfile
import time
import urllib.request
import uuid

ROOT = Path(__file__).resolve().parent
UPSTREAM = json.loads((ROOT / 'upstream.json').read_text())
PACKAGE = UPSTREAM['package']
KEYS = {'server_name', 'require_pin', 'sync_client_volume', 'resolution', 'max_fps',
        'low_latency', 'audio_auto_buffer', 'audio_cushion_ms'}

def validate(values):
    if not isinstance(values, dict) or not values or set(values) - KEYS:
        raise ValueError('Unexpected or empty receiver profile')
    for key, value in values.items():
        if key in {'require_pin', 'sync_client_volume', 'low_latency', 'audio_auto_buffer'}:
            if not isinstance(value, bool): raise ValueError('Expected a boolean: ' + key)
        elif key == 'max_fps':
            if type(value) is not int or not 1 <= value <= 60: raise ValueError('FPS must be between 1 and 60')
        elif key == 'audio_cushion_ms':
            if type(value) is not int or not 1 <= value <= 1000: raise ValueError('Invalid audio cushion')
        elif key == 'resolution':
            if value not in {'auto', '1920x1080', '1280x720'}: raise ValueError('Unsupported profile resolution')
        elif not isinstance(value, str) or not value.strip() or len(value) > 64 or any(ord(c) < 32 for c in value):
            raise ValueError('Invalid receiver name')
    return values

def save_state(path, state):
    path.parent.mkdir(mode=0o700, parents=True, exist_ok=True)
    fd, temporary = tempfile.mkstemp(dir=path.parent, prefix='.airplay-')
    try:
        with os.fdopen(fd, 'w') as out: json.dump(state, out, indent=2)
        os.replace(temporary, path)
    finally:
        if os.path.exists(temporary): os.unlink(temporary)

class Receiver:
    def __init__(self, adb, serial, port=5037):
        if not serial: raise ValueError('Pass --serial for the TV you explicitly authorized in adb devices')
        self.command = [adb, '-P', str(port), '-s', serial]
    def shell(self, arguments):
        # adb shell is parsed remotely. Quote each argument instead of interpolating it.
        out = subprocess.run(self.command + ['shell', shlex.join(arguments)],
                             check=True, capture_output=True, text=True, timeout=20)
        return out.stdout
    def settings(self):
        out = self.shell(['am', 'broadcast', '-n', PACKAGE + '/.SettingsReceiver', '-a', PACKAGE + '.LIST_SETTINGS'])
        if 'result=-1' not in out or '{' not in out: raise RuntimeError('Receiver unavailable; install and open it first')
        return json.loads(out[out.index('{'):out.rindex('}') + 1])
    def set(self, key, value):
        validate({key: value})
        rendered = str(value).lower() if isinstance(value, bool) else str(value)
        out = self.shell(['am', 'broadcast', '-n', PACKAGE + '/.SettingsReceiver', '-a', PACKAGE + '.SET_SETTING', '--es', 'key', key, '--es', 'value', rendered])
        if 'result=-1' not in out: raise RuntimeError('Receiver rejected setting: ' + key)

def apply(receiver, values, state_path):
    validate(values); current = receiver.settings()
    if set(values) - current.keys(): raise ValueError('Installed receiver does not support all requested settings')
    state = {'schema': 1, 'before': {key: current[key]['value'] for key in values},
             'requested': values, 'completed': [], 'time': int(time.time())}
    validate(state['before']); save_state(state_path, state)
    for key, value in values.items():
        receiver.set(key, value)
        state['completed'].append(key); save_state(state_path, state)
    actual = receiver.settings()
    if any(actual[key]['value'] != value for key, value in values.items()):
        raise RuntimeError('Verification failed; restore using the saved state file')
    state['verified'] = True; save_state(state_path, state)

def download(output):
    output.parent.mkdir(parents=True, exist_ok=True)
    fd, temporary = tempfile.mkstemp(dir=output.parent, prefix='.download-')
    try:
        digest = hashlib.sha256(); count = 0
        with os.fdopen(fd, 'wb') as out, urllib.request.urlopen(UPSTREAM['apk_url'], timeout=30) as response:
            while True:
                chunk = response.read(1024 * 1024)
                if not chunk: break
                count += len(chunk)
                if count > 100 * 1024 * 1024: raise ValueError('Unexpected download size')
                digest.update(chunk); out.write(chunk)
        if digest.hexdigest() != UPSTREAM['apk_sha256']: raise ValueError('Upstream APK hash mismatch; file was not installed')
        os.replace(temporary, output)
    finally:
        if os.path.exists(temporary): os.unlink(temporary)
    print('Verified upstream APK:', output)

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--adb', default='adb'); parser.add_argument('--serial'); parser.add_argument('--port', type=int, default=5037)
    commands = parser.add_subparsers(dest='command', required=True)
    fetch = commands.add_parser('download'); fetch.add_argument('--output', type=Path, default=ROOT / '.cache/AirPlay-0.0.36.apk')
    commands.add_parser('status')
    setup = commands.add_parser('apply'); setup.add_argument('--profile', choices=['balanced', 'low-latency'], default='balanced'); setup.add_argument('--name'); setup.add_argument('--dry-run', action='store_true')
    undo = commands.add_parser('restore'); undo.add_argument('state', type=Path)
    args = parser.parse_args()
    if args.command == 'download': download(args.output); return
    if args.command == 'apply':
        values = json.loads((ROOT / 'profiles' / (args.profile + '.json')).read_text())
        if args.name is not None: values['server_name'] = args.name
        validate(values)
        if args.dry_run: print(json.dumps(values, indent=2, ensure_ascii=False)); return
    receiver = Receiver(args.adb, args.serial, args.port)
    if args.command == 'status':
        current = receiver.settings(); print(json.dumps({k: v['value'] for k, v in current.items() if k in KEYS}, indent=2, ensure_ascii=False)); return
    if args.command == 'restore':
        state = json.loads(args.state.read_text())
        if state.get('schema') != 1: raise ValueError('Unsupported saved state')
        values = validate(state['before'])
    path = ROOT / '.state' / (time.strftime('%Y%m%d-%H%M%S') + '-' + uuid.uuid4().hex[:8] + '.json')
    print('Undo state path (saved before any changes):', path, flush=True)
    apply(receiver, values, path)
    print('Verified. Private undo state:', path)
    print('Restore with: python3 airplay/manage.py --serial YOUR_TV restore ' + str(path))

if __name__ == '__main__': main()
