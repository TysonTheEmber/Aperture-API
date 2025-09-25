# Changelog

All notable changes will be documented in this file.

## [Unreleased]

### Added
- Spotless (Google Java Format), Checkstyle, and ErrorProne to the build.
- @ParametersAreNonnullByDefault and @MethodsReturnNonnullByDefault package-level annotations across core packages.

### Changed
- Normalized command surface to only two roots: /aperture and /camera.
- General formatting and minor refactors to satisfy tooling.

### Removed
- Unused/legacy command classes: CameraCommandsV2, ApertureCameraCommand, DemoCameraCommand, PathExportCommand, SmoothingCommand.
- Unused test listener and stray assets (logo).
- CMDCam compatibility references not required for current features.

### Notes
- Target platform: Forge 1.20.1, Java 17.
- GPL-3.0 licensing preserved.
