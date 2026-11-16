#!/usr/bin/env python3
"""Keep kiosk.js in sync with products.json (single source of truth).

Injects the CATALOG constant between the CATALOG_START / CATALOG_END markers.
file:// pages cannot fetch JSON, so the catalog must be inlined in the shared script.
Usage: python3 tools/sync_kiosk_catalog.py
"""
import json, os, re

HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
data = json.load(open(os.path.join(HERE, 'products.json')))
products = data['products']
currency = data.get('currency', 'CHF')
default_price = float(data.get('defaultPrice', 3.50))

items = []
for p in products:
    items.append({
        'id': p['id'], 'name': p['name'], 'short': p['short'], 'cn': p['cn'],
        'cat': p['cat'], 'kind': p.get('kind', 'classic'),
        'price': float(p.get('price', default_price)),
        'card': 'assets/products/card/%s.png' % p['id'],
        'hero': 'assets/products/hero/%s.png' % p['id']
    })

block = ('  /* CATALOG_START */\n'
         '  var CURRENCY = ' + json.dumps(currency) + ';\n'
         '  var DEFAULT_PRICE = ' + json.dumps(default_price) + ';\n'
         '  var CATALOG = ' + json.dumps(items, ensure_ascii=False, separators=(',', ':')) + ';\n'
         '  var CATS = [\'CLASSICS\', \'ESPRESSO\', \'MILK\', \'TEA\', \'FRAPPE\'];\n'
         '  function byId(id) {\n'
         '    for (var i = 0; i < CATALOG.length; i++) if (CATALOG[i].id === id) return CATALOG[i];\n'
         '    return CATALOG[0];\n'
         '  }\n'
         '  function listFor(cat) {\n'
         '    var own = CATALOG.filter(function (p) { return p.cat === cat; });\n'
         '    var rest = CATALOG.filter(function (p) { return p.cat !== cat; });\n'
         '    return own.concat(rest).slice(0, 9);\n'
         '  }\n'
         '  /* CATALOG_END */')

path = os.path.join(HERE, 'kiosk.js')
s = open(path).read()
if '/* CATALOG_START */' in s:
    s = re.sub(r'/\* CATALOG_START \*/.*?/\* CATALOG_END \*/', lambda m: block, s, flags=re.S)
else:
    anchor = "  var SLIDES_DEFAULT = ["
    s = s.replace(anchor, block + '\n\n' + anchor, 1)
open(path, 'w').write(s)
print('kiosk.js: %d products synced' % len(items))
