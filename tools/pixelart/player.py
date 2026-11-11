"""Draws the minimal player sheet: 8 columns by 3 rows of 16x24 cells.

Columns follow SpriteLayout.playerColumn: UP_LEFT, UP, UP_RIGHT, RIGHT, DOWN_RIGHT, DOWN, DOWN_LEFT, LEFT.
Rows are walk a, idle and walk b. The figure is drawn from rectangles, so no raw input is needed.

Usage: python -I tools/pixelart/player.py <out.png> [preview.png]
"""

from __future__ import annotations

import sys
from pathlib import Path

from PIL import Image

CELL_W, CELL_H = 16, 24
COLUMNS = 8
ROWS = 3

Rgba = tuple[int, int, int, int]
Grid = list[list[Rgba | None]]

OUTLINE: Rgba = (0x1d, 0x18, 0x30, 255)
FILL: Rgba = (0xfc, 0xf4, 0xe4, 255)
SHADE: Rgba = (0xbc, 0xd0, 0xe6, 255)
EYE = OUTLINE
PREVIEW_BACKGROUND: Rgba = (0x45, 0x9b, 0x4c, 255)
PREVIEW_SCALE = 8

IDLE, WALK_A, WALK_B = 0, 1, 2
ROW_POSES = {0: WALK_A, 1: IDLE, 2: WALK_B}

# Round head and narrow body as {row: (first column, last column)}. The feet end on row 21 and row 22 takes the
# outline.
HEAD = {5: (6, 9), 6: (4, 11), 7: (3, 12), 8: (3, 12), 9: (3, 12), 10: (3, 12), 11: (3, 12), 12: (4, 11),
        13: (5, 10)}
BODY = {14: (5, 10), 15: (5, 10), 16: (5, 10), 17: (5, 10), 18: (6, 9)}

LEG_TOP, LEG_BOTTOM = 18, 22
EYE_TOP = 9

# Eye columns per facing, empty for the back views. Each eye is two pixels tall.
EYES = {0: [], 1: [], 2: [], 3: [11], 4: [8, 11], 5: [6, 9], 6: [4, 7], 7: [4]}

# Columns drawn in profile, which show the legs as a stride instead of a pair.
SIDE_COLUMNS = (3, 7)

# Leg rows of a profile stride as (first column, last column) per row, back leg then front leg.
STRIDE_LEGS = [[(6, 7), (5, 6), (5, 6), (4, 5)], [(8, 9), (9, 10), (9, 10), (10, 11)]]
STANDING_LEG = [(7, 8)] * 4

# Rows each leg is shortened by, left then right, and the arm swing per pose.
LEG_LIFT = {IDLE: (0, 0), WALK_A: (1, 0), WALK_B: (0, 1)}
ARM_SWING = {IDLE: (0, 0), WALK_A: (-1, 1), WALK_B: (1, -1)}


def put(grid: Grid, x: int, y: int, colour: Rgba) -> None:
    """Sets one pixel, ignoring coordinates outside the cell."""
    if 0 <= x < CELL_W and 0 <= y < CELL_H:
        grid[y][x] = colour


def fill_rows(grid: Grid, spec: dict[int, tuple[int, int]], dy: int) -> None:
    """Fills the horizontal spans of a body part, shifted down by dy rows."""
    for y, (first, last) in spec.items():
        for x in range(first, last + 1):
            put(grid, x, y + dy, FILL)


def draw_legs(grid: Grid, pose: int, side: bool) -> None:
    """Draws the legs: a diagonal stride in profile, otherwise two legs where the lifted one is shorter."""
    if side:
        legs = [STANDING_LEG] if pose == IDLE else STRIDE_LEGS
        for leg in legs:
            for y, (first, last) in zip(range(LEG_TOP, LEG_BOTTOM), leg):
                for x in range(first, last + 1):
                    put(grid, x, y, FILL)
        return
    lift = LEG_LIFT[pose]
    for (first, last), up in (((5, 6), lift[0]), ((9, 10), lift[1])):
        for y in range(LEG_TOP, LEG_BOTTOM - up):
            for x in range(first, last + 1):
                put(grid, x, y, FILL)


def draw_arms(grid: Grid, pose: int, side: bool, dy: int) -> None:
    """Draws the two arms swinging against each other. A profile has no visible arms."""
    if side:
        return
    swing = ARM_SWING[pose]
    for x, offset in ((4, swing[0]), (11, swing[1])):
        for y in (15 + offset + dy, 16 + offset + dy):
            put(grid, x, y, FILL)


def apply_shade(grid: Grid) -> None:
    """Lights the figure from the upper left: the rightmost pixel of every row takes the shade."""
    for y in range(CELL_H):
        xs = [x for x in range(CELL_W) if grid[y][x] is not None]
        if xs:
            grid[y][xs[-1]] = SHADE


def add_outline(grid: Grid) -> Grid:
    """Returns the grid with a one pixel outline on every empty pixel that touches the figure."""
    out = [row[:] for row in grid]
    for y in range(CELL_H):
        for x in range(CELL_W):
            if grid[y][x] is None and any(
                    0 <= x + dx < CELL_W and 0 <= y + dy < CELL_H and grid[y + dy][x + dx] is not None
                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                out[y][x] = OUTLINE
    return out


def draw_frame(column: int, row: int) -> Grid:
    """Draws one cell. The walking rows bob the figure up by one pixel."""
    pose = ROW_POSES[row]
    dy = 0 if pose == IDLE else -1
    grid: Grid = [[None] * CELL_W for _ in range(CELL_H)]
    fill_rows(grid, HEAD, dy)
    fill_rows(grid, BODY, dy)
    draw_legs(grid, pose, column in SIDE_COLUMNS)
    apply_shade(grid)
    for eye_x in EYES[column]:
        put(grid, eye_x, EYE_TOP + dy, EYE)
        put(grid, eye_x, EYE_TOP + 1 + dy, EYE)
    return add_outline(grid)


def build_player_sheet() -> Image.Image:
    """Builds the full 8 column by 3 row sheet."""
    sheet = Image.new("RGBA", (CELL_W * COLUMNS, CELL_H * ROWS), (0, 0, 0, 0))
    for row in range(ROWS):
        for column in range(COLUMNS):
            grid = draw_frame(column, row)
            for y in range(CELL_H):
                for x in range(CELL_W):
                    if grid[y][x] is not None:
                        sheet.putpixel((column * CELL_W + x, row * CELL_H + y), grid[y][x])
    return sheet


def build_preview(sheet: Image.Image) -> Image.Image:
    """Returns the sheet over a grass colour, enlarged without smoothing."""
    background = Image.new("RGBA", sheet.size, PREVIEW_BACKGROUND)
    background.alpha_composite(sheet)
    return background.resize((sheet.width * PREVIEW_SCALE, sheet.height * PREVIEW_SCALE), Image.Resampling.NEAREST)


def main(argv: list[str]) -> int:
    if not argv:
        print("usage: player.py <out.png> [preview.png]", file=sys.stderr)
        return 1
    sheet = build_player_sheet()
    sheet.save(Path(argv[0]))
    if len(argv) > 1:
        build_preview(sheet).save(Path(argv[1]))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
