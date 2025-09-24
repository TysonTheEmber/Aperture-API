# Camera Paths (JSON)

This document describes the JSON format and locations for data‑driven camera paths in Aperture-API (Forge 1.20.1). It also includes example paths you can drop into datapacks or your mod resources.

Locations
- Datapack-style: data/apertureapi/camera_paths/*.json
- Mod resources (for shipping examples): src/main/resources/data/apertureapi/camera_paths/*.json
- World override (optional): <world>/apertureapi/camera_paths/*.json

Schema (v1)
- File-level fields
  - schema: integer (currently 1)
  - id: unique string path ID
  - loop: bool
  - speed: float (global playback speed multiplier)
  - fov: object
    - mode: inherit | fixed | keyframed
    - default: float (used for inherit/fixed, and as fallback for keyframed)
  - easing: string (default easing for segments)
  - keyframes: array of keyframe objects
  - path: interpolation strategies
    - position: { interp: linear | bezier | catmullRom, tension?: float }
    - rotation: { interp: lerp | slerp | lookAt }
  - triggers: array of trigger objects (see below)

- Keyframe fields
  - time: float seconds from start
  - pos: { x, y, z }
  - rot: { yaw, pitch, roll? } in degrees (optional if lookAt is used)
  - lookAt: { x, y, z } (optional – overrides rot at that keyframe)
  - fov: float (optional per-keyframe override if fov.mode == keyframed)
  - easeIn / easeOut: per-segment easing overrides (e.g., quadIn, cubicInOut)

- Triggers
  - Common fields: { at: float, type: string, ... }
  - Built-ins (suggested): sound, command, title, subtitle, actionbar, function

Validation expectations
- Time is monotonic non-decreasing.
- Must have at least one keyframe; rotation source is either rot or lookAt.
- Unknown fields are ignored (forwards compatibility).

Examples

Simple linear
```json
{
  "schema": 1,
  "id": "linear_flyby",
  "loop": false,
  "speed": 1.0,
  "keyframes": [
    { "time": 0, "pos": { "x": 0, "y": 80, "z": 0 }, "rot": { "yaw": 90, "pitch": -5 } },
    { "time": 4, "pos": { "x": 20, "y": 82, "z": 0 }, "rot": { "yaw": 90, "pitch": -5 } }
  ],
  "path": { "position": { "interp": "linear" }, "rotation": { "interp": "lerp" } }
}
```

Catmull-Rom with lookAt and triggers
```json
{
  "schema": 1,
  "id": "ignis_intro",
  "loop": false,
  "speed": 1.0,
  "fov": { "mode": "keyframed", "default": 70.0 },
  "keyframes": [
    { "time": 0.0, "pos": { "x": 10.5, "y": 72, "z": -3.25 }, "fov": 72.0 },
    { "time": 1.2, "pos": { "x": 8.1,  "y": 73, "z": -6.0  }, "lookAt": { "x": 0, "y": 72, "z": -8 } },
    { "time": 3.0, "pos": { "x": 6.5,  "y": 74, "z": -12.0 }, "fov": 78.0 }
  ],
  "path": {
    "position": { "interp": "catmullRom", "tension": 0.5 },
    "rotation": { "interp": "slerp" }
  },
  "triggers": [
    { "at": 0.0, "type": "sound", "id": "echoesofbattle:ignis_intro", "volume": 1.0 },
    { "at": 2.5, "type": "actionbar", "text": "Ignis, Inferno Knight" }
  ]
}
```

Orbit center (fixed FOV)
```json
{
  "schema": 1,
  "id": "orbit_center",
  "loop": true,
  "speed": 1.0,
  "fov": { "mode": "fixed", "default": 75.0 },
  "keyframes": [
    { "time": 0.0, "pos": { "x": 8.0,  "y": 72.0, "z": 0.0 },  "lookAt": { "x": 0, "y": 72, "z": 0 } },
    { "time": 2.0, "pos": { "x": 0.0,  "y": 72.0, "z": 8.0 },  "lookAt": { "x": 0, "y": 72, "z": 0 } },
    { "time": 4.0, "pos": { "x": -8.0, "y": 72.0, "z": 0.0 },  "lookAt": { "x": 0, "y": 72, "z": 0 } },
    { "time": 6.0, "pos": { "x": 0.0,  "y": 72.0, "z": -8.0 }, "lookAt": { "x": 0, "y": 72, "z": 0 } },
    { "time": 8.0, "pos": { "x": 8.0,  "y": 72.0, "z": 0.0 },  "lookAt": { "x": 0, "y": 72, "z": 0 } }
  ],
  "path": {
    "position": { "interp": "catmullRom", "tension": 0.0 },
    "rotation": { "interp": "lookAt" }
  }
}
```

Commands (planned)
- /aperture camera list
- /aperture camera play <pathId> [speed] [loop] [hideHud] [lockInput] [targets]
- /aperture camera stop [targets]
- /aperture camera seek <seconds> [targets]
- /aperture camera preview <pathId> (client-only helper)

Tips
- Use /reload (server) or F3+T (client resources) to refresh datapack content.
- Keep times in seconds; sampling is tick-based at runtime.
- Use lookAt for stable aim without gimbal issues; use slerp for authored rotations.

