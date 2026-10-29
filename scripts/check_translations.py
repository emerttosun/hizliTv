"""Verify complete English/Turkish Android resources and format placeholders."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET
ROOT = Path(__file__).resolve().parents[1]
def catalog(folder):
    result = {}
    for path in folder.glob('*.xml'):
        for item in ET.parse(path).getroot():
            if item.tag == 'string': result[item.attrib['name']] = ''.join(item.itertext())
            elif item.tag == 'plurals':
                for child in item: result[item.attrib['name'] + '/' + child.attrib['quantity']] = child.text or ''
    return result
def placeholders(value): return sorted(re.findall(r'%(?:\d+\$)?[dsf]', value.replace('%%', '')))
if __name__ == '__main__':
    base = ROOT / 'tv-ui/src/main/res'
    en, tr = catalog(base / 'values'), catalog(base / 'values-tr')
    assert en.keys() == tr.keys(), ('Missing translations', en.keys() ^ tr.keys())
    for key in en: assert placeholders(en[key]) == placeholders(tr[key]), ('Format mismatch', key)
    print(f'English/Turkish resources: {len(en)} entries, matching keys and placeholders.')
