"""Draw the trees, bushes, rocks and iron ore resource sprites in several numbered variants.

Each sprite is built from rounded lumps lit from the upper left and wrapped in the palette outline. Pines stack foliage tiers on a short trunk, round trees hold a single crown, rocks are low boulders and iron ore is a rock with rust coloured specks.
The default folder gets tree-1 to tree-6, bush-1 to bush-3, rock-1 to rock-4 and iron-1 to iron-3. The dense_forest folder gets pines and the swamp folder gets olive round trees. The game picks up any number of variants, so adding one to SET needs no code change.

usage: python -I tools/pixelart/trees.py [--out src/main/resources/assets/resources] [--preview preview.png]
"""
import argparse
import random
from pathlib import Path

from PIL import Image

OUTLINE = (0x1d, 0x18, 0x30)


def hexc(value):
    return tuple(int(value[i:i + 2], 16) for i in (1, 3, 5))


# foliage ramps, dark to light
GREEN = ["#337a47", "#459b4c", "#76b04c", "#b3d05e"]
OLIVE = ["#43553f", "#6c8268", "#a7b074", "#b3d05e"]
TRUNK = ["#4e2f36", "#7a4a3a", "#a0643f"]
STONE = ["#4a4868", "#7d7a8c", "#aeadb8", "#dcecf8"]


class Canvas:
    """A transparent pixel grid that shapes are painted on, then outlined and turned into an image."""

    def __init__(self, w, h):
        self.w, self.h = w, h
        self.px = [[None] * w for _ in range(h)]

    def set(self, x, y, c):
        """Paints one pixel, ignoring coordinates outside the grid."""
        if 0 <= x < self.w and 0 <= y < self.h:
            self.px[y][x] = c

    def outline(self):
        """Adds the palette outline on every empty pixel that touches a painted one."""
        out = [r[:] for r in self.px]
        for y in range(self.h):
            for x in range(self.w):
                if self.px[y][x] is None and any(
                        0 <= x + dx < self.w and 0 <= y + dy < self.h and self.px[y + dy][x + dx] is not None
                        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                    out[y][x] = OUTLINE
        self.px = out

    def image(self):
        """Returns the grid as an RGBA image."""
        img = Image.new("RGBA", (self.w, self.h), (0, 0, 0, 0))
        for y in range(self.h):
            for x in range(self.w):
                if self.px[y][x] is not None:
                    img.putpixel((x, y), self.px[y][x] + (255,))
        return img


def blob(cv, cx, cy, rx, ry, ramp, flat_bottom=0.0):
    """Fills a lit ellipse: light on the upper left, dark on the lower right, with a ragged lower rim."""
    for y in range(int(cy - ry) - 1, int(cy + ry) + 2):
        for x in range(int(cx - rx) - 1, int(cx + rx) + 2):
            dx, dy = (x + 0.5 - cx) / rx, (y + 0.5 - cy) / ry
            d = dx * dx + dy * dy
            if dy > 0:
                d += flat_bottom * dy
            if d > 1.0:
                continue
            light = -dx * 0.6 - dy * 0.8
            if dy > 0.35 and d > 0.5:
                tone = 0
            elif light > 0.7:
                tone = 3
            elif light > -0.05:
                tone = 2
            else:
                tone = 1
            cv.set(x, y, hexc(ramp[tone]))


def trunk(cv, cx, top, bottom, width):
    """Paints a shaded trunk with a root flare on each side of its foot."""
    for y in range(top, bottom + 1):
        for x in range(cx - width // 2, cx - width // 2 + width):
            tone = 2 if x == cx - width // 2 else 0 if x == cx - width // 2 + width - 1 else 1
            cv.set(x, y, hexc(TRUNK[tone]))
    # roots
    cv.set(cx - width // 2 - 1, bottom, hexc(TRUNK[1]))
    cv.set(cx - width // 2 + width, bottom, hexc(TRUNK[0]))


def pine(seed, ramp, w=32, h=48):
    """Draws a pine: a trunk under three or four foliage tiers that narrow towards a rounded tip."""
    rng = random.Random(seed)
    cv = Canvas(w, h)
    cx = w / 2
    base = h - 2
    trunk_h = rng.randint(4, 6)
    trunk(cv, int(cx), base - trunk_h - 2, base, 4)
    tiers = rng.choice([3, 4, 4])
    width = rng.uniform(11.5, 13.5)
    y = base - trunk_h - 1
    tier_h = rng.uniform(6.0, 6.8) if tiers == 4 else rng.uniform(7.2, 8.0)
    for i in range(tiers):
        rx = width * (1 - i * 0.22)
        blob(cv, cx + rng.uniform(-0.4, 0.4), y - tier_h * 0.6, rx, tier_h, ramp, flat_bottom=0.6)
        y -= tier_h * 1.05
    blob(cv, cx, y, max(2.5, width * 0.22), 3.5, ramp)
    cv.outline()
    return cv.image()


def round_tree(seed, ramp, w=32, h=48):
    """Draws a round tree: a trunk under one large crown with a few overlapping lobes."""
    rng = random.Random(seed)
    cv = Canvas(w, h)
    cx = w / 2
    base = h - 2
    trunk(cv, int(cx), base - 9, base, 4)
    r = rng.uniform(11.5, 13.5)
    cy = base - 9 - r * 0.75
    blob(cv, cx, cy, r, r * 0.92, ramp)
    for _ in range(rng.randint(2, 3)):
        blob(cv, cx + rng.uniform(-r * 0.5, r * 0.5), cy + rng.uniform(-r * 0.3, r * 0.4), r * 0.55, r * 0.5, ramp)
    cv.outline()
    return cv.image()


def bush(seed, ramp, w=24, h=20):
    """Draws a low bush of three or four overlapping lobes."""
    rng = random.Random(seed)
    cv = Canvas(w, h)
    for _ in range(rng.randint(3, 4)):
        blob(cv, w / 2 + rng.uniform(-4, 4), h - 8 + rng.uniform(-2, 1), rng.uniform(6, 8), rng.uniform(5.5, 6.5), ramp, 0.8)
    cv.outline()
    return cv.image()


def rock(seed, ramp, w=24, h=20, ore=None):
    """A low boulder of one to three lumps; ore adds a few two pixel coloured specks."""
    rng = random.Random(seed)
    cv = Canvas(w, h)
    for i in range(rng.randint(1, 3)):
        rx = rng.uniform(7.0, 9.0) if i == 0 else rng.uniform(4.0, 5.5)
        ox = 0 if i == 0 else rng.choice([-1, 1]) * rng.uniform(4, 6)
        blob(cv, w / 2 + ox, h - 2 - rx * 0.75, rx, rx * 0.85, ramp, 0.9)
    if ore:
        cells = [(x, y) for y in range(h) for x in range(w - 1) if cv.px[y][x] is not None and cv.px[y][x + 1] is not None]
        for x, y in rng.sample(cells, min(len(cells), 9)):
            cv.set(x, y, hexc(ore[1]))
            cv.set(x + 1, y, hexc(ore[0]))
    cv.outline()
    return cv.image()


def iron(seed, ramp):
    """Draws a rock with rust coloured ore specks."""
    return rock(seed, ramp, ore=("#9c4247", "#d4705c"))


# (folder, name, maker, seed, ramp)
SET = [("default", f"tree-{i + 1}", pine, 11 + i * 7, GREEN) for i in range(4)] \
    + [("default", f"tree-{i + 5}", round_tree, 101 + i * 7, GREEN) for i in range(2)] \
    + [("default", f"bush-{i + 1}", bush, 201 + i * 5, GREEN) for i in range(3)] \
    + [("default", f"rock-{i + 1}", rock, 501 + i * 3, STONE) for i in range(4)] \
    + [("default", f"iron-{i + 1}", iron, 601 + i * 3, STONE) for i in range(3)] \
    + [("dense_forest", f"tree-{i + 1}", pine, 301 + i * 7, GREEN) for i in range(4)] \
    + [("swamp", f"tree-{i + 1}", round_tree, 401 + i * 7, OLIVE) for i in range(3)]


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--out", default="src/main/resources/assets/resources")
    parser.add_argument("--preview")
    args = parser.parse_args()
    images = [(folder, name, maker(seed, ramp)) for folder, name, maker, seed, ramp in SET]
    if args.preview:
        sheet = Image.new("RGBA", (34 * len(images), 50), (0x5a, 0xa8, 0x4a, 255))
        for i, (_, _, img) in enumerate(images):
            sheet.alpha_composite(img, (i * 34 + (32 - img.width) // 2, 50 - img.height - 1))
        sheet.resize((sheet.width * 4, sheet.height * 4), Image.NEAREST).save(args.preview)
        return
    for folder, name, img in images:
        (Path(args.out) / folder).mkdir(parents=True, exist_ok=True)
        img.save(Path(args.out) / folder / f"{name}.png")
    print(f"resources {len(images)}")


main()
