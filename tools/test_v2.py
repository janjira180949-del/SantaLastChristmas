"""Compile every production/test source and run every standalone regression test."""
from pathlib import Path
import json
import os
import re
import shutil
import subprocess
import sys
import time

root = Path(__file__).resolve().parents[1]
# Optional validation storage keeps compilation/logs off a full system drive.
# Runtime cwd and every production/test source remain in the original project.
validation_root = Path(os.environ.get('SANTA_TEST_OUTPUT', str(root / 'build/v2'))).resolve()
output = (validation_root / 'classes').resolve()
assert output.is_relative_to(validation_root) and output == validation_root / 'classes'
assert validation_root != Path(validation_root.anchor) and output.name == 'classes'
test_cwd = Path(os.environ.get('SANTA_TEST_CWD', str(root))).resolve()
# An alternate writable cwd must still use this project's actual sources and artwork.
assert (test_cwd / 'src').resolve() == (root / 'src').resolve()
assert (test_cwd / 'assets').resolve() == (root / 'assets').resolve()
if output.exists():
    shutil.rmtree(output)
output.mkdir(parents=True)
logs = validation_root / 'logs'
logs.mkdir(parents=True, exist_ok=True)
sources = sorted(root.glob('src/**/*.java')) + sorted(root.glob('tests/*.java'))
command = ['javac', '--release', '17', '-encoding', 'UTF-8', '-Xlint:all', '-d', str(output)]
compiled = subprocess.run(command + [str(p) for p in sources], cwd=root, capture_output=True, text=True)
(logs / 'compile.log').write_text(compiled.stdout + compiled.stderr, encoding='utf-8')
print(f'Complete compilation: {len(sources)} Java files, exit {compiled.returncode}', flush=True)
if compiled.returncode:
    print(compiled.stderr)
    sys.exit(compiled.returncode)
results = []
for source in sorted(root.glob('tests/*Test.java')):
    package = re.search(r'^package\s+(\w+);', source.read_text(encoding='utf-8'), re.M)
    name = (package.group(1) + '.' if package else '') + source.stem
    start = time.monotonic()
    # LaunchSmokeTest intentionally opens the real JFrame; other tests render headlessly.
    command = ['java', '-Xmx1500m']
    if name != 'LaunchSmokeTest':
        command.append('-Djava.awt.headless=true')
    command += ['-cp', str(output), name]
    try:
        result = subprocess.run(command, cwd=test_cwd, capture_output=True, text=True, timeout=120)
        report = result.stdout + result.stderr
        exit_code = result.returncode
    except subprocess.TimeoutExpired as error:
        report = f'Timed out after 120 seconds: {error}'
        exit_code = -1
    (logs / (source.stem + '.log')).write_text(report, encoding='utf-8')
    results.append({'test': name, 'exit_code': exit_code, 'seconds': round(time.monotonic() - start, 2)})
    print(f'{name}: {"PASS" if exit_code == 0 else "FAIL"}', flush=True)
    if exit_code:
        print(report[-1600:], flush=True)
(validation_root / 'test-results.json').write_text(json.dumps(results, indent=2), encoding='utf-8')
passed = sum(result['exit_code'] == 0 for result in results)
print(f'{passed}/{len(results)} regression entry points passed.', flush=True)
sys.exit(0 if passed == len(results) else 1)
