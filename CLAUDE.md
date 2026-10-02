# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A Kotlin Multiplatform + Compose Multiplatform remake of MORPH3D, a 1993 DOS demo in x86 assembler.
The original sources live in `dos/` (`MORPHSUB.ASM`, `3DTRANS.ASM`, `MORPH3D.ASM`, `SINTAB.ASM`) and are
the reference for behavior and constants. Targets: macOS desktop (JVM) and a universal iOS app (iPhone/iPad).

## Commands

- Run desktop full screen (screen saver mode, exits on any key / click / mouse move): `./gradlew :desktopApp:run`
- Run desktop windowed (exits on Esc): `./gradlew :desktopApp:run --args="--window"`
- Compose hot reload (desktop): `./gradlew :desktopApp:hotRun --mainClass com.github.seregamorph.morph3d_kmp.MainKt`
- Tests (common tests, run on JVM): `./gradlew :shared:jvmTest`
- Single test: `./gradlew :shared:jvmTest --tests 'com.github.seregamorph.morph3d_kmp.Morph3DEngineTest.trailsKeepRecentFramesOnly'`
- macOS DMG: `./gradlew :desktopApp:packageDmg`
- iOS build from CLI:
  `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 17' build`
  (the Xcode "Compile Kotlin Framework" build phase calls `./gradlew :shared:embedAndSignAppleFrameworkForXcode`)
- Regenerate app icons (iOS asset, macOS `icon.png` and `.icns`): `./icon/generate.sh` (uses `icon/IconGenerator.java`, `sips`, `iconutil`)

## Architecture

Modules: `shared` (KMP: `jvm`, `iosArm64`, `iosSimulatorArm64`; produces static framework `Shared`),
`desktopApp` (Kotlin/JVM, Compose Desktop entry point), `iosApp` (Xcode/SwiftUI host). All Kotlin is in
package `com.github.seregamorph.morph3d_kmp`; virtually all logic is in `shared/src/commonMain`.

- **Engine / renderer split.** `Morph3DEngine` is pure simulation (no Compose); `Morph3DScene` is a
  composable that drives it from `withFrameNanos` and draws on a `Canvas`. A `mutableLongStateOf` frame
  counter is read inside the draw lambda so each frame invalidates only the draw phase, not recomposition.
- **Time in "ticks".** One tick = one frame of the original (60 ticks/s). `advance(seconds)` converts
  real time to fractional ticks (capped at `MAX_TICKS_PER_FRAME` after stalls) and steps in ≤1-tick
  increments. Constants in the engine's companion intentionally mirror the assembler names/values
  (`MORPHDELAY`, `SHADOW`, `FADELVL`, ...) — keep that correspondence when porting or tweaking.
- **Angles in 1/10 degrees.** `sinT`/`cosT` in `PointCloud.kt` take 3600 = full period, matching the
  original `SINTAB`; figure generators and rotation code use these units.
- **Coordinate spaces.** Figures are in original 3D units (`PointCloud`, 200 points = `POINTS`). The engine
  projects into the original 320x200 screen space (`Frame.sx/sy`, `NaN` = culled behind near plane); the
  renderer scales uniformly to the window and centers it.
- **Morph cycle.** Two `PointCloud` sources (`src1` current, `src2` target) blended with a sin² weight;
  the palette cross-fades with the same curve. Dots "flow" by interpolating toward the next point and
  rotating arrays (`rotateLeft`) each `MOTION_TICKS`. Next figure/palette is always different from current.
- **Trails.** The engine keeps a deque of recent `Frame`s (pooled to avoid allocation) covering
  `TRAIL_TICKS`; the renderer draws oldest-first with brightness `TRAIL_FADE^age`.
- **Figures/Palettes.** `Figures.generate(index, cloud)` (`COUNT` figures: torus, lissajous variants,
  sphere, cylinder, cone/tree, etc.) and `Palettes.load(index, out)` (9 sets × 4 depth shades × RGB)
  are ports of the assembler tables; the engine blends between the 4 shades smoothly by depth.

Platform hosts are thin:
- `desktopApp/.../main.kt`: fullscreen screen-saver mode (hidden cursor, exit on mouse move detected by
  polling `MouseInfo` after a 1 s settle delay) vs. `--window`/`-w`; sets the Dock icon from `/icon.png`.
- `shared/src/iosMain/.../MainViewController.kt` exposes `MainViewController()` to Swift;
  `iosApp/iosApp/iOSApp.swift` hosts it full screen, hides status bar, and re-applies
  `isIdleTimerDisabled` on every activation to keep the display awake.

Dependency versions are in `gradle/libs.versions.toml`.
