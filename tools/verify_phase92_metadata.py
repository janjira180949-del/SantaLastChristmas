"""Require complete metadata equality after removing the one authorized tree landmark."""
import json
from pathlib import Path
import zipfile

root = Path(__file__).resolve().parents[1]
with zipfile.ZipFile(root / 'docs/PHASE9_2_PREVIOUS_SOURCES.zip') as archive:
    before = json.loads(archive.read('assets/world/map_data.json'))
current = json.loads((root / 'assets/world/map_data.json').read_text(encoding='utf-8'))
assert current['maps']['christmas_town']['npc'].pop('Elias') == [55, 43] # Exact authorized keeper relocation.
assert current['maps']['village']['landmarks'].pop('Christmas Tree') == [40, 27]
assert current['maps']['village']['npc']['Toby'] == [40, 31]
assert before['maps']['village']['npc']['Toby'] == [16, 24]
current['maps']['village']['npc']['Toby'] = before['maps']['village']['npc']['Toby']
assert current == before, 'An unrelated marker, collider, region, map size or gate changed'
print('PASS: tree landmark and authenticated Toby placement projected; all other metadata exactly matches prior maps')
