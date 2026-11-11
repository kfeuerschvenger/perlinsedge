"""Draw the trees, bushes, rocks and iron ore resource sprites in several numbered variants.

Each sprite is built from rounded lumps lit from the upper left and wrapped in the palette outline. Pines stack foliage tiers on a short trunk, round trees hold a single crown, rocks are low boulders and iron ore is a rock with rust coloured specks.
Every variant gets a small hue and lightness tint so trees and rocks of one kind do not all share a colour. The default folder gets pines and round trees, bushes, rocks and iron ore; dense_forest gets pines, swamp olive round trees, desert sandstone rocks, palms and cacti, and tundra snow capped rocks and pines. The game picks up any number of variants, so adding one to SET needs no code change.

usage: python -I tools/pixelart/trees.py [--out src/main/resources/assets/resources] [--preview preview.png]
"""
import argparse
import colorsys
import math
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
SANDSTONE = ["#7b6040", "#a2825a", "#dba867", "#f4d890"]
SNOW = ["#bcd0e6", "#fcf4e4"]
TRUNK_PALM = ["#7b6040", "#a2825a", "#c4925a"]
CACTUS = ["#1d4b4c", "#337a47", "#459b4c", "#76b04c"]

# small hue and lightness shifts so variants of one kind do not all share a colour
TINTS = [(0.0, 0.0), (0.025, 0.03), (-0.02, -0.035), (0.04, -0.01), (-0.035, 0.02), (0.015, -0.05)]


def tint(ramp, shift):
    """Shifts every colour of a ramp by a hue and lightness delta, keeping the ramp order."""
    dh, dl = shift
    out = []
    for colour in ramp:
        r, g, b = (c / 255 for c in hexc(colour))
        h, l, s = colorsys.rgb_to_hls(r, g, b)
        r, g, b = colorsys.hls_to_rgb((h + dh) % 1.0, min(1.0, max(0.0, l + dl)), s)
        out.append("#%02x%02x%02x" % (round(r * 255), round(g * 255), round(b * 255)))
    return out


def snow_cap(cv, depth=2):
    """Paints the topmost painted pixels of every column with snow, the very top one brightest."""
    for x in range(cv.w):
        painted = [y for y in range(cv.h) if cv.px[y][x] is not None]
        for i, y in enumerate(painted[:depth]):
            cv.px[y][x] = hexc(SNOW[1] if i == 0 else SNOW[0])


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


def snowy(maker, depth=2):
    """Wraps a maker so its sprite gets a snow cap of the given depth before the outline."""
    def make(seed, ramp):
        original = Canvas.outline

        def outline_with_snow(cv):
            snow_cap(cv, depth)
            original(cv)

        Canvas.outline = outline_with_snow
        try:
            return maker(seed, ramp)
        finally:
            Canvas.outline = original
    return make


def iron(seed, ramp):
    """Draws a rock with rust coloured ore specks."""
    return rock(seed, ramp, ore=("#9c4247", "#d4705c"))


def palm(seed, ramp, w=32, h=48):
    """Draws a palm: a banded trunk that leans to one side under a crown of drooping fronds."""
    rng = random.Random(seed)
    cv = Canvas(w, h)
    base = h - 2
    height = rng.randint(24, 30)
    lean = rng.choice([-1, 1]) * rng.uniform(2.0, 4.0)
    top_x, top_y = w / 2, base - height
    for i in range(height + 1):
        t = i / height
        x = int(round(w / 2 - 1 - lean / 2 + lean * t * t))
        y = base - i
        band = 0 if i % 3 == 0 else 1
        cv.set(x, y, hexc(TRUNK_PALM[band + 1]))
        cv.set(x + 1, y, hexc(TRUNK_PALM[band]))
        top_x = x + 0.5
    crown = set()

    def disc(fx, fy, r):
        for yy in range(int(fy - r) - 1, int(fy + r) + 2):
            for xx in range(int(fx - r) - 1, int(fx + r) + 2):
                if ((xx + 0.5 - fx) / r) ** 2 + ((yy + 0.5 - fy) / (r * 0.75)) ** 2 <= 1.0:
                    crown.add((xx, yy))

    fronds = rng.randint(6, 7)
    start = rng.uniform(0, math.pi)
    for k in range(fronds):
        angle = start + k * 2 * math.pi / fronds + rng.uniform(-0.2, 0.2)
        length = rng.uniform(10.0, 12.5)
        steps = int(length)
        for j in range(1, steps + 1):
            r = 1.7 - 1.0 * j / steps
            disc(top_x + math.cos(angle) * j, top_y + math.sin(angle) * j * 0.4 - 0.25 * j + 0.06 * j * j, r)
    disc(top_x, top_y, 2.5)
    for xx, yy in crown:
        above, below = (xx, yy - 1) in crown, (xx, yy + 1) in crown
        tone = 3 if not above else 1 if not below else 2
        cv.set(xx, yy, hexc(ramp[tone]))
    for dx in (-1, 1):
        cv.set(int(top_x) + dx, top_y + 2, hexc("#7b6040"))
    cv.outline()
    return cv.image()


def cactus(seed, ramp, w=24, h=32):
    """Draws a column cactus with one or two arms, lit from the left, ribbed with a darker stripe."""
    rng = random.Random(seed)
    cv = Canvas(w, h)
    base = h - 2
    cx = w // 2
    height = rng.randint(16, 24)

    def column(x0, top, bottom, width):
        for y in range(top, bottom + 1):
            for x in range(x0, x0 + width):
                rel = x - x0
                tone = 3 if rel == 0 else 1 if rel == width - 1 else 2
                if (y - top) % 4 == 0 and rel == width // 2:
                    tone = 1
                cv.set(x, y, hexc(ramp[tone]))
        for x in range(x0 + 1, x0 + width - 1):
            cv.set(x, top - 1, hexc(ramp[2]))

    column(cx - 2, base - height, base, 5)
    arms = rng.choice([1, 2, 2])
    sides = rng.sample([-1, 1], arms)
    for side in sides:
        join = base - rng.randint(height // 3, height // 2)
        reach = rng.randint(3, 4)
        x_arm = cx - 2 - reach - 2 if side < 0 else cx + 3 + reach - 1
        for x in range(min(cx, x_arm), max(cx, x_arm + 3)):
            for y in (join, join + 1):
                cv.set(x, y, hexc(ramp[2 if y == join else 1]))
        column(x_arm, join - rng.randint(5, 8), join + 1, 3)
    if rng.random() < 0.5:
        cv.set(cx, base - height - 2, hexc("#d4705c"))
    cv.outline()
    return cv.image()


# (folder, name, maker, seed, ramp)
def variants(folder, kind, maker, seed, ramp, count, start=1):
    """Numbered variants of one kind, each with its own shape seed and colour tint."""
    return [(folder, f"{kind}-{start + i}", maker, seed + i * 7, tint(ramp, TINTS[i % len(TINTS)])) for i in range(count)]


SET = variants("default", "tree", pine, 11, GREEN, 5) \
    + variants("default", "tree", round_tree, 101, GREEN, 3, start=6) \
    + variants("default", "bush", bush, 201, GREEN, 3) \
    + variants("default", "rock", rock, 501, STONE, 4) \
    + variants("default", "iron", iron, 601, STONE, 3) \
    + variants("dense_forest", "tree", pine, 301, GREEN, 6) \
    + variants("swamp", "tree", round_tree, 401, OLIVE, 4) \
    + variants("desert", "rock", rock, 701, SANDSTONE, 4) \
    + variants("desert", "palm", palm, 1001, GREEN, 4) \
    + variants("desert", "cactus", cactus, 1101, CACTUS, 4) \
    + variants("tundra", "rock", snowy(rock, 3), 801, tint(STONE, (0.0, -0.12)), 4) \
    + variants("tundra", "tree", snowy(pine), 901, GREEN, 4)


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
