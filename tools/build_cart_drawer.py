#!/usr/bin/env python3
"""Turn Figma screen 06-cart into a real bottom-sheet drawer over 04-shopping.

Run AFTER tools/apply_products.py (that script rebuilds pages/ from pages-figma/).

What it does
------------
1. Captures the 06-cart artboard (dark scrim + 900x750 bottom panel) from
   pages/06-cart.html into partials/cart-drawer-source.json (one-time).
2. Prefixes every Figma class .fN -> .cdfN so the drawer CSS cannot collide
   with 04-shopping's own .f1 ... .f265 rules.
3. Writes kiosk-cart.css (prefixed rules + open/close motion).
4. Injects the drawer DOM into pages/04-shopping.html (idempotent, marked).
5. Rebuilds pages/06-cart.html as "shopping page backdrop + auto-open drawer",
   so opening 06-cart.html directly shows exactly the same sheet.

pages-static/ and pages-figma/ keep the original standalone artboard.
"""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PARTIALS = ROOT / 'partials'
if not PARTIALS.exists():
    PARTIALS.mkdir()
SOURCE = PARTIALS / 'cart-drawer-source.json'
SHOP = ROOT / 'pages' / '04-shopping.html'
CART = ROOT / 'pages' / '06-cart.html'
CSS_OUT = ROOT / 'kiosk-cart.css'

LINK_TAG = '<link rel="stylesheet" href="../kiosk-cart.css">'
MARK_A = '<!-- CART_DRAWER_START -->'
MARK_B = '<!-- CART_DRAWER_END -->'


def classify(m):
    # "f82" -> "cdf82" (keeps the f, avoids clashing with 04-shopping's .f82)
    tokens = ['cd' + t if re.fullmatch(r'f\d+', t) else t for t in m.group(1).split()]
    return 'class="' + ' '.join(tokens) + '"'


def prefix_css(css):
    return re.sub(r'(?<![\w-])\.f(\d+)\b', r'.cdf\1', css)


def capture_source():
    """Extract the pristine artboard body + its .fN rules from 06-cart.html."""
    s = CART.read_text()
    style = re.search(r'<style>(.*?)</style>', s, re.S).group(1)
    rules = [ln for ln in style.splitlines() if re.match(r'^\.f\d+\{', ln.strip())]
    body = re.search(
        r'<div id="stage"><div class="screen">(.*)</div></div>\s*<script',
        s, re.S).group(1)
    data = {'rules': '\n'.join(rules), 'body': body}
    SOURCE.write_text(json.dumps(data, ensure_ascii=False, indent=1))
    return data


def build_drawer_html(data):
    html = re.sub(r'class="([^"]*)"', classify, data['body'])
    html = html.replace('<div class="cdf1">',
                        '<div class="cdf1" id="k-cart-drawer">', 1)
    html = html.replace('<div class="cdf2"></div>',
                        '<div class="cdf2 k-cart-scrim" id="k-cart-scrim"></div>', 1)
    html = html.replace('<div class="cdf3">',
                        '<div class="cdf3 k-cart-panel" id="k-cart-panel">', 1)
    html = html.replace('<div class="cdf5">',
                        '<div class="cdf5 k-cart-drag" id="k-cart-drag">', 1)
    html = html.replace('<div class="cdf13">',
                        '<div class="cdf13 k-cart-x" id="k-cart-x">', 1)
    # Stable hooks for the state machine (no coordinate guessing).
    html = html.replace('<span class="tx">4 items in your cart</span>',
                        '<span class="tx" data-cart-count>4 items in your cart</span>', 1)
    html = html.replace('<span class="tx">$14.50</span>',
                        '<span class="tx" data-cart-subtotal>$14.50</span>', 1)
    html = html.replace('<span class="tx">$5.66</span>',
                        '<span class="tx" data-cart-total>$5.66</span>', 1)
    n = [0]

    def qty(m):
        out = '<span class="tx" data-cart-qty="%d">%s</span>' % (n[0], m.group(1))
        n[0] += 1
        return out

    html = re.sub(r'<span class="tx">(\d+)</span>', qty, html)
    return MARK_A + html + MARK_B


MOTION = """
/* ---------- bottom-sheet behaviour ---------- */
#k-cart-drawer{
  position:absolute;left:0;top:0;width:1080px;height:1920px;overflow:hidden;
  z-index:8000;pointer-events:none;
}
#k-cart-drawer .k-cart-scrim,
#k-cart-drawer .k-cart-panel{pointer-events:none}
#k-cart-drawer.on{pointer-events:auto}
#k-cart-drawer.on .k-cart-scrim,
#k-cart-drawer.on .k-cart-panel{pointer-events:auto}

/* The Figma export made the scrim opaque; a sheet needs the shopping page
   to remain visible (and dimmed) behind it. */
.cdf2{background-color:rgba(26,18,11,.50)!important}
.k-cart-scrim{opacity:0;transition:opacity .34s ease;cursor:pointer}
#k-cart-drawer.on .k-cart-scrim{opacity:1}
.k-cart-panel{
  transform:translateY(100%);
  transition:transform .46s cubic-bezier(.22,.72,.24,1);
  will-change:transform;
}
#k-cart-drawer.on .k-cart-panel{transform:translateY(0)}
.k-cart-drag{cursor:grab;touch-action:none}
.k-cart-drag:active{cursor:grabbing}
.k-cart-x{cursor:pointer}

/* While the sheet is open, only its own hotspots may receive clicks. */
#k-layer.cart-open .k-hit:not(.k-cart-hit){pointer-events:none!important}
#k-layer .k-hit.k-cart-hit{pointer-events:none}
#k-layer.cart-open .k-hit.k-cart-hit{pointer-events:auto}

/* Swipe affordances live in the debug console, keeping the artboard 1:1. */
@media (prefers-reduced-motion:reduce){
  .k-cart-panel,.k-cart-scrim{transition:none!important}
}
"""


def inject_shop(drawer_html):
    s = SHOP.read_text()
    # idempotent: strip a previous injection first
    s = re.sub(re.escape(MARK_A) + r'.*?' + re.escape(MARK_B), '', s, flags=re.S)
    s = s.replace(LINK_TAG + '\n', '').replace(LINK_TAG, '')
    s = s.replace('<link rel="stylesheet" href="../kiosk.css">',
                  '<link rel="stylesheet" href="../kiosk.css">\n' + LINK_TAG)
    # Insert as the last child of #stage (after .screen, before #stage closes),
    # so it shares the artboard coordinate system with #k-layer.
    i = s.index('<script src="../kiosk.js"')
    head, tail = s[:i], s[i:]
    stripped = head.rstrip()
    assert stripped.endswith('</div></div>'), 'unexpected shopping page tail'
    pos = head.rfind('</div>')
    head = head[:pos] + drawer_html + head[pos:]
    SHOP.write_text(head + tail)


def rebuild_cart():
    s = SHOP.read_text()
    s = re.sub(r'<title>.*?</title>',
               '<title>Cart / 购物车抽屉 · Jetinno Coffee Kiosk</title>', s, count=1)
    s = s.replace('data-screen="04-shopping"',
                  'data-screen="06-cart" data-drawer-open="1"', 1)
    CART.write_text(s)


def main():
    data = json.loads(SOURCE.read_text()) if SOURCE.exists() else capture_source()
    rules = prefix_css(data['rules'])
    # The rules used to live inline in pages/*.html; as an external stylesheet
    # they resolve from the project root, not from pages/.
    rules = rules.replace('url(../assets/', 'url(assets/')
    CSS_OUT.write_text(
        '/* Generated by tools/build_cart_drawer.py — do not edit by hand.\n'
        '   Source: Figma frame 97:763 (screen 06-cart). */\n'
        + rules + '\n' + MOTION)
    drawer = build_drawer_html(data)
    inject_shop(drawer)
    rebuild_cart()
    print('source      :', SOURCE.relative_to(ROOT))
    print('css         :', CSS_OUT.name, len(rules.splitlines()), 'prefixed rules')
    print('drawer html :', len(drawer), 'bytes')
    print('injected    : pages/04-shopping.html')
    print('rebuilt     : pages/06-cart.html (shopping backdrop + auto-open sheet)')


if __name__ == '__main__':
    main()
