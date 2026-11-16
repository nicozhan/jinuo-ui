#!/usr/bin/env python3
"""Build the local product catalog from the user's source images.

- copies the 17 source files into assets/products/original/  (untouched)
- renders unified 640x640 card images into assets/products/card/<id>.png
- renders 1024x1024 hero images into assets/products/hero/<id>.png
- writes a contact sheet for visual review

Usage: python3 tools/build_catalog.py
"""
import json, os, shutil
from collections import deque
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = '/Users/qian2/Downloads/coffee/assets'
OUT = os.path.join(HERE, 'assets', 'products')
CARD_BG = (247, 243, 236)          # #F7F3EC, matches the artboard card surface
CARD, HERO = 640, 1024

products = json.load(open(os.path.join(HERE, 'products.json')))['products']
for sub in ('original', 'card', 'hero'):
    os.makedirs(os.path.join(OUT, sub), exist_ok=True)


def copy_original(fname):
    s = os.path.join(SRC, fname)
    d = os.path.join(OUT, 'original', fname)
    if not os.path.exists(d):
        shutil.copy2(s, d)
    return d


def load_rgba(path):
    return Image.open(path).convert('RGBA')


def flood_background(im, max_side=256):
    """Return an alpha mask with the contiguous studio background removed.

    Works on a downscaled copy; accepts pixels that are close to the sampled
    corner colour OR very light/low-saturation, but only when the colour step
    from the already-accepted neighbour is small (keeps gradients, protects
    the product silhouette).
    """
    w, h = im.size
    k = max_side / max(w, h)
    sm = im.resize((max(1, int(w * k)), max(1, int(h * k))), Image.LANCZOS).convert('RGB')
    W, H = sm.size
    px = sm.load()
    corners = [px[0, 0], px[W - 1, 0], px[0, H - 1], px[W - 1, H - 1]]
    corner = tuple(sum(c[i] for c in corners) // 4 for i in range(3))

    def dist(a, b):
        return ((a[0] - b[0]) ** 2 + (a[1] - b[1]) ** 2 + (a[2] - b[2]) ** 2) ** .5

    def eligible(c):
        mx, mn = max(c), min(c)
        bright = (c[0] + c[1] + c[2]) / 3
        sat = mx - mn
        return dist(c, corner) <= 55 or (bright >= 195 and sat <= 68)

    seen = bytearray(W * H)
    q = deque()
    for x in range(W):
        for y in (0, H - 1):
            i = y * W + x
            if not seen[i] and eligible(px[x, y]):
                seen[i] = 1; q.append((x, y))
    for y in range(H):
        for x in (0, W - 1):
            i = y * W + x
            if not seen[i] and eligible(px[x, y]):
                seen[i] = 1; q.append((x, y))
    while q:
        x, y = q.popleft()
        c = px[x, y]
        for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
            if nx < 0 or ny < 0 or nx >= W or ny >= H:
                continue
            i = ny * W + nx
            if seen[i]:
                continue
            nc = px[nx, ny]
            if eligible(nc) and dist(nc, c) <= 30:
                seen[i] = 1; q.append((nx, ny))

    removed = sum(seen) / float(W * H)
    if removed < 0.08:                 # couldn't isolate a background → keep the photo
        return None, removed
    mask = Image.new('L', (W, H), 255)
    mask.putdata([255 if v == 0 else 0 for v in seen])
    mask = mask.resize((w, h), Image.LANCZOS)
    return mask, removed


def prepare(path):
    """Return a product RGBA with a clean CARD_BG studio background."""
    im = load_rgba(path)
    alpha = im.getchannel('A')
    has_alpha = alpha.getextrema()[0] < 250
    if has_alpha:
        bg = Image.new('RGBA', im.size, CARD_BG + (255,))
        bg.alpha_composite(im)
        return bg.convert('RGB'), 'alpha'
    mask, ratio = flood_background(im)
    if mask is None:
        return im.convert('RGB'), 'photo'
    im.putalpha(mask)
    bg = Image.new('RGBA', im.size, CARD_BG + (255,))
    bg.alpha_composite(im)
    return bg.convert('RGB'), 'flood %.0f%%' % (ratio * 100)


def fit_square(im, size, pad):
    w, h = im.size
    k = min((size - 2 * pad) / w, (size - 2 * pad) / h)
    nw, nh = max(1, int(w * k)), max(1, int(h * k))
    inner = im.resize((nw, nh), Image.LANCZOS)
    cv = Image.new('RGB', (size, size), CARD_BG)
    cv.paste(inner, ((size - nw) // 2, (size - nh) // 2))
    return cv


report = []
for i, p in enumerate(products):
    src = copy_original(p['file'])
    clean, how = prepare(src)
    card = fit_square(clean, CARD, 26)
    card.save(os.path.join(OUT, 'card', p['id'] + '.png'), optimize=True)
    hero_src = copy_original(p['hero']) if p.get('hero') else src
    hero_clean, hero_how = prepare(hero_src)
    hero = fit_square(hero_clean, HERO, 70)
    hero.save(os.path.join(OUT, 'hero', p['id'] + '.png'), optimize=True)
    report.append((p['id'], p['file'], how, hero_how))

# contact sheet
W, H = 168, 192
sheet = Image.new('RGB', (W * 4, (H + 24) * 4), '#EFEAE3')
dr = ImageDraw.Draw(sheet)
for i, p in enumerate(products):
    im = Image.open(os.path.join(OUT, 'card', p['id'] + '.png'))
    im.thumbnail((W - 8, W - 8))
    x = (i % 4) * W + (W - im.width) // 2
    y = (i // 4) * (H + 24)
    sheet.paste(im, (x, y))
    dr.text(((i % 4) * W + 5, y + H - 20), '%02d %s' % (i + 1, p['id'][:22]), fill='#3A2A1E')
sheet.save(os.path.join(HERE, 'product-catalog-sheet.png'))

for r in report:
    print('%-28s %-40s %-12s hero:%s' % r)
print('card/hero/original ready; sheet: product-catalog-sheet.png')
