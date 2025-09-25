# Aperture API (Forge 1.20.1)

Aperture API provides an in-game camera editor, pathing system, Bezier tools/gizmo UI, and a minimal command surface for cinematic camera work on Forge 1.20.1 (Java 17). GPL-3.0 licensed.

What’s included (post-cleanup):
- Core runtime and camera path model (Catmull-Rom, Bezier, cosine, step, etc.)
- In-game editor screens and gizmo/overlay rendering
- Path preview and constant-speed playback utilities
- Minimal commands:
  - /aperture (help | version | api)
  - /camera (list | play <name> [speed] [loop] [auto-reset] [target] | stop [target] | reset [target] | interpolation <name> <mode> | export <name> <file>)

Removed:
- Legacy/duplicate/unused commands and CMDCam shims
- Unused assets and stale scaffolding

Build & run
- Requirements: Java 17, Gradle Wrapper
- Build: `./gradlew assemble`
- Run client dev: `./gradlew runClient`
- Run tests: `./gradlew test`
- Lint/format: `./gradlew spotlessApply check`

Project layout
- net.tysontheember.apertureapi
  - api/ (core API surface)
  - client/ (editor UI, gizmos, overlays, preview)
  - command/ (registration)
  - commands/ (implementations)
  - path/ (path math & evaluation)
  - util/ (helpers)
  - common/ (shared network/state)

Contributing
- Use Google Java Format via Spotless.
- Checkstyle and ErrorProne are enabled; keep the code warning-free where practical.
- Keep the public API minimal and documented; prefer package-private for internals.
