"""Draw every building sprite of the build mode with no raw input, 32 px wide and up to 48 px tall.

Every piece is built from isometric prisms (a lit top diamond, a bright left face and a dark right face, light from the upper left), painted in colours of palette.hex and wrapped in the palette outline.

Anchor convention: the footprint of the tile is the 32x16 diamond of a land tile top, and it always occupies the BOTTOM 16 rows of the canvas. The bottom centre pixel of the canvas is therefore the bottom vertex of the tile diamond, and the tile centre is the pixel (16, height - 8). A tree, by contrast, is drawn with its canvas bottom on the tile centre, so a building is drawn 8 art pixels lower than a tree sprite of the same height: to place one, put the bottom centre of the canvas on the bottom vertex of the tile top diamond (the tile centre plus 8 art pixels). The deck sprites (bridge, dock) are flat: their top diamond is the footprint itself. No sprite leaves the 32 px width, so one tile of the map never overlaps a neighbour sideways.

usage: python -I -B tools/pixelart/buildings.py [--out <assets/buildings folder>] [--preview preview.png] [--placed placed.png]
"""
import argparse
import sys
from pathlib import Path

from PIL import Image, ImageDraw

HERE = Path(__file__).resolve().parent
# Isolated mode leaves the script folder off the import path, and trees.py lives next to this file.
sys.path.insert(0, str(HERE))

from trees import OUTLINE, Canvas, hexc  # noqa: E402

WIDTH = 32
DIAMOND_H = 16
DEFAULT_OUT = HERE.parent.parent / "src" / "main" / "resources" / "assets" / "buildings"
ASSETS = HERE.parent.parent / "src" / "main" / "resources" / "assets"

# colour ramps, dark to light
STONE = ["#4a4868", "#7d7a8c", "#aeadb8", "#dcecf8"]
WOOD = ["#4e2f36", "#7a4a3a", "#a0643f", "#c4925a"]
PLANK = ["#7a4a3a", "#a0643f", "#c4925a", "#dba867"]
SAND = ["#7b6040", "#a2825a", "#dba867", "#f4d890"]
IRON = ["#1d1830", "#4a4868", "#7d7a8c", "#aeadb8"]
RED = ["#9c4247", "#d4705c", "#d4705c", "#f4bf9a"]
CREAM = ["#aeadb8", "#dcecf8", "#fcf4e4", "#fcf4e4"]
FIRE = ["#9c4247", "#d4705c", "#ffa02a", "#f4d890"]
GOLD = "#f4d890"


def px(cv, x, y, colour):
    """Paints one pixel with a hex colour."""
    cv.set(x, y, hexc(colour))


def rect(cv, x0, y0, x1, y1, colour):
    """Fills a rectangle, both corners included."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px(cv, x, y, colour)


def finish(cv):
    """Wraps the drawing in the outline and returns the image."""
    cv.outline()
    return cv.image()


def centre(h):
    """Returns the row of the tile centre of a canvas of the given height."""
    return h - DIAMOND_H // 2


def prism(cv, cx, cy, hw, height, ramp, top_fn=None, left_fn=None, right_fn=None, edges=True):
    """Draws an isometric box whose base diamond is centred on (cx, cy), with half width hw and half height hw / 2.

    The top face is lit (ramp[2]), the left face is ramp[1] and the right face ramp[0]. A top_fn(u, v) returning a hex colour or None paints the top face from its own coordinates, u running to the lower right and v to the upper right, both in [-0.5, 0.5]. A left_fn or right_fn(s, t) does the same for a side face with s across 0..1 and t down 0..1.
    """
    hh = hw / 2
    for x in range(cx - hw, cx + hw):
        dx = x + 0.5 - cx
        d = abs(dx) / hw
        half = hh * (1 - d)
        top0 = round(cy - height - half)
        top1 = round(cy - height + half)
        bottom = round(cy + half)
        is_left = dx < 0
        for y in range(top0, bottom):
            if y < top1:
                # the top face
                a, b = dx / hw, (y + 0.5 - (cy - height)) / hh
                u, v = (a + b) / 2, (a - b) / 2
                colour = top_fn(u, v) if top_fn else None
                if colour is None:
                    colour = ramp[2]
                    if edges and y == top0:
                        colour = ramp[3]
                px(cv, x, y, colour)
            else:
                face = ramp[1] if is_left else ramp[0]
                fn = left_fn if is_left else right_fn
                s = (x - (cx - hw)) / hw if is_left else (x - cx) / hw
                t = (y - top1) / max(1, bottom - top1)
                colour = fn(s, t) if fn else None
                if colour is not None:
                    face = colour
                px(cv, x, y, face)
    if edges and height > 1:
        # a dark seam where the two faces meet, and a light rim along the top edge of the left face
        for y in range(round(cy - height + hh), round(cy + hh) - 0):
            px(cv, cx, y, ramp[0])
            px(cv, cx - 1, y, ramp[0])


def brick_fn(ramp, rows, per_face=2):
    """Returns a face painter that lays brick courses: mortar lines between rows and staggered joints."""
    def paint(s, t):
        row = int(t * rows)
        within = t * rows - row
        if within < 1 / (rows * 0.9) * 0.55 and row > 0:
            return ramp[0]
        offset = 0.5 / per_face if row % 2 else 0.0
        col = ((s + offset) * per_face) % 1.0
        if col < 0.09:
            return ramp[0]
        return None
    return paint


# ----------------------------------------------------------------------------------------------------------------
# buildings
# ----------------------------------------------------------------------------------------------------------------

def wall():
    h = 36
    cv = Canvas(WIDTH, h)
    prism(cv, 16, centre(h), 15, 16, STONE, top_fn=cap_stones, left_fn=brick_fn(STONE, 4), right_fn=brick_fn(STONE, 4))
    return finish(cv)


def cap_stones(u, v):
    """The top of a wall: a lighter slab with a darker joint line across it."""
    if abs(u - v) < 0.04:
        return STONE[1]
    return STONE[2]


def fence():
    h = 22
    cv = Canvas(WIDTH, h)
    cy = centre(h)
    # two posts at the middle of the north east and south west edges, two rails between them
    posts = [(cx, cy + dy) for cx, dy in ((8, 4), (24, -4))]
    for cx, foot in posts:
        rect(cv, cx - 1, foot - 11, cx, foot, PLANK[1])
        rect(cv, cx - 1, foot - 11, cx - 1, foot, PLANK[2])
        rect(cv, cx, foot - 11, cx, foot, PLANK[0])
        px(cv, cx - 1, foot - 12, PLANK[3])
        px(cv, cx, foot - 12, PLANK[2])
        px(cv, cx - 1, foot - 11, PLANK[3])
    for lift in (4, 8):
        for x in range(9, 24):
            y = cy + 4 - round((x - 8) * 0.5) - lift
            px(cv, x, y, PLANK[2])
            px(cv, x, y + 1, PLANK[1])
    return finish(cv)


def chest():
    h = 24
    cv = Canvas(WIDTH, h)

    def side(s, t):
        if 0.4 < s < 0.62:
            return IRON[2] if t > 0.36 else IRON[3]
        if t < 0.34:
            return WOOD[3]
        if t < 0.42:
            return IRON[1]
        return None

    def side_dark(s, t):
        if 0.4 < s < 0.62:
            return IRON[1]
        if t < 0.34:
            return WOOD[2]
        if t < 0.42:
            return IRON[0]
        return None

    def lid(u, v):
        if abs(u + v) < 0.06:
            return IRON[2]
        return WOOD[3]

    cy = centre(h)
    prism(cv, 16, cy, 11, 10, WOOD, top_fn=lid, left_fn=side, right_fn=side_dark)
    # the gold clasp where the lid meets the body on the front corner
    rect(cv, 14, cy + 3 - 7, 17, cy + 3 - 4, GOLD)
    rect(cv, 15, cy + 3 - 6, 16, cy + 3 - 5, IRON[0])
    return finish(cv)


def anvil():
    h = 28
    cv = Canvas(WIDTH, h)
    cy = centre(h)
    prism(cv, 16, cy, 8, 3, IRON)
    prism(cv, 16, cy - 3, 4, 5, IRON)
    # the face: a wide slab that sticks out to the left as a horn
    prism(cv, 16, cy - 8, 9, 4, IRON, top_fn=lambda u, v: IRON[3] if u + v < 0 else IRON[2])
    for i, x in enumerate((5, 6, 7, 8)):
        rect(cv, x, cy - 13 + i // 2, x, cy - 10, IRON[2] if i < 2 else IRON[1])
    return finish(cv)


def workbench():
    h = 30
    cv = Canvas(WIDTH, h)
    cy = centre(h)
    # four legs under the table top
    for lx, ly in ((6, 3), (25, 3), (16, 9), (16, -3)):
        if ly == -3:
            continue
        rect(cv, lx - 1, cy + ly - 10, lx, cy + ly, PLANK[0] if lx > 16 else PLANK[1])

    def boards(u, v):
        n = int((v + 0.5) * 5)
        if abs(((v + 0.5) * 5) - round((v + 0.5) * 5)) < 0.12:
            return PLANK[1]
        return PLANK[3] if n % 2 else PLANK[2]

    prism(cv, 16, cy - 9, 14, 4, PLANK, top_fn=boards)
    # a saw lying on the top
    rect(cv, 11, cy - 17, 17, cy - 17, IRON[3])
    rect(cv, 18, cy - 18, 20, cy - 17, WOOD[1])
    px(cv, 12, cy - 16, IRON[2])
    px(cv, 14, cy - 16, IRON[2])
    px(cv, 16, cy - 16, IRON[2])
    return finish(cv)


def bed():
    h = 30
    cv = Canvas(WIDTH, h)
    cy = centre(h)
    # the headboard rises behind the mattress along the upper right edge
    for x in range(16, 30):
        edge = round(cy - 6 - (7 - 0.5) * (1 - (x + 0.5 - 16) / 14) * 0 - (14 - (x - 16)) * 0.5)
        for y in range(edge - 7, edge + 1):
            px(cv, x, y, WOOD[2] if y < edge - 5 else WOOD[1])
        px(cv, x, edge - 7, WOOD[3])

    def top(u, v):
        if v > 0.12:
            return CREAM[2] if u < 0.12 else CREAM[1]
        if v > -0.5:
            return RED[3] if abs(v + 0.12) < 0.03 else (RED[2] if (u + v) < 0.1 else RED[1])
        return None

    prism(cv, 16, cy, 14, 6, WOOD, top_fn=top, left_fn=lambda s, t: WOOD[2] if t < 0.4 else WOOD[1])
    return finish(cv)


def door(opened):
    h = 36
    cv = Canvas(WIDTH, h)

    def panel(lit):
        def paint(s, t):
            # a round topped doorway in the middle of the face
            inside = 0.26 < s < 0.74 and t > 0.2
            if not inside:
                return brick_fn(STONE, 4)(s, t)
            if t < 0.3 and (s < 0.3 or s > 0.7):
                return STONE[1] if lit else STONE[0]
            if opened:
                return IRON[0] if s > 0.32 and s < 0.68 else WOOD[0]
            if s < 0.32 or s > 0.68:
                return WOOD[0]
            if abs(t - 0.62) < 0.03:
                return WOOD[1]
            if 0.44 < s < 0.52 and 0.5 < t < 0.6:
                return GOLD
            return WOOD[3] if lit else WOOD[2]
        return paint

    prism(cv, 16, centre(h), 15, 16, STONE, top_fn=cap_stones, left_fn=panel(True), right_fn=panel(False))
    return finish(cv)


def deck_top(u, v):
    """Planks running along one axis with a dark gap between neighbours."""
    n = (v + 0.5) * 8
    if abs(n - round(n)) < 0.13:
        return PLANK[0]
    k = int(n)
    return [PLANK[2], PLANK[3], PLANK[1]][k % 3]


def bridge():
    h = 20
    cv = Canvas(WIDTH, h)
    prism(cv, 16, centre(h) + 0, 15, 3, PLANK, top_fn=deck_top, edges=False)
    return finish(cv)


def dock():
    h = 28
    cv = Canvas(WIDTH, h)
    cy = centre(h)
    # posts going down to the water under the deck corners
    for px_, py_ in ((3, 0), (28, 0), (16, 7)):
        rect(cv, px_ - 1, cy + py_ - 6, px_ + 1, cy + py_ + 4, WOOD[1])
        rect(cv, px_ - 1, cy + py_ - 6, px_ - 1, cy + py_ + 4, WOOD[2])
        rect(cv, px_ + 1, cy + py_ - 6, px_ + 1, cy + py_ + 4, WOOD[0])
    prism(cv, 16, cy - 5, 15, 3, PLANK, top_fn=deck_top, edges=False)
    # the post that stands proud of the deck at the lower corner
    rect(cv, 15, cy + 3 - 6, 17, cy + 3, WOOD[1])
    rect(cv, 15, cy + 3 - 6, 15, cy + 3, WOOD[2])
    rect(cv, 17, cy + 3 - 6, 17, cy + 3, WOOD[0])
    px(cv, 16, cy + 3 - 7, WOOD[3])
    return finish(cv)


def furnace(lit):
    h = 38
    cv = Canvas(WIDTH, h)
    cy = centre(h)

    def left_face(s, t):
        if 0.25 < s < 0.75 and t > 0.3:
            if t < 0.4 and not 0.35 < s < 0.65:
                return STONE[0]
            if not lit:
                return IRON[0]
            glow = abs(s - 0.5) * 2
            return FIRE[3] if glow < 0.25 and t > 0.6 else FIRE[2] if glow < 0.7 else FIRE[1]
        return brick_fn(STONE, 3, 2)(s, t)

    prism(cv, 16, cy, 12, 14, STONE, top_fn=cap_stones, left_fn=left_face, right_fn=brick_fn(STONE, 3, 2))
    # the chimney on the top
    prism(cv, 18, cy - 12, 4, 7, STONE, top_fn=lambda u, v: IRON[0], left_fn=brick_fn(STONE, 2, 1))
    if lit:
        # light spilling on the ground in front of the opening
        for x, y in ((8, cy + 2), (9, cy + 3), (10, cy + 3), (11, cy + 4)):
            px(cv, x, y, FIRE[1])
    return finish(cv)


FLAME_HEIGHT = [8, 10, 7, 10]
FLAME_SWAY = [0, 1, 0, -1]


def flame(cv, cx, base, frame, scale=1.0):
    """Paints a flame whose lower end is at row base: an orange body, a yellow heart and a pale core, swaying per frame."""
    top = round(FLAME_HEIGHT[frame] * scale)
    sway = FLAME_SWAY[frame]
    for k in range(top):
        t = k / max(1, top - 1)  # 0 at the tip, 1 at the base
        half = round((1.0 + 3.2 * scale) * (t ** 0.55)) if t > 0.0 else 0
        row = base - top + k + 1
        shift = round(sway * (1 - t))
        width_outer = half
        rect(cv, cx + shift - width_outer, row, cx + shift + width_outer, row, FIRE[2])
        if t > 0.35:
            w2 = max(0, round(half * 0.62))
            rect(cv, cx + shift - w2, row, cx + shift + w2, row, FIRE[3])
        if t > 0.7:
            w3 = max(0, round(half * 0.3))
            rect(cv, cx + shift - w3, row, cx + shift + w3, row, CREAM[2])
        if t > 0.8:
            px(cv, cx + shift - half, row, FIRE[1])
            px(cv, cx + shift + half, row, FIRE[1])
    # a spark that jumps around
    px(cv, cx + [3, -3, 2, -2][frame], base - top - [1, 2, 0, 1][frame], FIRE[3])


def campfire(frame):
    h = 32
    cv = Canvas(WIDTH, h)
    cy = centre(h)
    # a ring of stones on an ellipse
    import math
    n = 11
    for i in range(n):
        a = i / n * 2 * math.pi
        sx = round(16 + 11 * math.cos(a))
        sy = round(cy + 5.2 * math.sin(a))
        light = math.cos(a + 0.8) > 0.0
        rect(cv, sx - 1, sy - 1, sx + 1, sy + 1, STONE[2] if light else STONE[1])
        px(cv, sx - 1, sy - 1, STONE[3] if light else STONE[2])
        px(cv, sx + 1, sy + 1, STONE[0])
    # the embers inside the ring
    for y in range(cy - 3, cy + 4):
        for x in range(8, 25):
            if ((x - 16) / 8.5) ** 2 + ((y - cy) / 4.0) ** 2 <= 1:
                px(cv, x, y, "#4e2f36")
    # two crossed logs
    for (x0, y0, x1, y1) in ((9, cy - 2, 22, cy + 3), (10, cy + 3, 23, cy - 2)):
        steps = 13
        for i in range(steps + 1):
            x = x0 + (x1 - x0) * i // steps
            y = y0 + (y1 - y0) * i // steps
            rect(cv, x, y - 1, x, y, WOOD[2])
            px(cv, x, y + 1, WOOD[0])
    rect(cv, 22, cy - 1, 23, cy + 3, WOOD[3])
    flame(cv, 16, cy + 1, frame)
    return finish(cv)


def standing_torch(frame):
    h = 40
    cv = Canvas(WIDTH, h)
    cy = centre(h)
    # a few stones at the foot, then the post
    rect(cv, 12, cy, 20, cy + 2, STONE[1])
    rect(cv, 13, cy - 1, 19, cy - 1, STONE[2])
    rect(cv, 12, cy, 13, cy + 1, STONE[2])
    rect(cv, 19, cy + 1, 20, cy + 2, STONE[0])
    rect(cv, 15, cy - 17, 17, cy - 1, WOOD[1])
    rect(cv, 15, cy - 17, 15, cy - 1, WOOD[2])
    rect(cv, 17, cy - 17, 17, cy - 1, WOOD[0])
    # the cup that holds the fire
    rect(cv, 13, cy - 20, 19, cy - 18, IRON[1])
    rect(cv, 13, cy - 20, 19, cy - 20, IRON[2])
    rect(cv, 18, cy - 19, 19, cy - 18, IRON[0])
    flame(cv, 16, cy - 20, frame, 0.8)
    return finish(cv)


SPRITES = {
    "wall": wall,
    "fence": fence,
    "chest": chest,
    "anvil": anvil,
    "workbench": workbench,
    "bed": bed,
    "door_closed": lambda: door(False),
    "door_open": lambda: door(True),
    "bridge": bridge,
    "dock": dock,
    "furnace_off": lambda: furnace(False),
    "furnace_on": lambda: furnace(True),
}
for _i in range(4):
    SPRITES[f"campfire_{_i}"] = (lambda i=_i: campfire(i))
    SPRITES[f"standing_torch_{_i}"] = (lambda i=_i: standing_torch(i))


def draw_all():
    """Returns every (name, image) pair, in table order."""
    return [(name, maker()) for name, maker in SPRITES.items()]


def write_all(out):
    """Writes every sprite into the folder and returns how many files it wrote."""
    out.mkdir(parents=True, exist_ok=True)
    count = 0
    for name, img in draw_all():
        img.save(out / f"{name}.png")
        count += 1
    return count


def preview(path, scale=4):
    """Renders every sprite on grass green and on water blue, enlarged with no smoothing and labelled."""
    sprites = draw_all()
    cols = 6
    cell_w, cell_h = WIDTH + 8, 48 + 6
    rows = (len(sprites) + cols - 1) // cols
    panels = []
    for bg in ((0x59, 0xa8, 0x4a, 255), (0x38, 0x9c, 0xc4, 255)):
        sheet = Image.new("RGBA", (cols * cell_w, rows * cell_h), bg)
        for i, (_, img) in enumerate(sprites):
            x, y = (i % cols) * cell_w + 4, (i // cols) * cell_h + 48 + 3 - img.height
            sheet.alpha_composite(img, (x, y))
        panels.append(sheet.resize((sheet.width * scale, sheet.height * scale), Image.NEAREST))
    out = Image.new("RGBA", (panels[0].width, panels[0].height * 2), (0, 0, 0, 255))
    for k, panel in enumerate(panels):
        out.alpha_composite(panel, (0, k * panel.height))
    draw = ImageDraw.Draw(out)
    for i, (name, _) in enumerate(sprites):
        draw.text(((i % cols) * cell_w * scale + 6, (i // cols) * cell_h * scale + 2), name, fill=(255, 255, 255, 255))
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    out.save(path)


def placed(path, scale=3):
    """Renders a small iso map of grass tiles with a pond, and pieces placed on it at their anchor, to check they sit on the tile."""
    grass = Image.open(ASSETS / "tiles" / "grass-1.png").convert("RGBA")
    bed = Image.open(ASSETS / "tiles" / "bed-1.png").convert("RGBA")
    sheet = Image.open(ASSETS / "water" / "sheet.png").convert("RGBA")
    sprites = dict(draw_all())
    n = 7
    pond = {(3, 1), (3, 2), (3, 3), (3, 4), (2, 3), (4, 3), (2, 4), (4, 4), (4, 2), (2, 2)}
    layout = {
        (1, 1): "wall", (1, 2): "door_closed", (1, 3): "door_open", (0, 4): "fence", (0, 5): "fence",
        (1, 5): "chest", (2, 5): "anvil", (2, 0): "workbench", (3, 0): "bed", (4, 1): "furnace_off", (5, 1): "furnace_on",
        (5, 3): "campfire_1", (5, 4): "standing_torch_1", (4, 5): "bridge", (3, 3): "bridge", (4, 4): "dock",
    }
    w, h = (n * 2) * 16 + 32, n * 16 + 64
    img = Image.new("RGBA", (w, h), (0x1d, 0x18, 0x30, 255))
    ox, oy = w // 2 - 16, 40
    for i in range(n):
        for j in range(n):
            x, y = ox + (i - j) * 16, oy + (i + j) * 8
            if (i, j) in pond:
                img.alpha_composite(bed, (x, y + 10))
                img.alpha_composite(sheet, (x, y + 5))
            else:
                img.alpha_composite(grass, (x, y))
        # sprites of this diagonal band are drawn after the tiles of the same band
    for s in range(2 * n - 1):
        for i in range(n):
            j = s - i
            if (i, j) in layout:
                sprite = sprites[layout[(i, j)]]
                x, y = ox + (i - j) * 16, oy + (i + j) * 8
                # the bottom centre of the canvas goes on the bottom vertex of the tile top diamond
                img.alpha_composite(sprite, (x, y + DIAMOND_H - sprite.height))
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    img.resize((img.width * scale, img.height * scale), Image.NEAREST).save(path)


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--out", default=str(DEFAULT_OUT))
    parser.add_argument("--preview")
    parser.add_argument("--placed")
    args = parser.parse_args()
    if args.preview or args.placed:
        if args.preview:
            preview(args.preview)
        if args.placed:
            placed(args.placed)
        return
    print(f"buildings {write_all(Path(args.out))}")


if __name__ == "__main__":
    main()
