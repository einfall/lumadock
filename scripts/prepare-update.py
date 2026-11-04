"""Prepare public update metadata. No credentials or signing key are read."""
import argparse
import hashlib
import json
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument('apk', type=Path)
parser.add_argument('--version', required=True)
parser.add_argument('--code', required=True, type=int)
parser.add_argument('--notes', default='Luma Dock update')
parser.add_argument('--output', type=Path, default=Path('release-output'))
args = parser.parse_args()
assert args.code > 0 and args.apk.is_file()
assert all(c.isalnum() or c in '.-' for c in args.version)
name = 'Luma-Dock-' + args.version + '-universal.apk'
data = args.apk.read_bytes()
digest = hashlib.sha256(data).hexdigest()
manifest = {
    'schema_version': 1, 'package_name': 'com.gathin.tvhome',
    'version_name': args.version, 'version_code': args.code,
    'minimum_sdk': 26, 'release_notes': args.notes,
    'artifacts': {'universal': {
        'url': 'https://github.com/einfall/lumadock/releases/download/v' + args.version + '/' + name,
        'sha256': digest, 'size_bytes': len(data),
    }},
}
args.output.mkdir(parents=True, exist_ok=True)
(args.output / name).write_bytes(data)
raw = (json.dumps(manifest, ensure_ascii=False, indent=2) + '\n').encode()
(args.output / 'update.json').write_bytes(raw)
(args.output / 'SHA256SUMS').write_text(
    digest + '  ' + name + '\n' + hashlib.sha256(raw).hexdigest() + '  update.json\n',
    encoding='utf-8',
)
print('Prepared release assets:', args.output)
