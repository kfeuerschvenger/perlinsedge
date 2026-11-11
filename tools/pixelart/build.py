"""Post-process raw generated pixel art into the sprites the game loads.

Usage: python -I tools/pixelart/build.py --raw <dir> [--out <dir>]

The raw folder is local and never committed. The tool is idempotent: it rebuilds every output it finds an input for,
so it can be re-run whenever raw files are added or replaced.
"""

from __future__ import annotations

import argparse
import json
import sys
import tempfile
import zipfile
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
REPO = HERE.parent.parent
DEFAULT_RAW = REPO / "docs" / "private" / "assets-raw" / "batch1"
DEFAULT_OUT = REPO / "src" / "main" / "resources" / "assets"
PALETTE_FILE = HERE / "palette.hex"

ALPHA_CUTOFF = 128
TILE_W = 32
TILE_TOP_H = 16
LAND_FACE = 5
WATER_FACE = 3
MOUNTAIN_FACE = 10
ITEM_SIZE = 16
PLAYER_CELL = (32, 56)
PLAYER_COLUMNS = 8
PLAYER_ROWS = 3
SLIME_CELL = (20, 16)
SLIME_FRAMES = 6
EMPTY_HEART_FILL = "4a4868"
OUTLINE = "1d1830"

# Logical canvas per resource kind (width, height). A kind not listed keeps its content size.
RESOURCE_SIZES = {
    "tree": (32, 48),
    "willow": (40, 48),
    "palm": (32, 48),
    "pine": (28, 48),
    "cactus": (24, 40),
    "bush": (24, 20),
    "shrub": (24, 20),
    "fern": (16, 14),
    "reed": (16, 20),
    "rock": (24, 20),
    "iron": (24, 20),
    "crystal": (24, 24),
}

# Sheet column to the cardinal pose it shows. Columns follow the screen compass: UP, UP_RIGHT, RIGHT, DOWN_RIGHT,
# DOWN, DOWN_LEFT, LEFT, UP_LEFT. A diagonal reuses the horizontal pose of its side, east on the right and west on
# the left, so the facing never flips between a cardinal and its diagonal neighbour.
COLUMN_POSES = ["north", "east", "east", "east", "south", "west", "west", "west"]

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
# Terrain
# ----------------------------------------------------------------------------------------------------------------

def diamond_contains(x: int, r: int) -> bool:
    """Whether column x of row r lies inside the 32x16 top diamond, which grows two pixels per side per row."""
    if r < 0 or r >= TILE_TOP_H:
        return False
    half = 2 * (r + 1) if r < TILE_TOP_H // 2 else 2 * (TILE_TOP_H - r)
    return TILE_W // 2 - half <= x < TILE_W // 2 + half


def block_geometry(image: Image.Image) -> tuple[int, int]:
    """Returns the row of the top diamond apex and the face thickness of a raw isometric block.

    The block ends on a bottom vertex at the last opaque row, which fixes the lower diamond. The left and right
    tips of the top diamond sit seven rows under its apex, and tufts only ever add pixels above the apex, so the
    first opaque row of the two edge columns locates it.
    """
    alpha = image.getchannel("A")
    rows = [y for y in range(image.height) for x in range(image.width) if alpha.getpixel((x, y)) >= ALPHA_CUTOFF]
    bottom = max(rows)
    lower_apex = bottom - (TILE_TOP_H - 1)
    tips = []
    for x in (0, image.width - 1):
        column = [y for y in range(image.height) if alpha.getpixel((x, y)) >= ALPHA_CUTOFF]
        if column:
            tips.append(column[0])
    apex = max(tips) - (TILE_TOP_H // 2 - 1)
    return apex, max(0, lower_apex - apex)


def face_for(name: str) -> int:
    if name.startswith("water") or "water" in name:
        return WATER_FACE
    if name.startswith("mountain"):
        return MOUNTAIN_FACE
    return LAND_FACE


def clip_block(raw: Image.Image, face: int) -> Image.Image:
    """Rebuilds a raw block as a 32 x (16 + face) tile clipped to the isometric block silhouette.

    The top diamond comes from the raw top, so tufts above it are dropped. The side faces come from the bottom of the
    raw faces, which keeps the shaded stripe along the lower edge and removes the extra thickness.
    """
    apex, raw_face = block_geometry(raw)
    out = Image.new("RGBA", (TILE_W, TILE_TOP_H + face), (0, 0, 0, 0))
    src = raw.load()
    dst = out.load()
    for y in range(out.height):
        for x in range(TILE_W):
            if diamond_contains(x, y):
                sy = apex + y
            elif diamond_contains(x, y - face):
                sy = y + apex + (raw_face - face)
            else:
                continue
            if 0 <= sy < raw.height and x < raw.width:
                dst[x, y] = src[x, sy]
    return out


# ----------------------------------------------------------------------------------------------------------------
# Entities
# ----------------------------------------------------------------------------------------------------------------

def mirror(image: Image.Image) -> Image.Image:
    return image.transpose(Image.Transpose.FLIP_LEFT_RIGHT)


def fit_frames(frames: dict[str, Image.Image], cell: tuple[int, int], quant: Quantiser) -> dict[str, Image.Image]:
    """Crops all poses to one shared region and places them in the cell on one ground line and one centre.

    Poses keep their native pixels, so a pose never moves relative to the others and a walk cycle does not jitter.
    Only content larger than the cell is scaled down, by one shared factor, with premultiplied resampling; the result
    goes back through the palette and the binary alpha.
    """
    boxes = {key: content_box(img) for key, img in frames.items()}
    centre = next(iter(frames.values())).width / 2
    left = min(box[0] for box in boxes.values())
    right = max(box[2] for box in boxes.values())
    top = min(box[1] for box in boxes.values())
    bottom = max(box[3] for box in boxes.values())
    half = max(centre - left, right - centre)
    region = (int(centre - half), top, int(centre + half + 0.999), bottom)
    scale = min(cell[0] / (region[2] - region[0]), cell[1] / (region[3] - region[1]), 1.0)
    out = {}
    for key, img in frames.items():
        crop = img.crop(region).convert("RGBa")
        size = (max(1, round(crop.width * scale)), max(1, round(crop.height * scale)))
        small = crop.resize(size, Image.Resampling.LANCZOS).convert("RGBA") if scale < 1.0 else crop.convert("RGBA")
        small = quant.apply(small)
        cell_img = Image.new("RGBA", cell, (0, 0, 0, 0))
        cell_img.paste(small, ((cell[0] - small.width) // 2, cell[1] - small.height))
        out[key] = cell_img
    return out


def read_player_poses(zip_path: Path, work: Path) -> tuple[dict[str, Image.Image], dict[str, list[Image.Image]]]:
    """Reads the rotations and the walk frames per cardinal direction from the character archive."""
    with zipfile.ZipFile(zip_path) as archive:
        archive.extractall(work)
    rotations: dict[str, Image.Image] = {}
    walks: dict[str, list[Image.Image]] = {}
    for state in sorted(p for p in work.iterdir() if p.is_dir()):
        for direction in ("south", "east", "north", "west"):
            rot = state / "rotations" / f"{direction}.png"
            if rot.is_file() and direction not in rotations:
                rotations[direction] = Image.open(rot).convert("RGBA")
        animations = state / "animations"
        if not animations.is_dir():
            continue
        for animation in sorted(p for p in animations.iterdir() if p.is_dir() and "walk" in p.name.lower()):
            for direction in ("south", "east", "north", "west"):
                folder = animation / direction
                files = sorted(folder.glob("frame_*.png")) if folder.is_dir() else []
                if files and direction not in walks:
                    walks[direction] = [Image.open(f).convert("RGBA") for f in files]
    return rotations, walks


def complete_poses(rotations: dict[str, Image.Image], walks: dict[str, list[Image.Image]]) -> None:
    """Fills a missing east or west from its mirror, in place."""
    for direction, opposite in (("west", "east"), ("east", "west")):
        if direction not in rotations and opposite in rotations:
            rotations[direction] = mirror(rotations[opposite])
        if direction not in walks and opposite in walks:
            walks[direction] = [mirror(f) for f in walks[opposite]]


def build_player_sheet(zip_path: Path, quant: Quantiser) -> Image.Image:
    """Builds the 8 column by 3 row sheet: walk a, idle, walk b."""
    with tempfile.TemporaryDirectory(prefix="pixelart-player-") as tmp:
        rotations, walks = read_player_poses(zip_path, Path(tmp))
    complete_poses(rotations, walks)
    missing = [d for d in ("south", "east", "north", "west") if d not in rotations]
    if missing:
        raise SystemExit(f"player archive has no rotation for: {', '.join(missing)}")

    frames: dict[str, Image.Image] = {}
    for direction, rotation in rotations.items():
        walk = walks.get(direction)
        if walk:
            frames[f"{direction}:a"] = walk[len(walk) // 4]
            frames[f"{direction}:b"] = walk[3 * len(walk) // 4]
        else:
            frames[f"{direction}:a"] = rotation
            frames[f"{direction}:b"] = rotation
        frames[f"{direction}:idle"] = rotation
    cells = fit_frames(frames, PLAYER_CELL, quant)

    sheet = Image.new("RGBA", (PLAYER_CELL[0] * PLAYER_COLUMNS, PLAYER_CELL[1] * PLAYER_ROWS), (0, 0, 0, 0))
    for column, pose in enumerate(COLUMN_POSES):
        for row, tag in enumerate(("a", "idle", "b")):
            sheet.paste(cells[f"{pose}:{tag}"], (column * PLAYER_CELL[0], row * PLAYER_CELL[1]))
    return sheet


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


def build_tiles(raw: Path, out: Path, quant: Quantiser) -> int:
    count = 0
    for file in sorted((raw / "tiles").glob("*.png")):
        name = file.stem.removeprefix("tile-")
        base, _ = split_variant(name)
        save(clip_block(load(file, quant), face_for(base)), out / "tiles" / f"{name}.png")
        count += 1
    return count


def build_resources(raw: Path, out: Path, quant: Quantiser) -> int:
    count = 0
    for file in sorted((raw / "resources").glob("*/*.png")):
        kind, _ = split_variant(file.stem)
        width, height = RESOURCE_SIZES.get(kind, (0, 0))
        content = crop_to_content(load(file, quant))
        if width and (content.width > width or content.height > height):
            print(f"  note: {file.parent.name}/{file.name} content {content.width}x{content.height} exceeds {width}x{height}")
        save(place_bottom_centre(content, width, height), out / "resources" / file.parent.name / file.name)
        count += 1
    return count


def build_items(raw: Path, out: Path, quant: Quantiser) -> int:
    count = 0
    for file in sorted((raw / "items").glob("*.png")):
        content = crop_to_content(load(file, quant))
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
    archive = raw / "player" / "explorer.zip"
    if archive.is_file():
        save(build_player_sheet(archive, quant), out / "entities" / "player.png")
        count += 1
    return count


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description="Quantise and size raw pixel art into the game assets.")
    parser.add_argument("--raw", type=Path, default=DEFAULT_RAW, help="folder with the raw generated images")
    parser.add_argument("--out", type=Path, default=DEFAULT_OUT, help="assets folder the game loads from")
    args = parser.parse_args(argv)
    if not args.raw.is_dir():
        print(f"raw folder not found: {args.raw}", file=sys.stderr)
        return 1

    quant = Quantiser(load_palette(PALETTE_FILE))
    totals = {}
    for label, step in (("tiles", build_tiles), ("resources", build_resources), ("items", build_items),
                        ("gui", build_gui), ("entities", build_entities)):
        print(label)
        totals[label] = step(args.raw, args.out, quant)
    print(json.dumps(totals))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
