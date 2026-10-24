# Xaero's Shaped Waypoints

Fabric client mod for Minecraft 26.3 that adds square, rectangle, circle, polygon, star and custom-shaped waypoints for Xaero's Minimap / World Map.

> Version strings in `gradle.properties` are unverified for 26.3. Check https://fabricmc.net/develop/ before building.

## Commands

| Command | Result |
|---|---|
| `/shape square <name> <size> [color]` | Square centered on you |
| `/shape rect <name> <width> <length>` | Rectangle |
| `/shape circle <name> <radius> [points]` | Circle (16 points by default) |
| `/shape polygon <name> <sides> <radius>` | Regular polygon |
| `/shape star <name> <points> <outer> <inner>` | Star |
| `/shape custom start <name>` | Start recording a custom shape |
| `/shape custom point` / `undo` / `finish` / `finishopen` / `cancel` | Edit or end the recording |
| `/shape export <name>` | Add every vertex as a Xaero waypoint |
| `/shape toggle <name>` / `remove <name>` / `list` | Manage shapes |

## Build

```
./gradlew build
```

The jar is written to `build/libs/`. Install it with Fabric API and Xaero's Minimap.

## Notes

Xaero has no public API. `XaeroBridge` uses reflection. If `/shape export` fails after a Xaero update, check the log and update the class and method names in `compat/XaeroBridge.java`.
