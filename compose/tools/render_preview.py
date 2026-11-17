#!/usr/bin/env python3
"""Static design render of the Compose 05-custom screen (NOT an HTML simulation).

    python3 tools/render_preview.py

This draws the exact same 1080x1920 artboard spec used by CustomScreen.kt so
the layout can be eyeballed without an Android device. The real deliverable is
the Compose source; this PNG is only a geometry reference.
"""
from __future__ import annotations

from pathlib import Path
import sys

from PIL import Image, ImageDraw, ImageFont

HERE = Path(__file__).resolve().parents[1]
RES = HERE / "app/src/main/res"
D = RES / "drawable-nodpi"
F = RES / "font"
PRODUCT_ID = sys.argv[1] if len(sys.argv) > 1 else "americano"
CARAMEL = PRODUCT_ID == "caramel-macchiato"
NAME = "Caramel Macchiato" if CARAMEL else "Americano"
OUT = HERE / ("preview/05-custom-caramel-compose.png" if CARAMEL else "preview/05-custom-compose.png")
OUT.parent.mkdir(parents=True, exist_ok=True)

W, H = 1080, 1920
INK = (58, 42, 30)
CREAM = (255, 253, 249)
SAND = (251, 248, 243)
CHIP = (243, 237, 228)
CARAMEL = (192, 122, 62)
RED = (160, 0, 45)
MUTED = (124, 102, 80)
OUTER = (26, 18, 11)


def font(name: str, size: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(str(F / name), size)


def fra(size: int, weight: int = 400) -> ImageFont.FreeTypeFont:
    return font(f"fraunces_{weight}.ttf", size)


def sans(size: int, weight: int = 400) -> ImageFont.FreeTypeFont:
    return font(f"instrument_sans_{weight}.ttf", size)


def spaced(draw: ImageDraw.ImageDraw, xy, text: str, fnt, fill, spacing: float = 0.0,
           centre: bool = False, right: bool = False):
    """Draw text with optional letter spacing; centre/right align the whole run.

    With zero spacing we draw the whole run at once, preserving the face's
    kerning pairs. Char-by-char is only used for tracked uppercase UI labels.
    """
    x, y = xy
    if spacing == 0.0:
        total = draw.textlength(text, font=fnt)
        if centre:
            x -= total / 2
        elif right:
            x -= total
        draw.text((x, y), text, font=fnt, fill=fill, anchor="lt")
        return total
    widths = [draw.textlength(ch, font=fnt) for ch in text]
    total = sum(widths) + spacing * max(0, len(text) - 1)
    if centre:
        x -= total / 2
    elif right:
        x -= total
    for ch, cw in zip(text, widths):
        draw.text((x, y), ch, font=fnt, fill=fill, anchor="lt")
        x += cw + spacing
    return total


def rounded_image(path: Path, size: tuple[int, int], radius: int) -> Image.Image:
    im = Image.open(path).convert("RGBA")
    im = im.resize(size, Image.LANCZOS)
    mask = Image.new("L", size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, size[0] - 1, size[1] - 1), radius, fill=255)
    out = Image.new("RGBA", size, (0, 0, 0, 0))
    out.paste(im, (0, 0), mask)
    return out


def fit_image(path: Path, box: tuple[int, int]) -> Image.Image:
    """ContentScale.Fit inside a SAND-coloured box."""
    bw, bh = box
    src = Image.open(path).convert("RGBA")
    ratio = min(bw / src.width, bh / src.height)
    nw, nh = int(src.width * ratio), int(src.height * ratio)
    src = src.resize((nw, nh), Image.LANCZOS)
    layer = Image.new("RGBA", (bw, bh), SAND + (255,))
    layer.alpha_composite(src, ((bw - nw) // 2, (bh - nh) // 2))
    return layer


def crop_image(path: Path, size: tuple[int, int]) -> Image.Image:
    """ContentScale.Crop."""
    bw, bh = size
    src = Image.open(path).convert("RGBA")
    ratio = max(bw / src.width, bh / src.height)
    nw, nh = int(src.width * ratio), int(src.height * ratio)
    src = src.resize((nw, nh), Image.LANCZOS)
    return src.crop(((nw - bw) // 2, (nh - bh) // 2, (nw - bw) // 2 + bw, (nh - bh) // 2 + bh))


# --- catalogue (same order as Catalog.kt / kiosk.js) --------------------------------
CATALOG = [
    ("americano", "Americano", "CLASSICS"),
    ("caramel-macchiato", "Caramel Macchiato", "CLASSICS"),
    ("coconut-mocha", "Coconut Mocha", "CLASSICS"),
    ("espresso", "Espresso", "ESPRESSO"),
    ("luckin-americano", "Luckin Americano", "ESPRESSO"),
    ("luckin-caffe-mocha", "Luckin Caffe Mocha", "MILK"),
    ("luckin-cappuccino", "Luckin Cappuccino", "MILK"),
    ("luckin-latte", "Luckin Latte", "MILK"),
    ("shaken-iced-green-tea", "Shaken Iced Green Tea", "TEA"),
    ("tims-coconut-caramel", "Tims Coconut Caramel Macchiato", "MILK"),
    ("tims-coconut-matcha", "Tims Coconut Iced Matcha", "TEA"),
    ("tims-french-vanilla", "Tims French Vanilla", "MILK"),
    ("tims-hibiscus", "Tims Hibiscus Cold Brew", "TEA"),
    ("snowcap-mocha", "Snowcap Marshmallow Mocha", "FRAPPE"),
    ("tims-cafe-mocha", "Tims Cafe Mocha", "MILK"),
    ("tims-caramel-macchiato", "Tims Caramel Macchiato", "MILK"),
    ("java-chip-frappuccino", "Java Chip Frappuccino", "FRAPPE"),
    ("tims-iced-mango-matcha", "Tims Iced Mango Matcha", "TEA"),
    ("tims-iced-maple-cinnamon-latte", "Tims Iced Maple Cinnamon Latte", "MILK"),
    ("tims-mango-shake", "Tims Mango Shake", "FRAPPE"),
    ("very-berry-hibiscus", "Very Berry Hibiscus", "TEA"),
    ("vietnamese-spanish-latte", "Vietnamese Spanish Latte", "MILK"),
]


def res(pid: str, prefix: str) -> Path:
    return D / f"{prefix}_{pid.replace('-', '_')}.png"


def main() -> None:
    im = Image.new("RGB", (W, H), OUTER)
    d = ImageDraw.Draw(im)
    d.rounded_rectangle((0, 0, W - 1, H - 1), 40, fill=CREAM)

    # status row
    spaced(d, (64, 21), "MACHINE ID · JAV-08-221", sans(16, 600), MUTED, 1.92)
    spaced(d, (359, 21), "SUPPORT · +86 400 820 2006", sans(16, 600), MUTED, 1.92)
    spaced(d, (924, 21), "14:00", sans(16, 600), MUTED, 1.92)
    d.line((0, 66, 1080, 66), fill=(234, 229, 223), width=1)

    # header
    logo = Image.open(D / "kiosk_logo.png").convert("RGBA").resize((136, 64), Image.LANCZOS)
    im.paste(logo, (64, 73), logo)
    for x, label in ((485, "English"), (755, "Help")):
        d.rounded_rectangle((x, 71, x + 250, 126), 27, fill=SAND, outline=(238, 233, 227), width=1)
        spaced(d, (x + 125, 85), label, sans(23, 600), INK, 0, centre=True)

    # banner
    banner = crop_image(D / "kiosk_banner.jpg", (1080, 360))
    im.paste(banner.convert("RGB"), (0, 132))

    # category bar
    d.rectangle((0, 495, 1080, 572), fill=CREAM, outline=(234, 229, 223), width=1)
    cats = [("CLASSICS", 171.8), ("ESPRESSO", 178.3), ("MILK", 120), ("TEA", 120), ("FRAPPE", 143.3)]
    cx = 66.3
    for i, (label, cw) in enumerate(cats):
        col = INK if i == 0 else (186, 174, 162)
        spaced(d, (cx + cw / 2, 518), label, sans(23, 600), col, 2.76, centre=True)
        if i == 0:
            d.rounded_rectangle((cx + 16, 565, cx + cw - 16, 569), 2, fill=INK)
        cx += cw + 6

    # 30-card grid
    pool = [p for p in CATALOG if p[2] == "CLASSICS"] + [p for p in CATALOG if p[2] != "CLASSICS"]
    slots = [pool[i % len(pool)] for i in range(30)]
    spaced(d, (64, 605), "ALL DRINKS", sans(23, 600), INK, 2.76)
    spaced(d, (1016, 608), "30 ITEMS", sans(18, 600), MUTED, 1.4, right=True)
    d.line((64, 643, 1016, 643), fill=(234, 229, 223), width=1)

    for i, (pid, name, _) in enumerate(slots):
        row, col = divmod(i, 3)
        x = 64 + col * (301.34 + 24)
        y = 687.6 + row * 390
        photo = rounded_image(res(pid, "card"), (301, 301), 28)
        d.rounded_rectangle((int(x), int(y), int(x + 301), int(y + 346)), 28,
                             fill=CREAM, outline=(234, 229, 223), width=1)
        im.paste(photo.convert("RGB"), (int(x), int(y)))
        badge = ["Most Popular", "Top Pick", "Featured"][i % 3]
        bcol = CARAMEL if i % 3 == 1 else SAND
        tcol = CREAM if i % 3 == 1 else INK
        bw = d.textlength(badge, font=sans(15, 600)) + 28
        d.rounded_rectangle((int(x + 20), int(y + 20), int(x + 20 + bw), int(y + 52)), 16, fill=bcol)
        d.text((int(x + 34), int(y + 27)), badge, font=sans(15, 600), fill=tcol)
        d.rounded_rectangle((int(x), int(y + 256), int(x + 301), int(y + 346)), 18,
                             fill=(253, 251, 247))
        spaced(d, (x + 150.5, y + 268), name, fra(24), INK, centre=True)
        spaced(d, (x + 150.5, y + 300), "CHF 3.50", fra(24), RED, centre=True)

    # scrim
    scrim = Image.new("RGBA", (W, H), INK + (140,))
    im = Image.alpha_composite(im.convert("RGBA"), scrim)
    d = ImageDraw.Draw(im)

    # modal
    d.rounded_rectangle((160, 448, 920, 1472), 34, fill=CREAM)
    spaced(d, (204, 489), NAME, fra(36 if CARAMEL else 48, 600), INK)
    d.ellipse((820, 488, 876, 544), outline=(231, 224, 216), width=1)
    d.line((834, 502, 862, 530), fill=INK, width=2)
    d.line((862, 502, 834, 530), fill=INK, width=2)

    hero = fit_image(res(PRODUCT_ID, "hero"), (672, 385))
    mask = Image.new("L", (672, 385), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, 671, 384), 24, fill=255)
    im.paste(hero, (204, 572), mask)
    d = ImageDraw.Draw(im)

    groups = ([
        ("Milk", ["Whole", "Oat", "Almond"]),
        ("Sweetness", ["No Sugar", "Light", "Less Sugar", "Regular"]),
        ("Temperature", ["Iced", "No ice", "Hot"]),
    ] if CARAMEL else [
        ("Strength", ["Mild", "Medium", "Bold"]),
        ("Sweetness", ["Less sugar", "Regular sugar", "Extra sugar"]),
        ("Temperature", ["Iced", "No ice", "Hot"]),
    ])
    for gi, (title, opts) in enumerate(groups):
        row_y = 969 + gi * 88
        spaced(d, (204, row_y), title, fra(20), INK)
        n = len(opts)
        gap = 12
        cw = (672 - gap * (n - 1)) / n
        for oi, label in enumerate(opts):
            cx = 204 + oi * (cw + gap)
            sel = oi == 1
            d.rounded_rectangle((int(cx), row_y + 30, int(cx + cw), row_y + 82), 14,
                                fill=CARAMEL if sel else CHIP)
            size = 24 if (cw >= 200 or len(label) <= 9) else 20
            if len(label) > 12:
                size = 18
            fnt = fra(size, 600 if sel else 400)
            tw = d.textlength(label, font=fnt)
            d.text((cx + (cw - tw) / 2, row_y + 30 + (52 - size * 1.15) / 2), label,
                   font=fnt, fill=CREAM if sel else INK)

    # price bar (glass card + soft shadow)
    d.rounded_rectangle((204, 1249, 876, 1301), 18, fill=(226, 219, 210))
    d.rounded_rectangle((204, 1245, 876, 1297), 18, fill=(253, 251, 247),
                        outline=(238, 233, 227), width=1)
    spaced(d, (222, 1257), "Price", fra(20, 500), INK)
    val = "CHF 3.50"
    vw = d.textlength(val, font=fra(24))
    d.text((858 - vw, 1256), val, font=fra(24), fill=RED)

    # CTAs
    for x, label, bg in ((204, "Add to Cart", CARAMEL), (550, "Confirm", INK)):
        d.rounded_rectangle((x, 1337, x + 326, 1409), 36, fill=bg)
        fnt = sans(32, 600)
        tw = d.textlength(label, font=fnt)
        d.text((x + (326 - tw) / 2, 1356), label, font=fnt, fill=CREAM)

    im.convert("RGB").save(OUT)
    print("wrote", OUT)


if __name__ == "__main__":
    main()
