"""Draw every item icon as a 16x16 sprite in the palette, with the dark outline, with no raw input.

Each icon is built from a few shapes (shaded blobs, rectangles, lines and single pixels) lit from the upper left, painted in colours of palette.hex and wrapped in the palette outline. One drawing function per ItemType: the file name is the lower case constant name, which is the name the game looks the icon up by. The script writes one file per drawing into assets/items and removes files of that folder that no drawing produces, so the folder never keeps an icon nothing references.

usage: python -I tools/pixelart/items.py [--out <assets/items folder>] [--preview preview.png]
"""
import argparse
import sys
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
# Isolated mode leaves the script folder off the import path, and trees.py lives next to this file.
sys.path.insert(0, str(HERE))

from trees import OUTLINE, Canvas, blob, hexc  # noqa: E402

SIZE = 16
DEFAULT_OUT = HERE.parent.parent / "src" / "main" / "resources" / "assets" / "items"

# colour ramps, dark to light
WOOD = ["#4e2f36", "#7a4a3a", "#a0643f", "#c4925a"]
PLANK = ["#7a4a3a", "#a0643f", "#c4925a", "#dba867"]
STONE = ["#4a4868", "#7d7a8c", "#aeadb8", "#dcecf8"]
COAL = ["#1d1830", "#4a4868", "#4a4868", "#7d7a8c"]
IRON = ["#4a4868", "#7d7a8c", "#aeadb8", "#dcecf8"]
SLIME = ["#337a47", "#459b4c", "#76b04c", "#b3d05e"]
CRYSTAL = ["#2a4f94", "#3fa0cc", "#8ad6e6", "#dcecf8"]
GEM = ["#6c3fc4", "#a96af0", "#a96af0", "#fcf4e4"]
LEATHER = ["#7a4a3a", "#a0643f", "#c4925a", "#dba867"]
BONE = ["#aeadb8", "#dcecf8", "#fcf4e4", "#fcf4e4"]
FISH = ["#38607f", "#5f7fa8", "#9fb4e0", "#dcecf8"]
FISH_COOKED = ["#7a4a3a", "#a0643f", "#dba867", "#f4d890"]
MEAT_RAW = ["#9c4247", "#d4705c", "#d4705c", "#f4bf9a"]
MEAT_COOKED = ["#4e2f36", "#7a4a3a", "#a0643f", "#c4925a"]
HAY = ["#a2825a", "#c4925a", "#dba867", "#f4d890"]
FLINT = ["#1d1830", "#4a4868", "#7d7a8c", "#aeadb8"]


def px(cv, x, y, colour):
    """Paints one pixel with a hex colour."""
    cv.set(x, y, hexc(colour))


def rect(cv, x0, y0, x1, y1, colour):
    """Fills a rectangle, both corners included."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px(cv, x, y, colour)


def line(cv, x0, y0, x1, y1, colour):
    """Draws a one pixel line with Bresenham's algorithm."""
    dx, dy = abs(x1 - x0), -abs(y1 - y0)
    sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
    err = dx + dy
    while True:
        px(cv, x0, y0, colour)
        if x0 == x1 and y0 == y1:
            return
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x0 += sx
        if e2 <= dx:
            err += dx
            y0 += sy


def pixels(cv, points, colour):
    """Paints a list of (x, y) pixels."""
    for x, y in points:
        px(cv, x, y, colour)


def finish(cv):
    """Wraps the drawing in the outline and returns the image."""
    cv.outline()
    return cv.image()


# ----------------------------------------------------------------------------------------------------------------
# materials
# ----------------------------------------------------------------------------------------------------------------

def wood_log():
    cv = Canvas(SIZE, SIZE)
    # the bark runs from the left to an end face on the right, shaded top to bottom
    for i, y in enumerate(range(4, 12)):
        colour = WOOD[3] if i == 0 else WOOD[2] if i < 3 else WOOD[1] if i < 6 else WOOD[0]
        rect(cv, 1, y, 10, y, colour)
    for x in (3, 6, 8):
        px(cv, x, 7, WOOD[0])
    px(cv, 4, 9, WOOD[3])
    # the end face, with two growth rings
    blob(cv, 11.5, 8, 3.5, 4, PLANK)
    blob(cv, 11.5, 8, 2.2, 2.6, ["#a2825a", "#c4925a", "#dba867", "#f4d890"])
    px(cv, 12, 8, "#a0643f")
    px(cv, 11, 7, "#f4d890")
    return finish(cv)


def wood_plank():
    cv = Canvas(SIZE, SIZE)
    # two planks side by side, grain along their length, a nail in each end
    for x0, y0 in ((1, 3), (5, 6)):
        rect(cv, x0, y0, x0 + 9, y0 + 3, PLANK[2])
        rect(cv, x0, y0, x0 + 9, y0, PLANK[3])
        rect(cv, x0, y0 + 3, x0 + 9, y0 + 3, PLANK[1])
        line(cv, x0 + 2, y0 + 1, x0 + 6, y0 + 1, PLANK[1])
        px(cv, x0 + 1, y0 + 2, PLANK[0])
        px(cv, x0 + 8, y0 + 2, PLANK[0])
    return finish(cv)


def stone():
    cv = Canvas(SIZE, SIZE)
    blob(cv, 7.5, 9, 6.5, 5, STONE, flat_bottom=0.6)
    blob(cv, 6, 6.5, 3.5, 2.5, STONE)
    pixels(cv, [(10, 9), (11, 10), (9, 11)], STONE[0])
    return finish(cv)


def stone_brick():
    cv = Canvas(SIZE, SIZE)
    mortar, face, light, dark = STONE[0], STONE[2], STONE[3], STONE[1]
    rect(cv, 1, 3, 14, 12, mortar)
    for y0, x_cuts in ((4, (1, 8)), (8, (1, 5, 12))):
        cuts = list(x_cuts) + [15]
        for a, b in zip(cuts, cuts[1:]):
            rect(cv, a, y0, b - 2, y0 + 3, face)
            rect(cv, a, y0, b - 2, y0, light)
            rect(cv, a, y0 + 3, b - 2, y0 + 3, dark)
    return finish(cv)


def iron_ore():
    cv = Canvas(SIZE, SIZE)
    blob(cv, 7.5, 9, 6.5, 5, STONE, flat_bottom=0.6)
    blob(cv, 6, 6.5, 3.5, 2.5, STONE)
    # rust specks
    for x, y in ((5, 7), (9, 8), (7, 11), (11, 10), (4, 10)):
        px(cv, x, y, "#d4705c")
        px(cv, x + 1, y, "#9c4247")
    return finish(cv)


def iron_ingot():
    cv = Canvas(SIZE, SIZE)
    # a bar seen from the front corner: a lit top, a front face and a darker side
    for i in range(4):
        rect(cv, 4 + i * 0, 4 + i, 13 - i * 0, 4 + i, IRON[3] if i == 0 else IRON[2])
    rect(cv, 1, 8, 11, 12, IRON[1])
    rect(cv, 1, 8, 11, 8, IRON[3])
    rect(cv, 12, 5, 14, 11, IRON[0])
    pixels(cv, [(1, 8), (2, 7), (3, 6), (4, 5), (5, 4)], IRON[3])
    line(cv, 3, 10, 9, 10, IRON[2])
    return finish(cv)


def crystals():
    cv = Canvas(SIZE, SIZE)

    def shard(cx, top, bottom, half):
        for y in range(top, bottom + 1):
            width = half if y > top + 2 else max(0, (y - top))
            for x in range(cx - width, cx + width + 1):
                tone = CRYSTAL[3] if x <= cx - width + (0 if width == 0 else 0) and width > 0 else CRYSTAL[2]
                if x > cx:
                    tone = CRYSTAL[1]
                if x == cx + width and width > 0:
                    tone = CRYSTAL[0]
                px(cv, x, y, tone)
    shard(4, 6, 13, 2)
    shard(11, 7, 13, 2)
    shard(7, 2, 13, 3)
    px(cv, 6, 5, CRYSTAL[3])
    px(cv, 6, 6, CRYSTAL[3])
    return finish(cv)


def refined_crystals():
    cv = Canvas(SIZE, SIZE)
    # a cut gem: a wide crown over a tapering pavilion
    for y in range(3, 7):
        half = 2 + (y - 3) * 1
        rect(cv, 8 - half, y, 7 + half, y, GEM[2])
    for y in range(7, 13):
        half = max(0, 6 - (y - 7) * 1)
        rect(cv, 8 - half, y, 7 + half, y, GEM[1] if y < 10 else GEM[0])
    rect(cv, 6, 3, 9, 3, GEM[3])
    pixels(cv, [(5, 4), (4, 5), (3, 6)], GEM[3])
    line(cv, 8, 7, 8, 12, GEM[0])
    px(cv, 13, 2, GEM[3])
    px(cv, 12, 1, GEM[3])
    px(cv, 14, 2, GEM[3])
    px(cv, 13, 3, GEM[3])
    return finish(cv)


def coal():
    cv = Canvas(SIZE, SIZE)
    blob(cv, 7.5, 9, 6.5, 5, COAL, flat_bottom=0.5)
    blob(cv, 6, 6.5, 3.5, 2.5, COAL)
    pixels(cv, [(5, 6), (6, 6), (5, 7)], STONE[2])
    pixels(cv, [(10, 9), (10, 10)], STONE[1])
    return finish(cv)


def fiber():
    cv = Canvas(SIZE, SIZE)
    # a bunch of long strands fanning out from a red tie
    strands = [(6, 3), (7, 5), (7, 7), (8, 9), (9, 11), (9, 12)]
    for i, (bottom, top) in enumerate(strands):
        colour = HAY[3] if i % 3 == 0 else HAY[2] if i % 3 == 1 else HAY[1]
        line(cv, bottom, 13, top, 2 if i % 2 == 0 else 3, colour)
    pixels(cv, [(5, 2), (11, 2), (12, 3)], HAY[2])
    rect(cv, 6, 9, 10, 10, "#d4705c")
    rect(cv, 6, 10, 10, 10, "#9c4247")
    return finish(cv)


def slime_ball():
    cv = Canvas(SIZE, SIZE)
    blob(cv, 8, 8.5, 6, 5.5, SLIME, flat_bottom=0.3)
    pixels(cv, [(5, 5), (6, 4), (5, 6)], "#6ee07c")
    px(cv, 6, 5, "#fcf4e4")
    return finish(cv)


def stick():
    cv = Canvas(SIZE, SIZE)
    line(cv, 2, 13, 13, 2, WOOD[2])
    line(cv, 2, 14, 13, 3, WOOD[1])
    line(cv, 1, 13, 12, 2, WOOD[3])
    pixels(cv, [(7, 8), (8, 6)], WOOD[0])
    # a short twig
    line(cv, 8, 7, 11, 8, WOOD[2])
    return finish(cv)


def leather():
    cv = Canvas(SIZE, SIZE)
    blob(cv, 8, 8, 6.5, 5.5, LEATHER, flat_bottom=0.0)
    # a hide pinched at the corners, with a seam of stitches
    pixels(cv, [(1, 3), (14, 3), (1, 13), (14, 13)], LEATHER[1])
    for x in range(4, 12, 2):
        px(cv, x, 8, LEATHER[3])
    pixels(cv, [(4, 5), (5, 4)], LEATHER[3])
    return finish(cv)


def bone():
    cv = Canvas(SIZE, SIZE)
    line(cv, 4, 11, 11, 4, BONE[2])
    line(cv, 5, 11, 12, 4, BONE[1])
    line(cv, 3, 11, 10, 4, BONE[2])
    # a knob made of two bumps on each end
    for x, y in ((3, 10), (2, 11), (4, 12), (3, 12), (3, 11)):
        px(cv, x, y, BONE[2])
    for x, y in ((12, 5), (13, 4), (11, 3), (12, 3), (12, 4)):
        px(cv, x, y, BONE[2])
    pixels(cv, [(4, 12), (3, 12), (12, 3), (13, 4)], BONE[1])
    pixels(cv, [(5, 11), (12, 4)], BONE[0])
    return finish(cv)


def flint():
    cv = Canvas(SIZE, SIZE)
    # an angular flake with a sharp tip, lit from the upper left
    for y in range(2, 13):
        half = 1 + (y - 2) * 4 // 10
        rect(cv, 8 - half, y, 8 + half, y, FLINT[2])
    for y in range(2, 13):
        half = 1 + (y - 2) * 4 // 10
        px(cv, 8 + half, y, FLINT[1])
        px(cv, 8 - half, y, FLINT[3])
    rect(cv, 4, 12, 12, 13, FLINT[1])
    line(cv, 7, 4, 7, 9, FLINT[3])
    line(cv, 9, 7, 11, 11, FLINT[1])
    return finish(cv)


# ----------------------------------------------------------------------------------------------------------------
# food
# ----------------------------------------------------------------------------------------------------------------

def fish(ramp, belly, eye, cooked):
    cv = Canvas(SIZE, SIZE)
    blob(cv, 7, 8, 5.5, 3.6, ramp)
    # the tail fans out to the right
    for i, y in enumerate(range(4, 13)):
        reach = 13 - abs(8 - y) // 2
        rect(cv, 11, y, reach, y, ramp[1])
    px(cv, 11, 8, ramp[1])
    rect(cv, 4, 9, 9, 10, belly)
    px(cv, 4, 7, eye)
    if cooked:
        for x in (6, 8, 10):
            line(cv, x, 5, x + 1, 11, MEAT_COOKED[1])
    else:
        pixels(cv, [(6, 6), (7, 6)], ramp[3])
    return finish(cv)


def raw_fish():
    return fish(FISH, "#dcecf8", "#1d1830", cooked=False)


def cooked_fish():
    return fish(FISH_COOKED, "#f4d890", "#1d1830", cooked=True)


def raw_meat():
    cv = Canvas(SIZE, SIZE)
    # a steak with a rim of fat and a little marbling
    blob(cv, 8, 8.5, 6.5, 5, MEAT_RAW, flat_bottom=0.2)
    for x, y in ((4, 5), (5, 4), (6, 4), (7, 4), (8, 4), (9, 4), (10, 5), (11, 6)):
        px(cv, x, y, "#fcf4e4")
    pixels(cv, [(6, 8), (7, 9), (8, 8), (9, 9)], "#f4bf9a")
    return finish(cv)


def cooked_meat():
    cv = Canvas(SIZE, SIZE)
    # a roast on a bone, browned with a lighter top
    blob(cv, 6.5, 6.5, 5, 4.2, MEAT_COOKED)
    line(cv, 9, 9, 13, 13, "#fcf4e4")
    line(cv, 8, 9, 12, 13, "#aeadb8")
    pixels(cv, [(13, 12), (12, 13), (14, 13), (13, 14)], "#fcf4e4")
    pixels(cv, [(4, 4), (5, 3), (6, 3)], "#c4925a")
    pixels(cv, [(8, 8), (9, 7)], MEAT_COOKED[0])
    return finish(cv)


# ----------------------------------------------------------------------------------------------------------------
# tools and equipment
# ----------------------------------------------------------------------------------------------------------------

def handle(cv, x0, y0, x1, y1):
    """Draws a two tone wooden handle."""
    line(cv, x0, y0, x1, y1, WOOD[2])
    line(cv, x0 + 1, y0, x1 + 1, y1, WOOD[1])


def torch():
    cv = Canvas(SIZE, SIZE)
    rect(cv, 7, 7, 8, 14, WOOD[2])
    rect(cv, 8, 7, 8, 14, WOOD[1])
    rect(cv, 6, 6, 9, 7, WOOD[0])
    # the flame, hottest in the middle
    flame = [(7, 5), (8, 5), (6, 4), (7, 4), (8, 4), (9, 4), (7, 3), (8, 3), (7, 2), (8, 1)]
    pixels(cv, flame, "#ffa02a")
    pixels(cv, [(7, 4), (8, 4), (7, 3), (8, 3), (7, 5), (8, 5)], "#ffa02a")
    pixels(cv, [(7, 4), (8, 4), (8, 5), (7, 5)], "#f4d890")
    px(cv, 5, 5, "#d4705c")
    px(cv, 10, 5, "#d4705c")
    return finish(cv)


def pickaxe():
    cv = Canvas(SIZE, SIZE)
    handle(cv, 2, 14, 11, 5)
    # a curved head across the top of the handle
    head = [(3, 5), (4, 4), (5, 3), (6, 3), (7, 3), (8, 3), (9, 4), (10, 4), (11, 5), (12, 6), (13, 7), (13, 8), (14, 9)]
    for x, y in head:
        px(cv, x, y, STONE[2])
        px(cv, x, y + 1, STONE[1])
    pixels(cv, [(4, 4), (5, 3), (6, 3), (7, 3), (8, 3)], STONE[3])
    pixels(cv, [(3, 5), (3, 6), (14, 9), (14, 10)], STONE[1])
    return finish(cv)


def axe():
    cv = Canvas(SIZE, SIZE)
    # a wooden haft rising from the lower left, the head hung on its upper end and flaring to one side
    handle(cv, 3, 14, 11, 4)
    rows = {2: (8, 11), 3: (6, 11), 4: (4, 11), 5: (3, 10), 6: (3, 9), 7: (4, 8), 8: (6, 7)}
    for y, (x0, x1) in rows.items():
        rect(cv, x0, y, x1, y, STONE[2])
        # the steel behind the cutting edge is darker where it is thick, near the haft
        rect(cv, max(x0, x1 - 2), y, x1, y, STONE[1])
        # the bright cutting edge on the open side
        px(cv, x0, y, STONE[3])
    rect(cv, 8, 2, 10, 3, STONE[3])
    px(cv, 11, 3, WOOD[3])
    px(cv, 11, 4, WOOD[2])
    return finish(cv)


def sword():
    cv = Canvas(SIZE, SIZE)
    # a blade from the lower left to the upper right tip, a guard across it and a pommel
    line(cv, 5, 10, 13, 2, STONE[3])
    line(cv, 4, 10, 12, 2, STONE[2])
    line(cv, 5, 11, 13, 3, STONE[1])
    px(cv, 14, 1, "#fcf4e4")
    # the guard
    line(cv, 3, 8, 7, 12, "#ffa02a")
    line(cv, 4, 8, 8, 12, "#a0643f")
    # the grip and pommel
    line(cv, 4, 11, 2, 13, WOOD[2])
    line(cv, 5, 11, 3, 13, WOOD[1])
    pixels(cv, [(1, 14), (2, 14), (1, 13)], "#ffa02a")
    return finish(cv)


def potion_health():
    cv = Canvas(SIZE, SIZE)
    # a round flask of red liquid under a cork
    blob(cv, 8, 10, 5.5, 4.5, MEAT_RAW)
    rect(cv, 6, 4, 9, 6, "#bcd0e6")
    rect(cv, 6, 4, 6, 6, "#dcecf8")
    rect(cv, 6, 6, 9, 6, "#9c4247")
    rect(cv, 6, 1, 9, 3, "#a0643f")
    rect(cv, 6, 1, 9, 1, "#c4925a")
    pixels(cv, [(5, 8), (5, 9), (6, 7)], "#f4bf9a")
    pixels(cv, [(6, 9)], "#fcf4e4")
    rect(cv, 6, 13, 11, 13, "#9c4247")
    return finish(cv)


# One drawing per ItemType, keyed by the file name the game looks the icon up by.
ICONS = {
    "wood_log": wood_log,
    "wood_plank": wood_plank,
    "stone": stone,
    "stone_brick": stone_brick,
    "iron_ore": iron_ore,
    "iron_ingot": iron_ingot,
    "crystals": crystals,
    "refined_crystals": refined_crystals,
    "coal": coal,
    "fiber": fiber,
    "slime_ball": slime_ball,
    "stick": stick,
    "leather": leather,
    "bone": bone,
    "flint": flint,
    "raw_fish": raw_fish,
    "cooked_fish": cooked_fish,
    "raw_meat": raw_meat,
    "cooked_meat": cooked_meat,
    "torch": torch,
    "pickaxe": pickaxe,
    "axe": axe,
    "sword": sword,
    "potion_health": potion_health,
}


def draw_all():
    """Draws every icon, returning (name, image) pairs in a fixed order."""
    return [(name, draw()) for name, draw in ICONS.items()]


def write_all(out):
    """Writes every icon and removes the icons of the folder that no drawing produces."""
    out.mkdir(parents=True, exist_ok=True)
    wanted = set()
    for name, img in draw_all():
        img.save(out / f"{name}.png")
        wanted.add(f"{name}.png")
    for stale in sorted(out.glob("*.png")):
        if stale.name not in wanted:
            stale.unlink()
            print(f"removed {stale.name}")
    return len(wanted)


def preview(path, scale=4):
    """Renders every icon on a dark panel, a row of eight, enlarged with no smoothing."""
    icons = draw_all()
    cols = 8
    rows = (len(icons) + cols - 1) // cols
    cell = SIZE + 4
    sheet = Image.new("RGBA", (cols * cell + 4, rows * cell + 4), (0x1d, 0x18, 0x30, 255))
    for i, (_, img) in enumerate(icons):
        x, y = 4 + (i % cols) * cell, 4 + (i // cols) * cell
        sheet.alpha_composite(Image.new("RGBA", (SIZE, SIZE), (0x4a, 0x48, 0x68, 255)), (x, y))
        sheet.alpha_composite(img, (x, y))
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    sheet.resize((sheet.width * scale, sheet.height * scale), Image.NEAREST).save(path)


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--out", default=str(DEFAULT_OUT))
    parser.add_argument("--preview")
    args = parser.parse_args()
    if args.preview:
        preview(args.preview)
        return
    print(f"items {write_all(Path(args.out))}")


if __name__ == "__main__":
    main()
