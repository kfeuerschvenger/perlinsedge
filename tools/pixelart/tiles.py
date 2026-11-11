"""Draw the flat land tiles, the ground decals and the water layers.

Each land tile is a 32x21 block: a 32x16 top diamond in one flat colour with a one pixel darker edge on its two upper sides, which reads as a faint grid across the map, and a 5 pixel side face below.
A face strip per tile kind is the next 5 pixels of that face, which the game draws under the block where a lower neighbour exposes it, once per 5 pixels of drop.
Decals are small separate sprites drawn on top of some tiles: grass tufts, pebbles, flowers and mushrooms.
Water lies over a bed that is a sand block with the same grid edge and side faces as land, sunk 10 pixels per depth level below the land tops: level 1 for shallow, coast and river water and level 2 for deep water.
The bed colours are mixed toward the water colour when the sprite is baked, 45 percent for level 1 and 75 percent for level 2, so the grid and the decals on the bed fade with depth instead of looking drawn over the water, and the step between two depths shows its side faces.
One translucent sheet of the water colour is drawn over every water tile, plus three thin highlight lines the game fades in and out.
The sheet diamond is two pixels narrower on its right side than a land diamond, so neighbouring water tiles meet without overlapping and the translucent sheet has no doubled seams.

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
    # warmer and a little darker than sand, drawn for grassy land types on a desert map
    "dune": ("#e3b66f", "#cf9a5c", "#b5814b"),
    # sunk sand blocks under the water, fogged toward the water colour by FOG_OF when baked
    "bed": ("#f0d48e", "#dba867", "#c4925a"),
    "bed-deep": ("#f0d48e", "#dba867", "#c4925a"),
}
GRID = 0.09
LIP = 0.18
WATER_COLOUR = "#18a8c8"
# share of the water colour mixed into every pixel of a bed: level 1 (shallow, coast, river) and level 2 (deep)
FOG_OF = {"bed": 0.45, "bed-deep": 0.75}
# the faces of a block are this tall, and a face strip continues them by this much
FACE_STRIP_H = 5


def in_top(x, y):
    """Whether a pixel lies on the top diamond."""
    if not 0 <= y < TOP_H:
        return False
    half = 2 * (y + 1) if y < TOP_H // 2 else 2 * (TOP_H - y)
    return TILE_W // 2 - half <= x < TILE_W // 2 + half


def in_block(x, y, face=FACE_H):
    """Whether a pixel lies on the block silhouette: the top diamond pushed down by the face height."""
    return any(in_top(x, y - d) for d in range(face + 1))


def fog(colour, amount):
    """Mixes a colour toward the water colour, the way depth hides a bed."""
    water = hexc(WATER_COLOUR)
    return tuple(round(c * (1 - amount) + w * amount) for c, w in zip(colour, water))


def on_upper_edge(x, y):
    """Whether a top pixel touches the upper left or upper right side of the diamond.

    The lower sides are covered by the next tiles drawn in front, so the grid line goes on the upper sides, which are drawn last.
    """
    return y < TOP_H // 2 and in_top(x, y) and not in_top(x, y - 1)


def land_tile(top, left, right, grid=GRID, depth_fog=0.0):
    top, left, right = hexc(top), hexc(left), hexc(right)
    img = Image.new("RGBA", (TILE_W, TOP_H + FACE_H), (0, 0, 0, 0))
    for y in range(TOP_H + FACE_H):
        for x in range(TILE_W):
            if in_top(x, y):
                colour = darken(top, grid) if on_upper_edge(x, y) else top
            elif in_block(x, y):
                face = left if x < TILE_W // 2 else right
                colour = darken(top, LIP) if in_top(x, y - 1) else face
            else:
                continue
            if depth_fog:
                colour = fog(colour, depth_fog)
            img.putpixel((x, y), colour + (255,))
    return img


def face_strip(left, right, depth_fog=0.0):
    """Draws the part of a taller block's faces below the face of a land tile, in the colours of that tile.

    The strip is as wide as the tile and as tall as a block with a face of FACE_H + FACE_STRIP_H, with only the new face pixels filled, so it is drawn at the origin of the tile and one more strip is drawn 5 pixels lower for each further 5 pixels of drop.
    The game draws one half of it, the left or the right, under a tile whose lower neighbour on that side exposes the face.
    """
    left, right = hexc(left), hexc(right)
    img = Image.new("RGBA", (TILE_W, TOP_H + FACE_H + FACE_STRIP_H), (0, 0, 0, 0))
    for y in range(img.height):
        for x in range(TILE_W):
            if in_block(x, y, FACE_H + FACE_STRIP_H) and not in_block(x, y):
                colour = left if x < TILE_W // 2 else right
                if depth_fog:
                    colour = fog(colour, depth_fog)
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
    "lilypad-1": ([".aaa.", "abbba", ".aaa."], {"a": "#4f9c45", "b": "#76b04c"}),
    "lilypad-2": ([".aaap.", "abbpp.", ".aaa.."], {"a": "#4f9c45", "b": "#76b04c", "p": "#f08aa8"}),
}


# decals that lie on a level 1 bed, baked in the colours the fogged bed has; they are drawn under the water sheet
UNDERWATER = {
    "seaweed-1": (["a..a", "a.a.", ".aa.", ".a.."], {"a": "#27706c"}),
    "seaweed-2": (["..a", ".a.", "aa.", ".a."], {"a": "#1d4b4c"}),
    "pebble-1": DECALS["pebble-1"],
}

# colour and opacity of the one translucent sheet drawn over every water tile
WATER = {
    "sheet": (WATER_COLOUR, 0.30),
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
    tiles = {name: land_tile(*colours, depth_fog=FOG_OF.get(name, 0.0)) for name, colours in LAND.items()}
    faces = {name: face_strip(colours[1], colours[2], depth_fog=FOG_OF.get(name, 0.0)) for name, colours in LAND.items()}
    decals = {name: sprite(*spec) for name, spec in DECALS.items()}
    for name, (rows, palette) in UNDERWATER.items():
        fogged = {key: "#%02x%02x%02x" % fog(hexc(value), FOG_OF["bed"]) for key, value in palette.items()}
        decals[f"under-{name}"] = sprite(rows, fogged)
    if args.preview:
        preview(tiles, decals, args.preview)
        return
    (out / "tiles").mkdir(parents=True, exist_ok=True)
    (out / "decals").mkdir(parents=True, exist_ok=True)
    for name, img in tiles.items():
        img.save(out / "tiles" / f"{name}-1.png")
    (out / "faces").mkdir(parents=True, exist_ok=True)
    for name, img in faces.items():
        img.save(out / "faces" / f"{name}.png")
    for name, img in decals.items():
        img.save(out / "decals" / f"{name}.png")
    (out / "water").mkdir(parents=True, exist_ok=True)
    for name, (colour, opacity) in WATER.items():
        water_top(colour, opacity).save(out / "water" / f"{name}.png")
    for index, width in enumerate(SHIMMER_WIDTHS, start=1):
        shimmer(width).save(out / "water" / f"shimmer-{index}.png")
    print(f"tiles {len(tiles)}, faces {len(faces)}, decals {len(decals)}, water {len(WATER)}, shimmer {len(SHIMMER_WIDTHS)}")


if __name__ == "__main__":
    main()
