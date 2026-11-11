"""Post-process raw generated pixel art into the sprites the game loads.

Usage: python -I tools/pixelart/build.py --raw <dir> [--out <dir>]

The raw folder is local and never committed. The tool is idempotent: it rebuilds every output it finds an input for,
so it can be re-run whenever raw files are added or replaced. The land tiles, the decals, the water layers and the tree, bush, rock, iron ore, palm and cactus sprites are not built here: tiles.py and trees.py draw them.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
REPO = HERE.parent.parent
DEFAULT_OUT = REPO / "src" / "main" / "resources" / "assets"
PALETTE_FILE = HERE / "palette.hex"

# Isolated mode leaves the script folder off the import path, and player.py lives next to this file.
sys.path.insert(0, str(HERE))

from player import build_player_sheet  # noqa: E402

ALPHA_CUTOFF = 128
ITEM_SIZE = 16
SLIME_CELL = (20, 16)
SLIME_FRAMES = 6
EMPTY_HEART_FILL = "4a4868"
OUTLINE = "1d1830"

# Logical canvas per resource kind (width, height). A kind not listed keeps its content size.
RESOURCE_SIZES = {
    "tree": (32, 48),
    "willow": (40, 48),
    "pine": (28, 48),
    "bush": (24, 20),
    "shrub": (24, 20),
    "fern": (16, 14),
    "reed": (16, 20),
    "rock": (24, 20),
    "iron": (24, 20),
    "crystal": (24, 24),
}

# Resource kinds trees.py draws, so a raw file of one of them is ignored and never overwrites the drawn sprite.
DRAWN_KINDS = {"tree", "bush", "rock", "iron", "palm", "cactus"}

# Slime squash and stretch, as (width, height) factors of the content box, one per frame.
SLIME_SHAPES = [(1.0, 0.875), (1.1, 0.69), (0.9, 1.0), (0.85, 1.0), (0.95, 0.9), (1.1, 0.75)]

Rgb = tuple[int, int, int]


def load_palette(path: Path) -> list[Rgb]:
    """Reads one hex colour per line."""
    colours = []
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip().lstrip("#")
        if line:
            colours.append((int(line[0:2], 16), int(line[2:4], 16), int(line[4:6], 16)))
    return colours


class Quantiser:
    """Snaps colours to a fixed palette by nearest RGB distance, with no dithering."""

    def __init__(self, palette: list[Rgb]):
        self.palette = palette
        self.cache: dict[Rgb, Rgb] = {}

    def nearest(self, colour: Rgb) -> Rgb:
        hit = self.cache.get(colour)
        if hit is None:
            hit = min(self.palette, key=lambda p: (p[0] - colour[0]) ** 2 + (p[1] - colour[1]) ** 2 + (p[2] - colour[2]) ** 2)
            self.cache[colour] = hit
        return hit

    def apply(self, image: Image.Image) -> Image.Image:
        """Returns the image on the palette with binary alpha."""
        src = image.convert("RGBA")
        out = Image.new("RGBA", src.size, (0, 0, 0, 0))
        pixels = src.load()
        target = out.load()
        for y in range(src.height):
            for x in range(src.width):
                r, g, b, a = pixels[x, y]
                if a >= ALPHA_CUTOFF:
                    nr, ng, nb = self.nearest((r, g, b))
                    target[x, y] = (nr, ng, nb, 255)
        return out


def content_box(image: Image.Image):
    return image.getchannel("A").point(lambda a: 255 if a >= ALPHA_CUTOFF else 0).getbbox()


def place_bottom_centre(content: Image.Image, width: int, height: int) -> Image.Image:
    """Pads to at least width x height, with the content centred horizontally and resting on the bottom row."""
    width = max(width, content.width)
    height = max(height, content.height)
    canvas = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    canvas.paste(content, ((width - content.width) // 2, height - content.height))
    return canvas


def crop_to_content(image: Image.Image) -> Image.Image:
    box = content_box(image)
    return image.crop(box) if box else image


# ----------------------------------------------------------------------------------------------------------------
# Entities
# ----------------------------------------------------------------------------------------------------------------

def build_slime_strip(raw: Image.Image) -> Image.Image:
    """Builds a squash and stretch strip from one frame: rest, crouch, stretch, apex, fall, landing."""
    content = crop_to_content(raw)
    strip = Image.new("RGBA", (SLIME_CELL[0] * SLIME_FRAMES, SLIME_CELL[1]), (0, 0, 0, 0))
    for index, (fw, fh) in enumerate(SLIME_SHAPES):
        size = (min(SLIME_CELL[0], max(1, round(content.width * fw))), min(SLIME_CELL[1], max(1, round(content.height * fh))))
        frame = content.resize(size, Image.Resampling.NEAREST)
        strip.paste(place_bottom_centre(frame, *SLIME_CELL), (index * SLIME_CELL[0], 0))
    return strip


def build_empty_heart(full: Image.Image) -> Image.Image:
    """Draws the silhouette of the full heart as an outline around a dark neutral fill."""
    outline = tuple(int(OUTLINE[i:i + 2], 16) for i in (0, 2, 4))
    fill = tuple(int(EMPTY_HEART_FILL[i:i + 2], 16) for i in (0, 2, 4))
    src = full.load()
    out = Image.new("RGBA", full.size, (0, 0, 0, 0))
    pixels = out.load()

    def solid(x: int, y: int) -> bool:
        return 0 <= x < full.width and 0 <= y < full.height and src[x, y][3] > 0

    for y in range(full.height):
        for x in range(full.width):
            if solid(x, y):
                edge = not (solid(x - 1, y) and solid(x + 1, y) and solid(x, y - 1) and solid(x, y + 1))
                pixels[x, y] = (*(outline if edge else fill), 255)
    return out


# ----------------------------------------------------------------------------------------------------------------
# Driver
# ----------------------------------------------------------------------------------------------------------------

def save(image: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, format="PNG", optimize=False)
    print(f"  {path.name:<28} {image.width}x{image.height}")


def load(path: Path, quant: Quantiser) -> Image.Image:
    return quant.apply(Image.open(path))


def split_variant(stem: str) -> tuple[str, str]:
    """Splits 'tree-2' into ('tree', '2'). A stem without a number keeps an empty variant."""
    head, _, tail = stem.rpartition("-")
    return (head, tail) if head and tail.isdigit() else (stem, "")


def build_resources(raw: Path, out: Path, quant: Quantiser) -> int:
    count = 0
    for file in sorted((raw / "resources").glob("*/*.png")):
        kind, _ = split_variant(file.stem)
        if kind in DRAWN_KINDS:
            continue
        width, height = RESOURCE_SIZES.get(kind, (0, 0))
        content = crop_to_content(load(file, quant))
        if width and (content.width > width or content.height > height):
            print(f"  note: {file.parent.name}/{file.name} content {content.width}x{content.height} exceeds {width}x{height}")
        save(place_bottom_centre(content, width, height), out / "resources" / file.parent.name / file.name)
        count += 1
    return count


def note_if_clipped(file: Path, content: Image.Image) -> None:
    """Prints a note when the content is larger than the item canvas and so gets clipped."""
    if content.width > ITEM_SIZE or content.height > ITEM_SIZE:
        print(f"  note: {file.parent.name}/{file.name} content {content.width}x{content.height} exceeds {ITEM_SIZE}x{ITEM_SIZE} and is clipped")


def build_items(raw: Path, out: Path, quant: Quantiser) -> int:
    count = 0
    for file in sorted((raw / "items").glob("*.png")):
        content = crop_to_content(load(file, quant))
        note_if_clipped(file, content)
        canvas = Image.new("RGBA", (ITEM_SIZE, ITEM_SIZE), (0, 0, 0, 0))
        canvas.paste(content, ((ITEM_SIZE - content.width) // 2, (ITEM_SIZE - content.height) // 2))
        save(canvas, out / "items" / file.name)
        count += 1
    return count


def build_gui(raw: Path, out: Path, quant: Quantiser) -> int:
    count = 0
    folder = raw / "gui"
    for file in sorted(folder.glob("*.png")):
        content = crop_to_content(load(file, quant))
        note_if_clipped(file, content)
        canvas = Image.new("RGBA", (ITEM_SIZE, ITEM_SIZE), (0, 0, 0, 0))
        canvas.paste(content, ((ITEM_SIZE - content.width) // 2, (ITEM_SIZE - content.height) // 2))
        save(canvas, out / "gui" / file.name)
        count += 1
    full = out / "gui" / "heart-full.png"
    if full.is_file() and not (folder / "heart-empty.png").is_file():
        save(build_empty_heart(Image.open(full).convert("RGBA")), out / "gui" / "heart-empty.png")
        count += 1
    return count


def build_entities(raw: Path, out: Path, quant: Quantiser) -> int:
    count = 0
    for file in sorted((raw / "entities").glob("slime-*.png")):
        save(build_slime_strip(load(file, quant)), out / "entities" / file.name)
        count += 1
    save(build_player_sheet(), out / "entities" / "player.png")
    return count + 1


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description="Quantise and size raw pixel art into the game assets.")
    parser.add_argument("--raw", type=Path, required=True, help="folder with the raw generated images")
    parser.add_argument("--out", type=Path, default=DEFAULT_OUT, help="assets folder the game loads from")
    args = parser.parse_args(argv)
    if not args.raw.is_dir():
        print(f"raw folder not found: {args.raw}", file=sys.stderr)
        return 1

    quant = Quantiser(load_palette(PALETTE_FILE))
    totals = {}
    for label, step in (("resources", build_resources), ("items", build_items),
                        ("gui", build_gui), ("entities", build_entities)):
        print(label)
        totals[label] = step(args.raw, args.out, quant)
    print(json.dumps(totals))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
