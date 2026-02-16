# DeadEnderChest

Paper plugin for hardcore deathban servers. When a player dies, their ender chest contents are saved. A living player can hold the dead player's head and run `/dec` to spawn a chest with those items.

Each dead player's ender chest can only be looted **once** — after that, the head is useless for looting (but you keep it as a trophy).

Designed to pair with [HeadDrop](https://modrinth.com/plugin/head-drop) (100% player head drop on death).

## Commands

| Command | Alias | Description |
|---------|-------|-------------|
| `/deadenderchest` | `/dec` | Loot a dead player's ender chest while holding their head |

**Permission:** `deadenderchest.use` (default: all players)

## How It Works

1. Player dies — plugin saves their 27-slot ender chest to `plugins/DeadEnderChest/enderchests/<uuid>.yml`
2. Another player picks up the dropped head, holds it, runs `/dec`
3. A chest spawns nearby with the dead player's ender chest contents
4. The UUID is marked as looted in `looted.txt` — can't be looted again

## Build

Requires Java 21 and Gradle.

```bash
gradle jar
# Output: build/libs/DeadEnderChest.jar
```

Copy the jar to your server's `plugins/` directory and restart.

## Requirements

- Paper 1.21+
- Java 21+
- [HeadDrop](https://modrinth.com/plugin/head-drop) (for player head drops on death)
