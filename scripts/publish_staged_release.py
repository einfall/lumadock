"""GitHub Actions publisher for a prepared APK. Does not build with or receive signing keys."""
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import urllib.error
import urllib.parse
import urllib.request

repo = os.environ['GITHUB_REPOSITORY']
assert repo == 'einfall/lumadock', 'Publisher is restricted to the intended repository'
token = os.environ['GITHUB_TOKEN']
root = 'https://api.github.com/repos/' + repo

def api(method, path, body=None, binary=None):
    url = path if path.startswith('https://uploads.github.com/') else root + path
    assert url.startswith(root + '/') or url.startswith('https://uploads.github.com/repos/' + repo + '/')
    data = binary if binary is not None else None if body is None else json.dumps(body).encode()
    request = urllib.request.Request(url, data=data, method=method, headers={
        'Authorization': 'Bearer ' + token, 'Accept': 'application/vnd.github+json',
        'User-Agent': 'LumaDock-Release-Publisher',
        'Content-Type': 'application/octet-stream' if binary is not None else 'application/json',
        'X-GitHub-Api-Version': '2022-11-28',
    })
    with urllib.request.urlopen(request, timeout=120) as response:
        raw = response.read()
        return json.loads(raw) if raw else None

meta = json.loads(Path('_release/payload.json').read_text(encoding='utf-8'))
assert re.fullmatch(r'[0-9a-f]{40}', meta['source_sha'])
assert re.fullmatch(r'[0-9a-f]{64}', meta['sha256'])
assert re.fullmatch(r'[0-9a-f]{64}', meta['certificate_sha256'])
assert re.fullmatch(r'[0-9]+(?:\.[0-9]+){2}', meta['version'])
assert meta['version_code'] > 0
name = 'Luma-Dock-' + meta['version'] + '-universal.apk'
apk = Path('_release') / name
data = apk.read_bytes()
assert len(data) == meta['size_bytes']
assert hashlib.sha256(data).hexdigest() == meta['sha256']
head = api('GET', '/git/ref/heads/main')['object']['sha']
assert head == meta['source_sha'], 'Main changed after this release was prepared'
tools = Path(os.environ.get('ANDROID_HOME') or os.environ['ANDROID_SDK_ROOT']) / 'build-tools/34.0.0'
verify = subprocess.check_output([str(tools / 'apksigner'), 'verify', '--print-certs', str(apk)], text=True)
cert = re.search(r'certificate SHA-256 digest:\s*([a-fA-F0-9:]+)', verify)
assert cert and cert.group(1).replace(':', '').lower() == meta['certificate_sha256']
badging = subprocess.check_output([str(tools / 'aapt2'), 'dump', 'badging', str(apk)], text=True)
assert "name='com.gathin.tvhome'" in badging
assert "versionCode='" + str(meta['version_code']) + "'" in badging
assert "versionName='" + meta['version'] + "'" in badging
assert "sdkVersion:'26'" in badging
assert 'application-debuggable' not in badging
source = api('GET', '/contents/launcher/app/build.gradle?ref=' + meta['source_sha'])
gradle = base64.b64decode(source['content']).decode()
assert 'versionCode ' + str(meta['version_code']) in gradle
assert "versionName '" + meta['version'] + "'" in gradle

tag = 'v' + meta['version']
manifest = {
    'schema_version': 1, 'package_name': 'com.gathin.tvhome',
    'version_name': meta['version'], 'version_code': meta['version_code'],
    'minimum_sdk': 26, 'release_notes': meta['notes'],
    'artifacts': {'universal': {
        'url': 'https://github.com/' + repo + '/releases/download/' + tag + '/' + name,
        'sha256': meta['sha256'], 'size_bytes': len(data),
    }},
}
raw_manifest = (json.dumps(manifest, ensure_ascii=False, indent=2) + '\n').encode()
checksums = (meta['sha256'] + '  ' + name + '\n' + hashlib.sha256(raw_manifest).hexdigest() + '  update.json\n').encode()
try:
    release = api('GET', '/releases/tags/' + tag)
except urllib.error.HTTPError as error:
    if error.code != 404:
        raise
    release = api('POST', '/releases', {
        'tag_name': tag, 'target_commitish': meta['source_sha'],
        'name': 'Luma Dock ' + meta['version'], 'body': meta['notes'],
        'draft': True, 'prerelease': False,
    })
if not release['draft']:
    raise RuntimeError('A published release with this version already exists; it is not overwritten')
existing = {item['name']: item for item in release['assets']}
for asset_name, asset_bytes in [(name, data), ('update.json', raw_manifest), ('SHA256SUMS', checksums)]:
    if asset_name in existing:
        raise RuntimeError('A draft asset already exists: ' + asset_name)
    upload = 'https://uploads.github.com/repos/' + repo + '/releases/' + str(release['id']) + '/assets?name=' + urllib.parse.quote(asset_name)
    api('POST', upload, binary=asset_bytes)
published = api('PATCH', '/releases/' + str(release['id']), {'draft': False, 'make_latest': 'true'})
print('Published:', published['html_url'])
branch = os.environ.get('GITHUB_REF_NAME', '')
if branch.startswith('release-upload/'):
    current = api('GET', '/git/ref/heads/' + branch)['object']['sha']
    if current == os.environ['GITHUB_SHA']:
        api('DELETE', '/git/refs/heads/' + branch)
        print('Removed temporary upload branch')
