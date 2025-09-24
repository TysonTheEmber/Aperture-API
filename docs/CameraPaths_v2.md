# Camera Paths JSON v2

This schema extends v1 and adds richer interpolation, easing, roll and target support.

Schema

```
{
  "version": 2,
  "name": "...",
  "defaults": {
    "interp": "catmull:centripetal",
    "ease": "cubicInOut",
    "speedMode": "duration", // duration | speed
    "loop": false
  },
  "speed": { "durationSec": 12.0, "blocksPerSec": null },
  "segments": [
    {
      "p": [x,y,z],
      "rot": { "yaw": ..., "pitch": ..., "roll": ... } | { "q": [x,y,z,w] },
      "fov": 90.0,
      "ease": "cubicInOut",
      "durationSec": 1.5,
      "lookAt": { "type":"entity|block|self|none", "id":"...", "pos":[x,y,z], "followSpeed":1.0 },
      "tcb": { "t":0.0, "c":0.0, "b":0.0 },
      "bezier": { "in":[dx,dy,dz], "out":[dx,dy,dz] }
    }
  ]
}
```

Notes
- Interpolations: linear, cosine, hermite, bezier, catmull:{uniform|chordal|centripetal}
- Orientation: rot can be Euler (yaw/pitch/roll) or quaternion.
- Speed: duration-based or blocks/sec; arc-length tables recommended per segment.
- Backward-compat loader: detect v1 and map to v2 with sensible defaults.
