"""Draw every building sprite of the build mode with no raw input, 32 px wide and up to 48 px tall.

Every piece is a stack of axis aligned boxes in a tile local space and is rendered by a small ray caster, so a rotated piece is the same object seen from another side (a back view shows the back, never a mirror of the front). Light comes from the upper left, the colours are those of palette.hex and the drawing is wrapped in the palette outline.

Tile space: the origin is the tile centre, X grows towards the lower right edge and Y towards the lower left edge, both from -8 to 8 pixels, and Z grows up. A point projects to the screen at (X - Y, (X + Y) / 2 - Z) from the tile centre.

Anchor convention: the tile footprint (the 32x16 diamond of a land tile top) fills the bottom 16 rows of the canvas. The bottom centre pixel of a sprite is the bottom vertex of the tile diamond and the tile centre is the pixel (16, height - 8). A tree is drawn with its canvas bottom on the tile centre, so a building is drawn 8 art pixels lower than a tree of the same height. Every sprite of one family has the same height for all its orientations.

Orientation of 4 way pieces (suffix _0 to _3): the front faces the lower right edge (0), the lower left edge (1), the upper left edge (2) and the upper right edge (3), each step a quarter turn clockwise seen from above. 2 way pieces: axis 0 runs between the upper left and lower right edges, axis 1 between the upper right and lower left edges.

Fence parts are drawn to be composed by the game on the same anchor: the rails of a neighbour reach the middle of the tile edge, where the next tile's rail or a gate post continues them at the same heights.

usage: python -I -B tools/pixelart/buildings.py [--out <assets/buildings folder>] [--preview sheet.png] [--placed scene.png] [--orient scene.png]
"""
import argparse
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw

HERE = Path(__file__).resolve().parent
# Isolated mode leaves the script folder off the import path, and trees.py lives next to this file.
sys.path.insert(0, str(HERE))

from trees import OUTLINE, Canvas, hexc  # noqa: E402

WIDTH = 32
DIAMOND_H = 16
MAX_H = 48
DEFAULT_OUT = HERE.parent.parent / "src" / "main" / "resources" / "assets" / "buildings"
ASSETS = HERE.parent.parent / "src" / "main" / "resources" / "assets"

# colour ramps, dark to light: right face, left face, top, top rim
STONE = ["#4a4868", "#7d7a8c", "#aeadb8", "#dcecf8"]
WOOD = ["#4e2f36", "#7a4a3a", "#a0643f", "#c4925a"]
PLANK = ["#7a4a3a", "#a0643f", "#c4925a", "#dba867"]
SAND = ["#7b6040", "#a2825a", "#dba867", "#f4d890"]
IRON = ["#1d1830", "#4a4868", "#7d7a8c", "#aeadb8"]
RED = ["#9c4247", "#d4705c", "#d4705c", "#f4bf9a"]
CREAM = ["#aeadb8", "#dcecf8", "#fcf4e4", "#fcf4e4"]
BRASS = ["#a2825a", "#dba867", "#f4d890", "#f4d890"]
FIRE = ["#9c4247", "#d4705c", "#ffa02a", "#f4d890"]

RAIL_LOW = (2.5, 4.5)
RAIL_HIGH = (8.5, 10.5)
POST_HEIGHT = 13.0
GATE_POST_HEIGHT = 17.0
# half thickness of a rail and half width of a post, in tile pixels
RH = 0.75
PH = 1.0


# ----------------------------------------------------------------------------------------------------------------
# the box ray caster
# ----------------------------------------------------------------------------------------------------------------

def box(x0, x1, y0, y1, z0, z1, ramp, paint=None, ghost=False):
    """A box of tile space. A ghost box hides what is behind it and paints nothing, which the fence rails use to stop at the post."""
    return {"b": (x0, x1, y0, y1, z0, z1), "ramp": ramp, "paint": paint, "ghost": ghost}


def rot(x, y, k):
    """Turns a tile point k quarter turns, which moves the object front from the lower right edge to the lower left edge and on."""
    for _ in range(k % 4):
        x, y = -y, x
    return x, y


def unrot(x, y, k):
    """The inverse of rot."""
    for _ in range(k % 4):
        x, y = y, -x
    return x, y


def face_name(dx, dy):
    return {(1, 0): "+x", (-1, 0): "-x", (0, 1): "+y", (0, -1): "-y"}[(dx, dy)]


def render(boxes, k=0, h=MAX_H):
    """Ray casts the boxes turned k quarter turns onto a canvas of height h and returns the outlined image."""
    cv = Canvas(WIDTH, h)
    cy = h - DIAMOND_H // 2
    world = []
    for bx in boxes:
        x0, x1, y0, y1, z0, z1 = bx["b"]
        ax, ay = rot(x0, y0, k)
        bx_, by_ = rot(x1, y1, k)
        world.append((min(ax, bx_), max(ax, bx_), min(ay, by_), max(ay, by_), z0, z1, bx))
    masked = set()
    for py in range(h):
        sy = py + 0.5 - cy
        for px_ in range(WIDTH):
            sx = px_ + 0.5 - WIDTH // 2
            best = None
            for wx0, wx1, wy0, wy1, wz0, wz1, bx in world:
                lo = max(2 * wx0 - sx, 2 * wy0 + sx, 2 * (wz0 + sy))
                hx, hy, hz = 2 * wx1 - sx, 2 * wy1 + sx, 2 * (wz1 + sy)
                hi = min(hx, hy, hz)
                if hi - lo > 1e-6 and (best is None or hi > best[0] + 1e-9):
                    best = (hi, "x" if hx <= hi + 1e-9 else "y" if hy <= hi + 1e-9 else "z", wx0, wy0, bx, wx1, wy1)
            if best is None:
                continue
            w, side, wx0, wy0, bx, wx1, wy1 = best
            if bx["ghost"]:
                masked.add((px_, py))
                continue
            wxp, wyp, zp = (w + sx) / 2, (w - sx) / 2, w / 2 - sy
            shade = {"x": 0, "y": 1, "z": 2}[side]
            ramp = bx["ramp"]
            colour = ramp[shade]
            if side == "z" and min(wx1 - wx0, wy1 - wy0) >= 4 and (wxp - wx0 < 0.75 or wyp - wy0 < 0.75):
                colour = ramp[3]
            if bx["paint"]:
                ox, oy = unrot(wxp, wyp, k)
                if side == "z":
                    face = "top"
                else:
                    dx, dy = unrot(1 if side == "x" else 0, 1 if side == "y" else 0, k)
                    face = face_name(dx, dy)
                painted = bx["paint"](face, ox, oy, zp, shade)
                if painted:
                    colour = painted
            cv.set(px_, py, hexc(colour))
    # the outline, skipping the pixels a ghost box covers so it never overwrites a neighbouring part
    out = [r[:] for r in cv.px]
    for y in range(h):
        for x in range(WIDTH):
            if cv.px[y][x] is None and (x, y) not in masked and any(
                    0 <= x + dx < WIDTH and 0 <= y + dy < h and cv.px[y + dy][x + dx] is not None
                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                out[y][x] = OUTLINE
    cv.px = out
    return cv.image()


def top_row(img):
    """The first row of the image that holds a painted pixel."""
    for y in range(img.height):
        if any(img.getpixel((x, y))[3] for x in range(img.width)):
            return y
    return img.height


def family(boxes, ks, h=None):
    """Renders a piece for every orientation in ks on one shared height, the smallest that holds every orientation."""
    images = [render(boxes, k) for k in ks]
    keep = h if h else MAX_H - min(top_row(i) for i in images)
    return [i.crop((0, MAX_H - keep, WIDTH, MAX_H)) for i in images]


# ----------------------------------------------------------------------------------------------------------------
# face painters, in the object space where the front is +x
# ----------------------------------------------------------------------------------------------------------------

def bricks(ramp, course, joint, skip_top=True):
    """Returns a painter that lays courses of bricks with staggered joints."""
    def paint(face, x, y, z, shade):
        if face == "top":
            return None
        a = y if face in ("+x", "-x") else x
        row = int(max(0, z) // course)
        mortar = ramp[max(0, shade - 1)] if shade > 0 else "#33304f"
        if z % course < 0.8 and z > 0.8:
            return mortar
        if (a + 20 + (joint / 2 if row % 2 else 0)) % joint < 0.8:
            return mortar
        return None
    return paint


def chest_paint(face, x, y, z, shade):
    if face == "top":
        return None
    # the metal band that circles the body, and corner plates at the foot
    if 2.2 <= z < 3.4:
        return IRON[2 if shade == 1 else 1]
    if abs(y) > 5.3 and face in ("+x", "-x") and (z < 1.5 or 4.6 <= z < 6.0):
        return IRON[2 if shade == 1 else 1]
    # the lid seam
    if 6.0 <= z < 6.7:
        return WOOD[0]
    if face == "+x":
        if abs(y) < 1.8 and 3.6 <= z < 7.8:
            if abs(y) < 0.7 and 4.4 <= z < 6.2:
                return IRON[0]
            return BRASS[2]
    if face == "-x" and 3.3 < abs(y) < 5.2 and 4.8 <= z < 8.0:
        return IRON[2]
    return None


def furnace_paint(lit):
    brick = bricks(STONE, 3.0, 5.0)

    def paint(face, x, y, z, shade):
        if face == "+x":
            top = 3.0 + math.sqrt(max(0.0, 12.25 - y * y))
            if abs(y) < 3.5 and 1.5 <= z < top:
                if not lit:
                    return IRON[0] if z > 2.2 else "#4e2f36"
                if z < 3.2:
                    return FIRE[3]
                return FIRE[2] if z < 5.0 else FIRE[1]
            if abs(y) < 4.6 and 1.0 <= z < top + 1.1:
                return STONE[0] if shade == 0 else STONE[1]
        return brick(face, x, y, z, shade)
    return paint


def chimney_paint(face, x, y, z, shade):
    if face == "top":
        if abs(y) < 1.4 and -4.2 < x < -1.8:
            return IRON[0]
        return None
    if z % 3.0 < 0.8:
        return STONE[max(0, shade - 1)] if shade else "#33304f"
    return None


def anvil_top(face, x, y, z, shade):
    if face == "top" and abs(x) < 0.6 and -5.5 < y < -4.0:
        return IRON[0]
    return None


def board_paint(face, x, y, z, shade):
    if face == "top":
        n = (x + 5.0) / 2.5
        if n % 1.0 < 0.22:
            return PLANK[1]
        return PLANK[3] if int(n) % 2 else PLANK[2]
    return None


def bed_paint(face, x, y, z, shade):
    if face == "top":
        if x < -3.2:
            return CREAM[2]
        if x < -2.2:
            return RED[3]
        return None
    if x < -3.2:
        return CREAM[1] if shade == 1 else CREAM[0]
    return None


def deck_paint(face, x, y, z, shade):
    """The deck of a bridge: planks across the run (the x axis), a dark gap between neighbours."""
    if face == "top":
        n = (x + 8.0) / 2.0
        if n % 1.0 < 0.2:
            return PLANK[0]
        return [PLANK[2], PLANK[3], PLANK[2], PLANK[1]][int(n) % 4] if abs(y) < 5.5 else None
    if z < 1.0:
        return WOOD[shade] if shade < 2 else None
    return None


def stile_paint(face, x, y, z, shade):
    if face in ("+y", "-y") and abs(x) < 1.3 and 5.0 <= z < 6.2:
        return BRASS[1 if shade else 0]
    return None


# ----------------------------------------------------------------------------------------------------------------
# object definitions
# ----------------------------------------------------------------------------------------------------------------

def chest_boxes():
    return [
        box(-4.5, 4.5, -6.5, 6.5, 0, 6, WOOD, chest_paint),
        box(-4.5, 4.5, -6.5, 6.5, 6, 8.4, PLANK, chest_paint),
        box(-3.0, 3.0, -6.5, 6.5, 8.4, 9.8, PLANK, chest_paint),
    ]


def furnace_boxes(lit):
    return [
        box(-6, 6, -6, 6, 0, 12, STONE, furnace_paint(lit)),
        box(-5, -1, -2.5, 2.5, 12, 18, STONE, chimney_paint),
    ]


def anvil_boxes():
    dark = ["#1d1830", "#4a4868", "#7d7a8c", "#aeadb8"]
    return [
        box(-4, 4, -5, 4, 0, 2.5, dark),
        box(-2.5, 2.5, -3, 3, 2.5, 6.5, IRON),
        box(-4, 4, -7.5, 3, 6.5, 10.5, STONE, anvil_top),
        box(-3.6, 3.6, 3, 4.2, 7.0, 10.5, STONE),
        box(-3.0, 3.0, 4.2, 5.4, 7.8, 10.5, STONE),
        box(-2.2, 2.2, 5.4, 6.6, 8.6, 10.5, STONE),
        box(-1.4, 1.4, 6.6, 7.6, 9.3, 10.5, STONE),
        box(-0.7, 0.7, 7.6, 8, 9.9, 10.5, STONE),
    ]


def workbench_boxes():
    legs = [box(sx * 3 - (0 if sx > 0 else 2), sx * 3 + (2 if sx > 0 else 0), sy * 5 - (0 if sy > 0 else 2),
                sy * 5 + (2 if sy > 0 else 0), 0, 8, WOOD) for sx in (1, -1) for sy in (1, -1)]
    # legs sit under the corners of the top: x 3..5, y 5..7
    legs = [box(x0, x0 + 2, y0, y0 + 2, 0, 8, WOOD) for x0 in (3, -5) for y0 in (5, -7)]
    return legs + [
        box(3.5, 4.5, -5, 5, 3, 4.2, WOOD),
        box(-4.5, -3.5, -5, 5, 3, 4.2, WOOD),
        box(-5, 5, -7, 7, 8, 11, PLANK, board_paint),
        box(-3.5, 0, -6, -1.5, 11, 12, SAND),
        box(1.5, 2.5, 1.5, 6.5, 11, 12, WOOD),
        box(0.5, 3.5, 5.5, 7, 11, 13, IRON),
    ]


def bed_boxes():
    return [
        box(-8, 8, -6.5, 6.5, 0, 3, WOOD),
        box(-8, -7, -6.5, 6.5, 3, 10, WOOD),
        box(7, 8, -6.5, 6.5, 3, 6, WOOD),
        box(-7, 7, -6, 6, 3, 5.5, RED, bed_paint),
        box(-6.8, -3.6, -5, 5, 5.5, 7, CREAM),
    ]


def sleeping_bag_paint(face, x, y, z, shade):
    """A straw roll: a darker seam down the middle of the top and a pale pillow end."""
    if face == "top":
        if x < -4.2:
            return CREAM[2]
        if abs(y) < 0.7:
            return SAND[1]
    return None


def sleeping_bag_boxes():
    """A low straw mat with a rolled edge on each long side and a small pillow at the head."""
    return [
        box(-7, 7, -4.5, 4.5, 0, 1.5, SAND, sleeping_bag_paint),
        box(-7, 7, -5, -4, 0, 2.6, SAND),
        box(-7, 7, 4, 5, 0, 2.6, SAND),
        box(-6.8, -4, -3, 3, 1.5, 3, CREAM),
    ]


def bridge_boxes():
    return [
        box(-8, 8, -8, 8, 0, 3, PLANK, deck_paint),
        box(-8, 8, -8, -6.5, 0, 4.2, WOOD),
        box(-8, 8, 6.5, 8, 0, 4.2, WOOD),
    ]


def fence_post_box():
    return box(-PH, PH, -PH, PH, 0, POST_HEIGHT, PLANK)


def rail_boxes(direction):
    """The rails from the post to the middle of one tile edge, stopped by a ghost post so they never overdraw it."""
    boxes = []
    for z0, z1 in (RAIL_LOW, RAIL_HIGH):
        if direction == "lr":
            boxes.append(box(0, 8, -RH, RH, z0, z1, PLANK))
        elif direction == "ul":
            boxes.append(box(-8, 0, -RH, RH, z0, z1, PLANK))
        elif direction == "ll":
            boxes.append(box(-RH, RH, 0, 8, z0, z1, PLANK))
        else:
            boxes.append(box(-RH, RH, -8, 0, z0, z1, PLANK))
    boxes.append(box(-PH, PH, -PH, PH, 0, POST_HEIGHT, PLANK, ghost=True))
    return boxes


def gate_posts():
    """Two posts taller than a fence post, with a lighter cap, at the middle of the tile's two ends so the fence rails of the neighbours reach them."""
    boxes = []
    for x0, x1 in ((6, 8), (-8, -6)):
        boxes.append(box(x0, x1, -PH, PH, 0, GATE_POST_HEIGHT, PLANK))
        boxes.append(box(x0 - 0.4, x1 + 0.4, -PH - 0.4, PH + 0.4, GATE_POST_HEIGHT, GATE_POST_HEIGHT + 1, SAND))
    return boxes


def panel_box(along, a0, a1, c, t, z0, z1, ramp):
    """A box that runs from a0 to a1 along the given tile axis, centred on c across it, with half thickness t."""
    if along == "x":
        return box(a0, a1, c - t, c + t, z0, z1, ramp)
    return box(c - t, c + t, a0, a1, z0, z1, ramp)


def gate_panel(along, a0, a1, c, rising):
    """A framed gate leaf: a light wood frame around a darker board infill, with a dark diagonal brace.

    rising is +1 when the brace climbs towards a1 and -1 when it climbs towards a0.
    """
    boxes = [
        panel_box(along, a0, a1, c, 0.75, 8.5, 10.5, SAND),
        panel_box(along, a0, a1, c, 0.75, 2.5, 4.5, SAND),
        panel_box(along, a0, a0 + 1, c, 0.75, 2.5, 10.5, SAND),
        panel_box(along, a1 - 1, a1, c, 0.75, 2.5, 10.5, SAND),
        panel_box(along, a0 + 1, a1 - 1, c, 0.4, 4.5, 8.5, PLANK),
    ]
    inner0, inner1 = a0 + 1, a1 - 1
    steps = 4
    width = (inner1 - inner0) / steps
    for i in range(steps):
        n = i if rising > 0 else steps - 1 - i
        z0 = 4.5 + n * 0.8
        boxes.append(panel_box(along, inner0 + i * width, inner0 + (i + 1) * width, c, 0.75, z0, z0 + 1.4, WOOD))
    return boxes


def gate_closed_boxes():
    """Two leaves that meet in the middle, their braces rising towards the centre."""
    return gate_posts() + gate_panel("x", -6, -0.5, 0, 1) + gate_panel("x", 0.5, 6, 0, -1)


def gate_open_boxes():
    """Both leaves swung a quarter turn about their hinge posts to the same side, the passage between the posts clear."""
    return gate_posts() + gate_panel("y", 1, 7.5, 7, 1) + gate_panel("y", 1, 7.5, -7, 1)


# ----------------------------------------------------------------------------------------------------------------
# fire pieces, unrotated
# ----------------------------------------------------------------------------------------------------------------

FLAME_HEIGHT = [8, 10, 7, 10]
FLAME_SWAY = [0, 1, 0, -1]


def px(cv, x, y, colour):
    cv.set(x, y, hexc(colour))


def rect(cv, x0, y0, x1, y1, colour):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px(cv, x, y, colour)


def flame(cv, cx, base, frame, scale=1.0):
    """Paints a flame whose lower end is at row base: an orange body, a yellow heart and a pale core, swaying per frame."""
    top = round(FLAME_HEIGHT[frame] * scale)
    sway = FLAME_SWAY[frame]
    for k in range(top):
        t = k / max(1, top - 1)
        half = round((1.0 + 3.2 * scale) * (t ** 0.55)) if t > 0.0 else 0
        row = base - top + k + 1
        shift = round(sway * (1 - t))
        rect(cv, cx + shift - half, row, cx + shift + half, row, FIRE[2])
        if t > 0.35:
            w2 = max(0, round(half * 0.62))
            rect(cv, cx + shift - w2, row, cx + shift + w2, row, FIRE[3])
        if t > 0.7:
            w3 = max(0, round(half * 0.3))
            rect(cv, cx + shift - w3, row, cx + shift + w3, row, CREAM[2])
        if t > 0.8:
            px(cv, cx + shift - half, row, FIRE[1])
            px(cv, cx + shift + half, row, FIRE[1])
    px(cv, cx + [3, -3, 2, -2][frame], base - top - [1, 2, 0, 1][frame], FIRE[3])


def campfire(frame):
    h = 32
    cv = Canvas(WIDTH, h)
    cy = h - DIAMOND_H // 2
    n = 11
    for i in range(n):
        a = i / n * 2 * math.pi
        sx = round(16 + 11 * math.cos(a))
        sy = round(cy + 5.2 * math.sin(a))
        light = math.cos(a + 0.8) > 0.0
        rect(cv, sx - 1, sy - 1, sx + 1, sy + 1, STONE[2] if light else STONE[1])
        px(cv, sx - 1, sy - 1, STONE[3] if light else STONE[2])
        px(cv, sx + 1, sy + 1, STONE[0])
    for y in range(cy - 3, cy + 4):
        for x in range(8, 25):
            if ((x - 16) / 8.5) ** 2 + ((y - cy) / 4.0) ** 2 <= 1:
                px(cv, x, y, "#4e2f36")
    for (x0, y0, x1, y1) in ((9, cy - 2, 22, cy + 3), (10, cy + 3, 23, cy - 2)):
        steps = 13
        for i in range(steps + 1):
            x = x0 + (x1 - x0) * i // steps
            y = y0 + (y1 - y0) * i // steps
            rect(cv, x, y - 1, x, y, WOOD[2])
            px(cv, x, y + 1, WOOD[0])
    rect(cv, 22, cy - 1, 23, cy + 3, WOOD[3])
    flame(cv, 16, cy + 1, frame)
    cv.outline()
    return cv.image()


def standing_torch(frame):
    h = 40
    cv = Canvas(WIDTH, h)
    cy = h - DIAMOND_H // 2
    rect(cv, 12, cy, 20, cy + 2, STONE[1])
    rect(cv, 13, cy - 1, 19, cy - 1, STONE[2])
    rect(cv, 12, cy, 13, cy + 1, STONE[2])
    rect(cv, 19, cy + 1, 20, cy + 2, STONE[0])
    rect(cv, 15, cy - 17, 17, cy - 1, WOOD[1])
    rect(cv, 15, cy - 17, 15, cy - 1, WOOD[2])
    rect(cv, 17, cy - 17, 17, cy - 1, WOOD[0])
    rect(cv, 13, cy - 20, 19, cy - 18, IRON[1])
    rect(cv, 13, cy - 20, 19, cy - 20, IRON[2])
    rect(cv, 18, cy - 19, 19, cy - 18, IRON[0])
    flame(cv, 16, cy - 20, frame, 0.8)
    cv.outline()
    return cv.image()


# ----------------------------------------------------------------------------------------------------------------
# the table of sprites
# ----------------------------------------------------------------------------------------------------------------

FOUR_WAY = {
    "chest": chest_boxes,
    "furnace_off": lambda: furnace_boxes(False),
    "furnace_on": lambda: furnace_boxes(True),
    "anvil": anvil_boxes,
    "workbench": workbench_boxes,
    "bed": bed_boxes,
    "sleeping_bag": sleeping_bag_boxes,
}
TWO_WAY = {
    "bridge": bridge_boxes,
    "fence_gate_closed": gate_closed_boxes,
    "fence_gate_open": gate_open_boxes,
}
FENCE_H = 34


def draw_all():
    """Returns every (name, image) pair, in file order."""
    sprites = []
    for name, boxes in FOUR_WAY.items():
        for k, img in enumerate(family(boxes(), range(4), 38 if name.startswith("furnace") else None)):
            sprites.append((f"{name}_{k}", img))
    for name, boxes in TWO_WAY.items():
        h = FENCE_H if name.startswith("fence") else None
        for k, img in enumerate(family(boxes(), range(2), h)):
            sprites.append((f"{name}_{k}", img))
    sprites.append(("fence_post", family([fence_post_box()], [0], FENCE_H)[0]))
    for edge in ("ul", "ur", "ll", "lr"):
        sprites.append((f"fence_rail_{edge}", family(rail_boxes(edge), [0], FENCE_H)[0]))
    for i in range(4):
        sprites.append((f"campfire_{i}", campfire(i)))
    for i in range(4):
        sprites.append((f"standing_torch_{i}", standing_torch(i)))
    sprites.extend(oven_and_path_sprites())
    return sprites


def write_all(out):
    """Writes every sprite into the folder, removes the stale png files and returns how many files it wrote."""
    out.mkdir(parents=True, exist_ok=True)
    sprites = draw_all()
    names = {f"{name}.png" for name, _ in sprites}
    for old in sorted(out.glob("*.png")):
        if old.name not in names:
            old.unlink()
            print(f"removed {old.name}")
    for name, img in sprites:
        img.save(out / f"{name}.png")
    return len(sprites)


# ----------------------------------------------------------------------------------------------------------------
# previews
# ----------------------------------------------------------------------------------------------------------------

def preview(path, scale=3):
    """Renders every sprite on grass green and on water blue, enlarged with no smoothing and labelled."""
    sprites = draw_all()
    cols = 8
    cell_w, cell_h = WIDTH + 6, 44
    rows = (len(sprites) + cols - 1) // cols
    panels = []
    for bg in ((0x59, 0xa8, 0x4a, 255), (0x38, 0x9c, 0xc4, 255)):
        sheet = Image.new("RGBA", (cols * cell_w, rows * cell_h), bg)
        for i, (_, img) in enumerate(sprites):
            x, y = (i % cols) * cell_w + 3, (i // cols) * cell_h + cell_h - 2 - img.height
            sheet.alpha_composite(img, (x, y))
        panels.append(sheet.resize((sheet.width * scale, sheet.height * scale), Image.NEAREST))
    out = Image.new("RGBA", (panels[0].width, panels[0].height * 2), (0, 0, 0, 255))
    for k, panel in enumerate(panels):
        out.alpha_composite(panel, (0, k * panel.height))
    draw = ImageDraw.Draw(out)
    for i, (name, _) in enumerate(sprites):
        draw.text(((i % cols) * cell_w * scale + 4, (i // cols) * cell_h * scale + 1), name, fill=(255, 255, 255, 255))
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    out.save(path)


class Scene:
    """A small iso map of grass tiles and water, where sprites are placed at their anchor."""

    def __init__(self, n, m, water):
        self.n, self.m, self.water = n, m, water
        self.grass = Image.open(ASSETS / "tiles" / "grass-1.png").convert("RGBA")
        self.bed = Image.open(ASSETS / "tiles" / "bed-1.png").convert("RGBA")
        self.sheet = Image.open(ASSETS / "water" / "sheet.png").convert("RGBA")
        self.sprites = dict(draw_all())
        self.items = {}
        self.ox = m * 16 + 8
        self.img = Image.new("RGBA", ((n + m) * 16 + 32, (n + m) * 8 + 70), (0x1d, 0x18, 0x30, 255))

    def at(self, i, j):
        """The screen position of the top left of the tile (i, j): i grows to the lower right, j to the lower left."""
        return self.ox + (i - j) * 16, 44 + (i + j) * 8

    def put(self, i, j, name):
        self.items.setdefault((i, j), []).append(name)

    def render(self, path, scale=3):
        for i in range(self.n):
            for j in range(self.m):
                x, y = self.at(i, j)
                if (i, j) in self.water:
                    self.img.alpha_composite(self.bed, (x, y + 10))
                    self.img.alpha_composite(self.sheet, (x, y + 5))
                else:
                    self.img.alpha_composite(self.grass, (x, y))
        for s in range(self.n + self.m - 1):
            for i in range(self.n):
                j = s - i
                for name in self.items.get((i, j), []):
                    sprite = self.sprites[name]
                    x, y = self.at(i, j)
                    # the bottom centre of the canvas goes on the bottom vertex of the tile top diamond
                    self.img.alpha_composite(sprite, (x, y + DIAMOND_H - sprite.height))
        path = Path(path)
        path.parent.mkdir(parents=True, exist_ok=True)
        self.img.resize((self.img.width * scale, self.img.height * scale), Image.NEAREST).save(path)


def compose_fence(scene, cells, gates):
    """Places fence parts the way the game composes them: back rails, the post, then front rails.

    cells is the set of (i, j) fence tiles, gates maps a tile to its gate sprite name. A rail goes to every neighbour that is a fence or a gate.
    """
    linked = set(cells) | set(gates)
    for (i, j) in cells:
        for edge, (di, dj) in (("ul", (-1, 0)), ("ur", (0, -1))):
            if (i + di, j + dj) in linked:
                scene.put(i, j, f"fence_rail_{edge}")
        scene.put(i, j, "fence_post")
        for edge, (di, dj) in (("ll", (0, 1)), ("lr", (1, 0))):
            if (i + di, j + dj) in linked:
                scene.put(i, j, f"fence_rail_{edge}")
    for pos, name in gates.items():
        scene.put(pos[0], pos[1], name)


def placed(path):
    """A fence ring with a corner, a T and two gates, and a bridge of 3 tiles from the shore."""
    water = {(i, j) for i in range(8, 11) for j in range(4, 8)}
    scene = Scene(13, 10, water)
    ring = {(i, 1) for i in range(1, 6)} | {(i, 5) for i in range(1, 6)} | {(1, j) for j in range(1, 6)} | {(5, j) for j in range(1, 6)}
    gates = {(3, 5): "fence_gate_closed_0", (5, 3): "fence_gate_open_1"}
    ring -= set(gates)
    # the T: a spur that leaves the middle of the top row
    cells = ring | {(3, 0)}
    compose_fence(scene, cells, gates)
    # bridge: three tiles from the shore across the water, the run along the lower right axis
    for i in range(8, 11):
        scene.put(i, 6, "bridge_0")
    for pos, name in (((7, 7), "campfire_1"), ((6, 8), "standing_torch_2")):
        scene.put(pos[0], pos[1], name)
    scene.render(path)


def orient(path):
    """Every 4 way piece in its 4 orientations, one piece per row."""
    names = list(FOUR_WAY)
    scene = Scene(2 * len(names), 8, set())
    for i, name in enumerate(names):
        for k in range(4):
            scene.put(2 * i, 2 * k, f"{name}_{k}")
    scene.render(path)


# ----------------------------------------------------------------------------------------------------------------
# stone oven and cobblestone path
# ----------------------------------------------------------------------------------------------------------------

OVEN_H = 34
PATH_VARIANTS = 4
# cobble tones, dark mortar first; every colour is in palette.hex
COBBLE = ["#4a4868", "#7d7a8c", "#aeadb8", "#7d7a8c", "#aeadb8", "#dcecf8"]


def oven_paint(lit):
    """Terracotta bricks with an arched mouth in the front face: dark when cold, glowing when lit."""
    brick = bricks(RED, 3.0, 5.0)

    def paint(face, x, y, z, shade):
        if face == "+x":
            top = 3.0 + math.sqrt(max(0.0, 16.0 - y * y))
            if abs(y) < 4.0 and 1.5 <= z < top:
                if not lit:
                    return IRON[0] if z > 2.4 else WOOD[0]
                if z < 3.0:
                    return FIRE[3]
                return FIRE[2] if z < 5.0 else FIRE[1]
            if abs(y) < 5.2 and 1.0 <= z < top + 1.1:
                return STONE[1] if shade else STONE[0]
        return brick(face, x, y, z, shade)
    return paint


def oven_boxes(lit):
    """A domed bread oven: a stone plinth, a brick body, three shrinking tiers for the dome and a back chimney."""
    return [
        box(-7, 7, -7, 7, 0, 1.5, STONE),
        box(-6, 6, -6, 6, 1.5, 8, RED, oven_paint(lit)),
        box(-5, 5, -5, 5, 8, 11, RED, bricks(RED, 3.0, 5.0)),
        box(-3.6, 3.6, -3.6, 3.6, 11, 13.2, RED, bricks(RED, 3.0, 5.0)),
        box(-2, 2, -2, 2, 13.2, 14.6, RED),
        box(-5.5, -3, -1.5, 1.5, 11, 18, STONE, chimney_paint),
    ]


def cobble_path(variant):
    """A flat 32x16 tile top of irregular cobbles with dark joints, the bottom vertex on the bottom centre pixel."""
    cv = Canvas(WIDTH, DIAMOND_H)
    seeds = []
    state = 7919 * (variant + 1)
    for gy in range(-1, 6):
        for gx in range(-1, 8):
            state = (state * 1103515245 + 12345) & 0x7FFFFFFF
            jx, jy = (state >> 8) % 3 - 1, (state >> 14) % 3 - 1
            offset = 2 if gy % 2 else 0
            seeds.append((gx * 5 + offset + jx, gy * 3 + jy + 1, (state >> 4) % len(COBBLE[1:])))
    owner = {}
    for y in range(DIAMOND_H):
        for x in range(WIDTH):
            if abs(x + 0.5 - 16) / 16 + abs(y + 0.5 - 8) / 8 > 1.0:
                continue
            owner[(x, y)] = min(range(len(seeds)), key=lambda i: (x - 3 - seeds[i][0]) ** 2 * 0.5
                                + (y - seeds[i][1]) ** 2 * 1.4)
    for (x, y), i in owner.items():
        edge = any(owner.get((x + dx, y + dy), i) != i for dx, dy in ((1, 0), (0, 1)))
        tone = COBBLE[1 + seeds[i][2]]
        if edge:
            tone = COBBLE[0]
        elif owner.get((x - 1, y), i) != i or owner.get((x, y - 1), i) != i:
            tone = COBBLE[5] if tone == COBBLE[2] else COBBLE[2]
        cv.set(x, y, hexc(tone))
    return cv.image()


def oven_and_path_sprites():
    """The stone oven (off and lit, 4 orientations each, the furnace convention) and the cobblestone path variants."""
    sprites = []
    for name, lit in (("stone_oven_off", False), ("stone_oven_on", True)):
        for k, img in enumerate(family(oven_boxes(lit), range(4), OVEN_H)):
            sprites.append((f"{name}_{k}", img))
    for v in range(PATH_VARIANTS):
        sprites.append((f"cobblestone_path_{v}", cobble_path(v)))
    return sprites


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--out", default=str(DEFAULT_OUT))
    parser.add_argument("--preview")
    parser.add_argument("--placed")
    parser.add_argument("--orient")
    args = parser.parse_args()
    if args.preview or args.placed or args.orient:
        if args.preview:
            preview(args.preview)
        if args.placed:
            placed(args.placed)
        if args.orient:
            orient(args.orient)
        return
    print(f"buildings {write_all(Path(args.out))}")


if __name__ == "__main__":
    main()
