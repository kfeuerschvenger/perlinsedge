"""Draw the flat land tiles, the ground decals and the water layers.

Each land tile is a 32x21 block: a 32x16 top diamond in one flat colour with a one pixel darker edge on its two upper sides, which reads as a faint grid across the map, and a 5 pixel side face below.
Decals are small separate sprites drawn on top of some tiles: grass tufts, pebbles, flowers, mushrooms and seaweed.
Water is a flat opaque bed diamond per bed kind, sunk a full land face below the land tops, and a translucent top diamond per water type drawn over it, plus three thin highlight lines the game fades in and out.
The water diamond is two pixels narrower on its right side than a land diamond, so neighbouring water tiles meet without overlapping and the translucent sheet has no doubled seams.

usage: python -I tools/pixelart/tiles.py [--out src/main/resources/assets] [--preview preview.png]
"""
import argparse
import random
from pathlib import Path

from PIL import Image

TILE_W, TOP_H, FACE_H = 32, 16, 5
OUTLINE = (0x1d, 0x18, 0x30)


def hexc(value):
    return tuple(int(value[i:i + 2], 16) for i in (1, 3, 5))


def darken(colour, amount):
    return tuple(max(0, round(c * (1 - amount))) for c in colour)


# top colour, left face colour, right face colour
LAND = {
    "grass": ("#5aa84a", "#7a4a3a", "#4e2f36"),
    "forest": ("#4f9c45", "#7a4a3a", "#4e2f36"),
    "savanna": ("#a7b05a", "#7a4a3a", "#4e2f36"),
    "hill": ("#8aa85a", "#7b6040", "#55432f"),
    "sand": ("#f0d48e", "#dba867", "#c4925a"),
    "mountain": ("#9a98a6", "#7d7a8c", "#4a4868"),
    "snow": ("#eef4fa", "#bcd0e6", "#9fb4e0"),
    "ice": ("#c8e4f2", "#9fb4e0", "#7a8cc8"),
    "swamp": ("#6c8a4e", "#55432f", "#43553f"),
}
GRID = 0.09
LIP = 0.18


def in_top(x, y):
    """Whether a pixel lies on the top diamond."""
    if not 0 <= y < TOP_H:
        return False
    half = 2 * (y + 1) if y < TOP_H // 2 else 2 * (TOP_H - y)
    return TILE_W // 2 - half <= x < TILE_W // 2 + half


def in_block(x, y):
    """Whether a pixel lies on the block silhouette: the top diamond pushed down by the face height."""
    return any(in_top(x, y - d) for d in range(FACE_H + 1))


def on_upper_edge(x, y):
    """Whether a top pixel touches the upper left or upper right side of the diamond.

    The lower sides are covered by the next tiles drawn in front, so the grid line goes on the upper sides, which are drawn last.
    """
    return y < TOP_H // 2 and in_top(x, y) and not in_top(x, y - 1)


def land_tile(top, left, right):
    top, left, right = hexc(top), hexc(left), hexc(right)
    img = Image.new("RGBA", (TILE_W, TOP_H + FACE_H), (0, 0, 0, 0))
    for y in range(TOP_H + FACE_H):
        for x in range(TILE_W):
            if in_top(x, y):
                colour = darken(top, GRID) if on_upper_edge(x, y) else top
            elif in_block(x, y):
                face = left if x < TILE_W // 2 else right
                colour = darken(top, LIP) if in_top(x, y - 1) else face
            else:
                continue
            img.putpixel((x, y), colour + (255,))
    return img


def sprite(rows, palette):
    """Builds a small sprite from text rows; '.' is transparent, other characters index the palette."""
    img = Image.new("RGBA", (len(rows[0]), len(rows)), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), hexc(palette[ch]) + (255,))
    return img


DECALS = {
    "tuft-1": (["..a...a.", ".ab.a.b.", ".ab.ab..", "..b.b..."], {"a": "#76b04c", "b": "#337a47"}),
    "tuft-2": ([".a..a.", "ab.ab.", ".b.b.."], {"a": "#76b04c", "b": "#337a47"}),
    "tuft-3": (["a.a.a", "babab", ".b.b."], {"a": "#b3d05e", "b": "#459b4c"}),
    "pebble-1": ([".aa.", "abbc", ".cc."], {"a": "#aeadb8", "b": "#7d7a8c", "c": "#4a4868"}),
    "pebble-2": ([".ab..", "abbc.", ".cc.a", "...ac"], {"a": "#aeadb8", "b": "#7d7a8c", "c": "#4a4868"}),
    "flower-1": (["a.a", ".b.", "c.c"], {"a": "#fcf4e4", "b": "#ffa02a", "c": "#337a47"}),
    "flower-2": (["a...", ".c.a", "..c."], {"a": "#d4705c", "c": "#337a47"}),
    "mushroom-1": ([".aa.", "abba", ".cc.", ".cc."], {"a": "#9c4247", "b": "#fcf4e4", "c": "#f4d890"}),
    "shell-1": ([".a.", "aba"], {"a": "#f4bf9a", "b": "#d4705c"}),
    "seaweed-1": (["a..a", "a.a.", ".aa.", ".a.."], {"a": "#27706c"}),
    "seaweed-2": (["..a", ".a.", "aa.", ".a."], {"a": "#1d4b4c"}),
    "lilypad-1": ([".aaa.", "abbba", ".aaa."], {"a": "#4f9c45", "b": "#76b04c"}),
    "lilypad-2": ([".aaap.", "abbpp.", ".aaa.."], {"a": "#4f9c45", "b": "#76b04c", "p": "#f08aa8"}),
}


# top colour and opacity of the translucent water drawn over the bed
WATER = {
    "shallow": ("#14a8d0", 0.58),
    "coast": ("#25bcd0", 0.55),
    "river": ("#1f90c4", 0.58),
    "deep": ("#1a4a8a", 0.82),
}
# flat opaque bed diamonds: sand for the shallow, coast and river water, a darker stone sand for the deep water
BEDS = {
    "bed": "#cbb07a",
    "bed-deep": "#8a7a64",
}
SHIMMER_COLOUR = "#dcecf8"
SHIMMER_WIDTHS = (6, 4, 5)


def in_water(x, y):
    """Whether a pixel lies on the water diamond: the top diamond without its rightmost two pixels per row.

    Two neighbouring diamonds of the isometric grid overlap by two pixels on every shared edge. Dropping the right pair
    makes them meet exactly, so a translucent sheet is never drawn twice over the same pixel.
    """
    return in_top(x, y) and in_top(x + 2, y)


def water_top(colour, opacity):
    """Draws the top diamond of a water tile in one translucent colour, to be drawn over the bed."""
    img = Image.new("RGBA", (TILE_W, TOP_H), (0, 0, 0, 0))
    fill = hexc(colour) + (round(255 * opacity),)
    for y in range(TOP_H):
        for x in range(TILE_W):
            if in_water(x, y):
                img.putpixel((x, y), fill)
    return img


def bed_top(colour):
    """Draws a flat opaque 32x16 diamond with no grid line, which shows through the water above it."""
    img = Image.new("RGBA", (TILE_W, TOP_H), (0, 0, 0, 0))
    fill = hexc(colour) + (255,)
    for y in range(TOP_H):
        for x in range(TILE_W):
            if in_top(x, y):
                img.putpixel((x, y), fill)
    return img


def shimmer(width):
    """Draws a thin opaque highlight line, whose visibility the game fades with the global alpha."""
    img = Image.new("RGBA", (width, 1), (0, 0, 0, 0))
    for x in range(width):
        img.putpixel((x, 0), hexc(SHIMMER_COLOUR) + (255,))
    return img


def preview(tiles, decals, path):
    """Draws an isometric patch of each land tile with decals scattered on it."""
    size = 8
    rng = random.Random(7)
    names = list(tiles)
    canvas = Image.new("RGBA", (TILE_W * size * 2 + 64, TOP_H * size * len(names) // 2 + 200), (0x1d, 0x18, 0x30, 255))
    grass_decals = ["tuft-1", "tuft-2", "tuft-3", "pebble-1", "flower-1", "flower-2", "mushroom-1"]
    for i, name in enumerate(names):
        ox = 32 + (i % 2) * TILE_W * size
        oy = 16 + (i // 2) * (TOP_H * size // 2 + 24)
        for gy in range(size):
            for gx in range(size):
                sx = ox + (gx - gy) * TILE_W // 2 + TILE_W * size // 2 - TILE_W // 2
                sy = oy + (gx + gy) * TOP_H // 2
                canvas.alpha_composite(tiles[name], (sx, sy))
                if name in ("grass", "forest", "savanna", "hill", "swamp") and rng.random() < 0.3:
                    d = decals[rng.choice(grass_decals)]
                    canvas.alpha_composite(d, (sx + rng.randint(8, 20), sy + rng.randint(4, 10) - d.height // 2))
                elif name == "sand" and rng.random() < 0.15:
                    d = decals[rng.choice(["shell-1", "pebble-1"])]
                    canvas.alpha_composite(d, (sx + rng.randint(8, 20), sy + rng.randint(4, 10)))
    canvas.resize((canvas.width * 2, canvas.height * 2), Image.NEAREST).save(path)


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--out", default="src/main/resources/assets")
    parser.add_argument("--preview")
    args = parser.parse_args()
    out = Path(args.out)
    tiles = {name: land_tile(*colours) for name, colours in LAND.items()}
    decals = {name: sprite(*spec) for name, spec in DECALS.items()}
    if args.preview:
        preview(tiles, decals, args.preview)
        return
    (out / "tiles").mkdir(parents=True, exist_ok=True)
    (out / "decals").mkdir(parents=True, exist_ok=True)
    for name, img in tiles.items():
        img.save(out / "tiles" / f"{name}-1.png")
    for name, img in decals.items():
        img.save(out / "decals" / f"{name}.png")
    (out / "water").mkdir(parents=True, exist_ok=True)
    for name, (colour, opacity) in WATER.items():
        water_top(colour, opacity).save(out / "water" / f"{name}.png")
    for name, colour in BEDS.items():
        bed_top(colour).save(out / "tiles" / f"{name}-1.png")
    for index, width in enumerate(SHIMMER_WIDTHS, start=1):
        shimmer(width).save(out / "water" / f"shimmer-{index}.png")
    print(f"tiles {len(tiles)}, decals {len(decals)}, water {len(WATER)}, beds {len(BEDS)}, shimmer {len(SHIMMER_WIDTHS)}")


main()
