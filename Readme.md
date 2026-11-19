# Perlin's Edge

## Game Overview

Perlin's Edge is an isometric open-world survival and building game developed using JavaFX. Players explore procedurally generated landscapes, gather resources, craft tools and structures, and face various creatures in a dynamic world. Inspired by classic titles in the genre, Perlin's Edge delivers a relaxing (and occasionally hilarious) gaming experience where exploration and resource management are crucial for survival.

## Features

- **Procedural World Generation**: Every playthrough offers a unique landscape thanks to Perlin noise.
- **Isometric Perspective**: Classic visual style for exploration and building.
- **Crafting System**: Combine recipes to make tools, weapons, and more, at the inventory, the workbench or the anvil.
- **Processing Stations**: Smelt iron in a furnace, cook meat in a stone oven or on a campfire, and watch each one light up while it works.
- **Base Building**: Construct fortresses, huts, or your dream treehouse.
- **Inventory Management**: Organized chaos: your call.
- **Basic Combat**: Swing or smash your way through enemies.
- **Exploration**: Roam biomes, find hidden secrets, or just get lost (we won’t judge).
- **Pathfinding**: Smart navigation for friends: and foes.
- **Day and Night**: The sun crosses the sky, shadows follow its height, and the world turns warm at dawn and dusk and a dark moonlit blue at night.
- **Torchlight**: Hold a torch and it lights the night around you with a warm wash of crackling light that fades smoothly outward, drifts between orange and yellow, and breathes at its edge. The light appears the moment the torch is the item in your hand and disappears when you pick anything else or die.

## Debug Modes & Hotkeys

Geek out with our built-in debug modes. Toggle these cheats: I mean, features: with the F-keys:

- **F1**: Debug Mode: Displays FPS, coordinates, and other nerdy stats in the top-right corner. The sky line shows the day number, the phase and the clock, and the day number turns when the dawn begins at 03:00, not at midnight.
- **F2**: Grayscale Mode: See the Perlin noise tile values in all their gray glory.
- **F3**: Tile Values: Overlay each tile with its raw noise number (great for bragging rights).
- **F4**: Regenerate Map: Smash this to get a fresh world with a random seed. It is the same session, so the time of day, the day number and the fast time of F5 carry over to the new map: only a new game starts a new day.
- **F5**: Fast Time: Makes a whole day pass in about 40 seconds, so you can watch the sun cross the sky and the shadows turn.

## How to Play

### Main Menu & Map Selection

When you launch the game, you'll be greeted with the Main Menu. Click **New Game** to open the Map Selection screen, where you can choose from six distinct worlds:

- **Dense Forest**
- **Swamp**
- **Desert**
- **Tundra**
- **Mediterranean**
- **Islands**

Each map not only looks different but also features unique resources and special characteristics to keep you on your toes (and occasionally slipping on swamp mud).


### Exploration Mode

- **Move**: Right-click where you want to go. The walk goes around enemies and a tile an enemy stands on is refused.
- **Attack**: Left-click an enemy to hit it with the item in your hand. Next to it you swing at once, subject to the swing cooldown, and otherwise you walk after it, following it as it moves, and swing when you are next to it. A right-click, pausing or the enemy dying ends the chase.
- **Gather and use**: Left-click trees, rocks, chests, and other goodies. Out of reach, you walk to the nearest free tile next to it and it happens once when you arrive, and when you are already next to it, it happens at once. A new click that is refused, pausing, a mode change, or the target disappearing cancels the walk and you stop on the tile you were stepping to, while a move order replaces it with its own walk. A target that was hit a moment before is hit again as soon as it can be, whether you walked up to it during that moment or were already next to it when you clicked. Iron ore needs a pickaxe in your hand and crystals an iron pickaxe, see Tools below. Both appear only on rocky ground and are scarce, crystals rarer than iron, so a map with no rocky ground has neither.
- **Cursor feedback**: The crosshair on the tile under the mouse breathes slowly and is dark over light ground like sand and snow, light elsewhere. When you click, the tile flashes green if the order was accepted, and red if it was refused: water, a tile you cannot reach, nothing to do there, an enemy on the tile of a move order, or a missing tool.
- **Hotbar**: Nine slots of their own are always on the bar at the bottom of the screen, in addition to the 18 slots of the inventory, so you carry 27 stacks. Keys `1` to `9` or the mouse wheel pick the slot you hold in hand, and the choice stays put while the items in the slots change. `Q` throws one item from the slot in hand and `Ctrl+Q` throws the whole stack. An empty slot means a bare hand. In building mode the hotbar stays on screen and the number keys and the wheel still pick the slot in hand, while `Q` and `Ctrl+Q` do nothing.
- **Torch**: Select a torch in the hotbar and it lights a circle of about four and a half tiles around you at night, at dusk and at dawn. It does nothing at midday, when there is no darkness to push back, and the flame crackles a little. Outside its light the night stays dark, but not black.
- **Where items go**: Everything you pick up, craft or take from a chest goes first into the stacks of the same item you already carry, hotbar before inventory, then into the first free hotbar slot, then into the first free inventory slot.
- **Use**: `E` uses the item in hand, and pressing the number of the slot you already hold does nothing. A health potion heals 50, cooked meat 20 and raw meat 5, one item at a time and never above your maximum health. At full health nothing is used up and a hint says so. Using is off while an inventory is open, while paused and while dead.
- **Zoom**: `Ctrl` with the mouse wheel zooms the camera in every mode, while the plain wheel moves along the hotbar.- **Inventory**: `I` toggles your bag of holding, an 18 slot grid six wide and three tall, with the crafting list and an item information panel beside it. The hotbar stays on screen and works like the grid: drag stacks between the hotbar and the inventory or a chest, drop onto a slot to merge or swap, press `Q` or `Ctrl+Q` over a slot to throw from it, right-click while dragging to throw one item, and release outside the panels to throw the whole stack. Everything you carry drops where you die, hotbar included.
- **Quick move**: `Ctrl`+click a stack to send all of it to the other side. With no chest open, a hotbar stack goes to the inventory and an inventory stack to the hotbar. With a chest open, a stack of yours goes to the chest and a chest stack comes to you in the order above. It merges into matching stacks first and then takes the first free slot, and what does not fit stays where it was.
- **Crafting**: The recipes you can craft are listed first, the others follow in greyscale but can still be selected to read what they need. The ingredients and the description share a panel of fixed size, the ingredients first, and scroll with the wheel or the scrollbar when they do not fit. Each ingredient shows how many you hold against how many it needs, such as `Stone 2/3`, in green when you have enough and red when you do not, counting the hotbar and the inventory together. The Craft button works only for a recipe you can craft.
- **Combat numbers**: A number rises from every hit and heal and fades out: white for damage to resources, yellow for damage to enemies, red for damage you take and green with a plus for health you restore.
- **Slimes**: A slime hops after you and stops next to you. It squashes for about 0.6 seconds, which tells you the strike is coming, strikes, and waits about 1.5 seconds before it squashes again. Step away and it hops after you again. Both times are set in `AppConfig`.
- **Pause**: `ESC` for a breather or snack. While you hold the hammer with a building piece selected, `ESC` drops the piece first and a second press pauses.
- **Center Camera**: `SPACE` to bring the camera home.
- **Minimap**: Click to warp the view.

### Tools

What you hold in hand decides how you gather and fight. Only the slot you hold counts, a tool that sits elsewhere in the bag does nothing. A tool that does not suit the target works like a bare hand.

| Target | Health | Bare hand | Stone tool | Iron tool |
|---|---|---|---|---|
| Tree | 30 | 8 hits, one every 0.6 s | Stone axe: 3 hits, one every 0.3 s | Iron axe: 2 hits, one every 0.3 s |
| Stone | 30 | 8 hits, one every 0.6 s | Stone pickaxe: 3 hits, one every 0.3 s | Iron pickaxe: 2 hits, one every 0.3 s |
| Iron ore | 36 | Cannot be mined | Stone pickaxe: 3 hits, one every 0.3 s | Iron pickaxe: 2 hits, one every 0.3 s |
| Crystals | 25 | Cannot be mined | Cannot be mined | Iron pickaxe: 2 hits, one every 0.3 s |

Iron tools deal double the damage of their stone counterparts, to resources and to enemies. Against enemies a bare hand, a pickaxe and the hammer deal 10 damage per hit, the axe 12 and the sword 20, and the iron pickaxe, axe and sword deal 20, 24 and 40. The hammer suits no resource and works like a bare hand on them, see Building Mode below. Without a pickaxe in hand, iron ore and crystals show a hint and flash red. A stone pickaxe on crystals shows a hint that it is too hard and that you need an iron pickaxe.

### Building Mode

Build mode is the hammer in your hand. Craft a hammer from 2 sticks and 2 stones in the inventory, no station needed, and put it in a hotbar slot. Selecting that slot, with a number key, the wheel or a click, enters build mode, and selecting any other slot leaves it. You cannot build while you are dead or the game is paused, and opening the inventory or a chest closes the palette until you close it again.

- **Palette**: A row of every building piece appears above the hotbar, each with its icon, its name and its cost. It includes the stone oven, the cobblestone path and the straw sleeping bag. Each ingredient reads `held/needed`, in green when you have enough and red when you do not, the way the crafting list does. Click a piece to choose it, which does not place anything in the world. The piece stays chosen until you choose another, `ESC` drops it with the hammer still in hand, and the hammer remembers your last piece when you pick it up again.
- **Place**: Left-click a tile to place the chosen piece, which uses up the cost. The tile flashes green when the piece was placed and red when it was refused. The ghost of the piece is green where it fits, red where the tile refuses it and amber when you lack the resources.
- **Rotate**: `R` turns the ghost a quarter turn clockwise before you place it, and the palette card shows the way it faces. Chests, furnaces, stone ovens, anvils, workbenches and sleeping bags face four ways, bridges and fence gates run along one of two diagonals, and the campfire, the standing torch, the fence and the cobblestone path do not turn. Each piece type remembers its rotation while the hammer stays in hand.
- **Where pieces go**: Most pieces stand on land only. A bridge goes on water that shares an edge with land or another bridge, so it grows out from the shore one tile at a time, and a diagonal neighbour does not count. A bridge is the floor of the tile and makes the water walkable. The cobblestone path is a floor piece too, for walkable land, and costs 1 stone brick. The standing torch can stand on any floor, and the chests, furnaces, stone ovens, campfires, anvils, workbenches, sleeping bags and fences can stand on a path but not on a bridge. The campfire is land only.
- **Fences and gates**: A fence is a post with a half rail toward each neighbouring fence, and toward a gate that runs along the line between the two, so fences join up by themselves. A gate costs 2 planks and 4 sticks. Without the hammer, click a gate to walk up to it and swing it open or shut. A closed gate and every fence block you and the enemies, and the enemies never open a gate. An open gate can be walked through.
- **Demolish**: Right-click a building to take it down. The piece on top of a tile comes down first, and the bridge under it only once nothing stands on it. You get back, for each ingredient of the piece, its quantity times the share of health the building has left, rounded down, and what does not fit in your bags is dropped at your feet. A building at full health returns everything. A chest drops everything it held as piles on its tile.
- **Sleeping bag**: The straw sleeping bag costs 6 fiber. Click a bag to make it your spawn point. When you die you wake up on it and the bag is used up. Only one bag is active at a time, and demolishing it clears the spawn point.
- **Health**: Every building has health that depends on its kind. While you hold the hammer over a damaged building, a small health bar floats above it. Nothing damages buildings yet.
- **Guides**: A faint grid of outlines fades out around the tile under the mouse, and a see-through ghost of the chosen piece stands on that tile:
    - **Green**: Good to go.
    - **Amber**: You need more resources.
    - **Red**: Nope, not here.
    - No ghost means no piece is chosen.

### Inventory

- The grid holds the 18 inventory slots. The hotbar is not part of it: it stays on the bar at the bottom of the screen and takes part in everything below, and the slot you hold in hand keeps its white frame.
- Drag a stack onto another slot, in the grid, on the hotbar or in a chest, to move, stack or swap it. `Ctrl`+click sends the whole stack to the other side.
- Drag a stack out of the panels (release it outside the grid and the hotbar) to throw the whole stack into the world. Releasing it over a panel but over no slot puts it back.
- Right-click while dragging to throw one item and keep dragging the rest.
- `Q` over a slot throws one item, and `Ctrl+Q` throws the whole stack. With the mouse over no slot, they act on the selected slot. With no inventory open they act on the slot you hold in hand.
- Thrown items land one or two tiles ahead of you, in the direction you are facing, never inside a wall, water or a resource. Identical items you threw within a tile of each other merge into one stack.
- You cannot pick up what you threw for two seconds, and items left on the ground vanish after five minutes of game time. Time spent paused does not count.

### Crafting

1. Press `I` for inventory, or click a workbench or an anvil to open the inventory with that station's recipes and its name.
2. Head to the Crafting list on the right.
3. Click an item, then **Craft**... voilà! The ingredients are used up and the result goes into your bag. The recipes you can craft come first and the Craft button is off for the others. If the result does not fit, nothing is consumed and a hint says why.

Every recipe belongs to a station:

- **Inventory**: the basics, which are the stick, the wood plank, flint, the torch and the hammer.
- **Workbench**: the stone axe, pickaxe and sword, the stone brick and the health potion.
- **Anvil**: the iron axe, pickaxe and sword.

### Processing Stations

The furnace, the stone oven and the campfire work on what you load into them. With an accepted item in hand, click the station to load one unit per click, up to a cap for each role, input or fuel. Click it with anything else and a hint shows the status, for example `Input 5/20 (Iron ore 3, Wood log 2), Fuel 3/20 (Coal 3)`.

| Station | Does | Time per unit | Fuel | Caps |
|---|---|---|---|---|
| Furnace | Iron ore to iron ingot | 10 s | One coal or charcoal per ingot | 20 input, 20 fuel |
| Furnace | Wood log to charcoal | 8 s | None | 20 input, 20 fuel |
| Stone oven | Raw meat to cooked meat | 6 s | One log per two pieces | 20 input, 20 fuel |
| Campfire | Raw meat to cooked meat, one piece at a time | 15 s | A log burns for 60 s whether it cooks or not | 10 input, 10 fuel |

- The product drops on the tile in front of the station, following its rotation, as a pile that never despawns and can be picked up at once. Demolishing a station drops what it holds.
- The furnace and the stone oven show their lit sprite and a small glow only while working. The campfire shows flames and a larger light only while it has fuel.

## System Requirements

- **JDK** 17 or higher (I used Java 24!).
- **OS**: Windows, macOS, or Linux (JavaFX compatible).
- **RAM**: 4 GB min, 8 GB recommended.
- **Graphics**: JavaFX/OpenGL compatible.

## Development Setup

```bash
git clone https://github.com/kfeuerschvenger/perlinsedge.git
cd perlinsedge
```

1. Import the Maven project in your IDE.
2. Set up JDK 17+ (JavaFX is in `pom.xml`).
3. Run `GameApplication.java` from `src/main/java/com/feuerschvenger/perlinsedge/app/GameApplication.java`.

## Project Structure

```
perlins-edge/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/feuerschvenger/perlinsedge/
│   │   │       ├── app/                     # Main application classes
│   │   │       │   └── managers/            # Game state and logic managers
│   │   │       ├── config/                  # Configuration files, loot tables, and recipes definitions
│   │   │       ├── domain/                  # Business logic and game entities
│   │   │       │   ├── crafting/            # Crafting recipes and ingredients
│   │   │       │   ├── entities/            # Player, enemies, items, buildings, containers
│   │   │       │   ├── events/              # Event handling and game events
│   │   │       │   ├── pathfinding/         # Pathfinding algorithms and helpers
│   │   │       │   ├── strategies/          # Game strategies such as combat and movement for enemies
│   │   │       │   ├── utils/               # General utilities
│   │   │       │   └── world/               # World generation and modeling
│   │   │       ├── infra/                   # Infrastructure implementations
│   │   │       │   ├── fx/                  # JavaFX-related classes
│   │   │       │   │   ├── graphics/        # Rendering helpers
│   │   │       │   │   ├── input/           # Input handling
│   │   │       │   │   └── utils/           # Utilities for JavaFX
│   │   │       │   └── noise/               # Noise generation
│   │   │       └── ui/                      # User Interface components
│   │   │           ├── controller/          # Input controllers
│   │   │           └── view/                # UI panels
│   └── resources/                           # Game assets (images, sounds, etc.)
├── docs/                                    # Additional documentation and images
├── pom.xml                                  # Maven configuration file
├── LICENSE                                  # Project license
└── README.md                                # This file
```

## Programming Patterns & Practices

- SOLID Principles
- Interpolation for smooth entity movement on the map
- Isometric Projection for rendering map elements
- Frustum Culling to optimize rendering performance
- Singleton Pattern for sharing configurations across the application
- Strategy Pattern for enemy movement and attack behaviors
- A* (A-star) Pathfinding for 8-directional movement of players and enemies
- Procedural Map Generation using Perlin noise
- Factory Pattern for tile type assignment
- Observer Pattern for game event notifications
- Dependency Injection to decouple modules

## Things to Improve / Add

The bigger ideas, such as digging and farming, a player bow, fishing, shadows from light sources, damage to structures, and magic and sinister biomes, live in [docs/ROADMAP.md](docs/ROADMAP.md).

- **Better sprites!!!**
- **Save/Load** game progress
- Tree replanting mechanic
- Resource regeneration for stones & crystals
- Passive animals (flee when hurt), neutral animals (fight back if attacked)
- More enemy types, day/night-specific spawns
- Portals to other dimensions
- Weather changes (rain, sandstorms, snowstorms)
- Dropped items despawn timer
- Enemies respawn after a while
- Multiplayer support (local or online)
- More crafting recipes

## Big Changes to Consider

- Swap JavaFX for a dedicated game library (LibGDX?)
- Endless world generation: grow the map as players explore

## Contributing

Contributions are welcome! Please:

1. Fork the repo.
2. Create a branch (`git checkout -b feature/xyz`).
3. Commit your changes (`git commit -m "feat: ..."`).
4. Push and open a PR.

Ensure your code adheres to the project's style conventions and that all tests pass.

## License

MIT License. See `LICENSE` for details.

Third-party assets keep their own licenses, listed in `docs/ASSETS-LICENSES.md`. The GUI font is Pixelify Sans, under the SIL Open Font License 1.1.

## Contact

Questions or suggestions? Open an issue!

© 2025 Kevin Feuerschvenger
