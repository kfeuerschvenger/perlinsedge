# Pixel art post-process

Turns the raw images generated for the game into the sprites it loads. The raw folder is local and not committed, the output lands in `src/main/resources/assets` and is committed.

## Usage

```bash
python -I tools/pixelart/build.py --raw <dir>
```

Requires Python 3.12 and Pillow. `--out <dir>` changes the output folder, by default the game's `assets` folder. The tool is idempotent: it rebuilds every output it finds an input for, so run it again whenever raw files are added or replaced.

```mermaid
flowchart LR
  R[raw folder] --> Q[quantise to palette.hex]
  Q --> T[tiles: clip to block]
  Q --> S[resources and items: crop and pad]
  Q --> E[slime strip and player sheet]
  Q --> G[hearts]
  T --> O[src/main/resources/assets]
  S --> O
  E --> O
  G --> O
```

## Raw layout

| Raw input | Output |
|---|---|
| `tiles/tile-<name>-<n>.png` | `assets/tiles/<name>-<n>.png` |
| `resources/<biome>/<kind>-<n>.png` | `assets/resources/<biome>/<kind>-<n>.png` |
| `items/<item>.png` | `assets/items/<item>.png` |
| `gui/heart-full.png`, `heart-half.png` | `assets/gui/`, plus a derived `heart-empty.png` when no raw one exists |
| `entities/slime-<n>.png` | `assets/entities/slime-<n>.png`, a 6 frame strip |
| `player/explorer.zip` | `assets/entities/player.png`, a 256x168 sheet |

Item files are named after the lower case `ItemType` constant. The game picks up any variant number it finds, so adding `tile-grass-3.png` or `tree-4.png` needs no code change.

## What the tool does

- Every image is snapped to the 40 colours in `palette.hex` by nearest RGB distance, with no dithering, and its alpha becomes binary.
- Terrain is rebuilt as a 32x16 top diamond plus side faces, 5 pixels thick for land and 3 for water. The faces are re-cut from the bottom of the raw faces and everything outside the block silhouette is dropped, so grass tufts no longer poke out and neighbours tile exactly on the 2:1 grid.
- Resources are cropped to their content and centred on the bottom row of the canvas for their kind, for example 32x48 for a tree and 24x20 for a rock. Content larger than the canvas keeps its own size.
- Items and hearts are centred on 16x16.
- The slime strip has six frames of 20x16 made by squashing and stretching the single raw frame: rest, crouch, stretch, apex, fall, landing.
- The player sheet has 8 columns by 3 rows of 32x56 cells, which hold the 68x68 frames cropped to their content at native pixel size. The rows are walk a, idle and walk b. Walk a is the second frame of a four frame walk and walk b the fourth, and idle is the rotation. A direction without walk frames uses its rotation for all three rows, and a missing east or west is mirrored from the other side.

## Player columns

The columns follow the screen compass, in the order up, up right, right, down right, down, down left, left, up left. Only four poses exist, so a diagonal reuses the horizontal pose of its side.

```mermaid
flowchart LR
  C0[0 up] --> north
  C1[1 up right] --> east
  C2[2 right] --> east
  C3[3 down right] --> east
  C4[4 down] --> south
  C5[5 down left] --> west
  C6[6 left] --> west
  C7[7 up left] --> west
```

The poses share one crop, one ground line and one centre, so a walk cycle does not jitter.
