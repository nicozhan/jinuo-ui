#!/usr/bin/env python3
"""Replace the Figma placeholder products with the user's real product catalog.

Generated pages keep a 1:1 correspondence with Figma nodes:
  * every TEXT node  -> one <span class="tx"> in document order
  * every IMAGE fill -> one CSS class with background-image, in document order
This script uses that correspondence plus absolute artboard coordinates to
replace each product slot, image and label without touching layout, type or
price, and tags the nodes with data-* attributes for kiosk.js.

Idempotent: it always rebuilds pages/ from pages-figma/ (the pristine
interactive replica captured before the first replacement).

Usage: python3 tools/apply_products.py
"""
import json, os, re, shutil

HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
P = json.load(open(os.path.join(HERE, 'products.json')))['products']
FIGMA = json.load(open(os.path.join(HERE, 'file-paths.json')))
SRC_DIR = os.path.join(HERE, 'pages-figma')
DST_DIR = os.path.join(HERE, 'pages')

OLD_04 = {'Americano', 'Vanilla Sweet Cream Nitro Cold Brew',
          'Moka glacé au chocolat blanc', 'Espresso', 'Moka'}
OLD_05 = {'Americano', 'Fresh Ground Coffee', 'Espresso', 'Ristretto', 'Macchiato',
          'Café Latte', 'Cappuccino', 'Latte Macchiato', 'Flat White', 'Milk Coffee',
          'Milk Americano', 'Iced Latte', 'Iced Americano', 'Hot Milk', 'Hot Water',
          'Hot Chocolate'}
OLD_06 = {'Americano', 'Espresso', 'Cappuccino'}
OLD_19 = {'Espresso', 'Cappuccino', 'Latte'}

SPECS = [
    dict(slug='04-shopping', fid='172:868', old=OLD_04, kind='slot',
         xs=[64, 390, 715], ys=[656, 1066, 1500], count=9),
    dict(slug='05-custom', fid='1:1488', old=OLD_05, kind='slot',
         xs=[64, 390, 715], ys=[377, 909, 1441, 1909, 2442, 2974], count=16),
    dict(slug='06-cart', fid='97:763', old=OLD_06, kind='cart', count=3),
    dict(slug='19-order-confirm', fid='263:1294', old=OLD_19, kind='order', count=3),
]

NO_PRODUCT_PAGES = ['01-home', '02-lang', '03-help', '11-card', '15-wrong',
                    '16-pay', '17-making', '18-ready']


def frame(fid):
    return next(x for x in FIGMA['document']['children'][0]['children'] if x['id'] == fid)


def collect(f):
    O = f['absoluteBoundingBox']
    out = {'text': [], 'image': []}

    def walk(n):
        bb = n.get('absoluteBoundingBox') or {}
        x, y = bb.get('x', 0) - O['x'], bb.get('y', 0) - O['y']
        w, h = bb.get('width', 0), bb.get('height', 0)
        if n['type'] == 'TEXT':
            out['text'].append(dict(t=n.get('characters', ''), x=x, y=y, w=w, h=h,
                                    fs=(n.get('style') or {}).get('fontSize', 0)))
        for fl in (n.get('fills') or []):
            if fl.get('type') == 'IMAGE':
                out['image'].append(dict(ref=str(fl.get('imageRef'))[:8], x=x, y=y, w=w, h=h))
        for c in n.get('children', []):
            walk(c)
    walk(f)
    return out


def nearest_col(x, xs):
    centers = [v + 150 for v in xs]
    return min(range(len(centers)), key=lambda i: abs(x - centers[i]))


def row_of(y, ys):
    cands = [i for i, v in enumerate(ys) if v - 45 <= y <= v + 400]
    return max(cands) if cands else 0


def slot_of(x, y, xs, ys):
    return (row_of(y, ys), nearest_col(x, xs))


def has_cjk(s):
    return any('\u3400' <= ch <= '\u9fff' for ch in s)


def tag_class(html, cls, attr):
    pat = 'class="%s"' % cls
    n = html.count(pat)
    if n != 1:
        raise RuntimeError('class %s appears %d times (expected 1)' % (cls, n))
    return html.replace(pat, '%s %s' % (pat, attr), 1)


def add_class(html, cls, extra):
    pat = 'class="%s"' % cls
    n = html.count(pat)
    if n != 1:
        raise RuntimeError('class %s appears %d times (expected 1)' % (cls, n))
    return html.replace(pat, 'class="%s %s"' % (cls, extra), 1)


def run(spec):
    slug, f = spec['slug'], frame(spec['fid'])
    nodes = collect(f)
    html = open(os.path.join(SRC_DIR, slug + '.html')).read()

    css_start, css_end = html.find('<style>'), html.find('</style>')
    css, body = html[css_start:css_end], html[css_end:]
    img_matches = list(re.finditer(r'background-image:url\([^)]*\.\./assets/img/([0-9a-f]{8})[^)]*\)', css))
    txt_matches = list(re.finditer(r'<span class="tx">(.*?)</span>', body, re.S))
    img_cls = []
    for m in img_matches:
        opener = css.rfind('.f', 0, m.start())
        cls = re.match(r'\.([a-zA-Z0-9]+)\{', css[opener:])
        assert cls, (slug, 'cannot find owning class')
        img_cls.append(cls.group(1))

    assert len(img_matches) == len(nodes['image']), (slug, 'image count')
    assert [m.group(1) for m in img_matches] == [i['ref'] for i in nodes['image']], (slug, 'image order')
    assert len(txt_matches) == len(nodes['text']), (slug, 'text count')
    assert [m.group(1) for m in txt_matches] == [t['t'] for t in nodes['text']], (slug, 'text order')

    xs, ys = spec.get('xs'), spec.get('ys')
    if xs:
        grid = [i for i, im in enumerate(nodes['image']) if 280 <= im['w'] <= 320 and 280 <= im['h'] <= 320]
        positions = sorted({slot_of(nodes['image'][i]['x'], nodes['image'][i]['y'], xs, ys) for i in grid})
        slotmap = {pos: n for n, pos in enumerate(positions)}
        assert len(positions) == spec['count'], (slug, 'slots', len(positions))
    else:
        grid = [i for i, im in enumerate(nodes['image']) if 70 <= im['w'] <= 90 and 70 <= im['h'] <= 90]
        assert len(grid) == spec['count'], (slug, 'thumbnails', len(grid))
        slotmap = None
    by_y = sorted(grid, key=lambda i: nodes['image'][i]['y'])

    def img_slot(i):
        if slotmap:
            return slotmap[slot_of(nodes['image'][i]['x'], nodes['image'][i]['y'], xs, ys)]
        return by_y.index(i)

    # ---------- CSS edits ----------
    edits_css, tag_edits = [], []
    for i in grid:
        slot = img_slot(i)
        m = img_matches[i]
        rep = 'background-image:url(../assets/products/card/%s.png)' % P[slot]['id']
        edits_css.append((m.start(), m.end(), rep))
        attr = {'slot': 'data-slot="%d"' % slot,
                'cart': 'data-cart-row="%d"' % slot,
                'order': 'data-order-row="%d"' % slot}[spec['kind']]
        tag_edits.append((img_cls[i], 'attr', attr))

    if slug == '05-custom':
        hero_i = next(i for i, im in enumerate(nodes['image']) if im['w'] > 600 and 300 <= im['h'] <= 500)
        cls = img_cls[hero_i]
        rule = re.search(r'\.%s\{([^}]*)\}' % cls, css)
        assert rule, 'hero rule missing'
        decl = re.sub(r'background-image:url\([^)]*\)',
                      'background-image:url(../assets/products/hero/%s.png)' % P[0]['id'], rule.group(1))
        decl = decl.replace('background-size:cover', 'background-size:contain') + 'background-color:#FFFFFF;'
        edits_css.append((rule.start(), rule.end(), '.%s{%s}' % (cls, decl)))
        tag_edits.append((cls, 'attr', 'data-modal-img'))

    # ---------- body text edits ----------
    edits_body = []
    same = [k for k, u in enumerate(nodes['text']) if u['t'] in spec['old']] if not xs else []
    for i, t in enumerate(nodes['text']):
        v = t['t']
        if slug == '05-custom':
            if v in spec['old'] and t['fs'] < 40:
                slot = slotmap[slot_of(t['x'], t['y'], xs, ys)]
                label, attr = P[slot]['short'], 'data-slot-name="%d"' % slot
            elif has_cjk(v) and t['fs'] <= 20 and t['y'] > 600:
                slot = slotmap[slot_of(t['x'], t['y'], xs, ys)]
                label, attr = P[slot]['cn'], 'data-slot-cn="%d"' % slot
            elif v == 'Espresso' and t['fs'] >= 40:
                label, attr = P[0]['short'], 'data-modal-name'
            else:
                continue
        else:
            if v not in spec['old']:
                continue
            slot = slotmap[slot_of(t['x'], t['y'], xs, ys)] if xs else same.index(i)
            label = P[slot]['short']
            attr = {'slot': 'data-slot-name="%d"' % slot,
                    'cart': 'data-cart-name="%d"' % slot,
                    'order': 'data-order-name="%d"' % slot}[spec['kind']]
        m = txt_matches[i]
        edits_body.append((m.start(), m.end(), '<span class="tx" %s>%s</span>' % (attr, label)))
        if slug in ('04-shopping', '05-custom') and attr.startswith('data-slot-'):
            parents = re.findall(r'<div class="(f\d+)"', body[:m.start()])
            if parents:
                tag_edits.append((parents[-1], 'class', 'k-pname-box'))
        elif attr == 'data-modal-name':
            parents = re.findall(r'<div class="(f\d+)"', body[:m.start()])
            if parents:
                tag_edits.append((parents[-1], 'class', 'k-modal-name-box'))

    for a, b, rep in sorted(edits_css, reverse=True):
        css = css[:a] + rep + css[b:]
    for a, b, rep in sorted(edits_body, reverse=True):
        body = body[:a] + rep + body[b:]
    done = set()
    for cls, mode, val in tag_edits:
        key = (cls, mode, val)
        if key in done:
            continue
        done.add(key)
        body = add_class(body, cls, val) if mode == 'class' else tag_class(body, cls, val)

    html = html[:css_start] + css + body
    open(os.path.join(DST_DIR, slug + '.html'), 'w').write(html)
    print('%-20s %2d images, %2d labels replaced' % (slug, len(grid), len(edits_body)))


def main():
    # Capture the pristine interactive replica once, then always rebuild from it.
    if not os.path.isdir(SRC_DIR):
        os.makedirs(SRC_DIR)
        for f in os.listdir(DST_DIR):
            if f.endswith('.html'):
                shutil.copy2(os.path.join(DST_DIR, f), os.path.join(SRC_DIR, f))
        print('captured pristine interactive pages -> pages-figma/')
    for f in os.listdir(SRC_DIR):
        if f.endswith('.html') and f[:-5] in NO_PRODUCT_PAGES:
            shutil.copy2(os.path.join(SRC_DIR, f), os.path.join(DST_DIR, f))
    for spec in SPECS:
        run(spec)
    print('done')


if __name__ == '__main__':
    main()
