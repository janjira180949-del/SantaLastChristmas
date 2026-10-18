"""Compare complete authored metadata with the authenticated pre-Phase 8 archive."""
import hashlib
import json
from pathlib import Path
import zipfile

root = Path(__file__).resolve().parents[1]
with zipfile.ZipFile(root / 'docs/JOURNEY_HOME_PREVIOUS_SOURCES.zip') as archive:
    raw = archive.read('assets/world/map_data.json')
baseline = json.loads((root / 'docs/JOURNEY_HOME_BASELINE.json').read_text())
assert hashlib.sha256(raw).hexdigest() == baseline['assets/world/map_data.json']
before = json.loads(raw)
assert before['maps']['village']['exit'].pop('to_santa_base') == [12, 22]
after = json.loads((root / 'assets/world/map_data.json').read_text())
assert after['maps']['christmas_town']['npc'].pop('Elias') == [55, 43] # Exact authorized keeper relocation.
assert after['maps']['village']['landmarks'].pop('Christmas Tree') == [40,27]
assert after['maps']['village']['npc']['Toby'] == [40,31]
after['maps']['village']['npc']['Toby'] = [16,22] # Authenticated plaza placement projects to the original Phase 8 anchor.
assert before == after, 'Unrelated map metadata changed'
print('PASS: complete four-map equality after exact authorized exit/tree/Toby projections')
