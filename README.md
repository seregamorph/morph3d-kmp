# Morph3D KMP

A Kotlin Multiplatform (Compose) remake of **MORPH3D**, a DOS demo written in x86 assembler
by Courtney S. Sharp in November 1993. The original sources are in [dos/](./dos), see
[dos/screenshots](./dos/screenshots) for how it looked in DOSBox.

A figure of 200 colored dots rotates and roams in a black "room". The dots flow along the figure
leaving fading trails, the figure periodically morphs into another one (lissajous curves, torus,
sphere, cylinder, christmas tree, box, fountain, ...) while its color set cross-fades.

The first supported platform is macOS (desktop JVM).

## Project structure

* [shared](./shared/src/commonMain/kotlin/com/github/seregamorph/morph3d_kmp) - platform independent code:
    - `Figures.kt`, `Palettes.kt` - figure generators and color sets ported from `MORPHSUB.ASM` / `3DTRANS.ASM`
    - `Morph3DEngine.kt` - the simulation: morphing, dot motion, rotation, perspective projection, trails
    - `Morph3DScene.kt` - Compose canvas rendering
* [desktopApp](./desktopApp/src/main/kotlin/com/github/seregamorph/morph3d_kmp) - desktop entry point

## Differences from the original

* Resolution independent: the original 320x200 screen space is scaled uniformly to the window/display,
  dots are drawn as anti-aliased circles.
* The animation is time based (60 original frames per second), independent of the display refresh rate.
* The 4 discrete depth shades of a color set are blended smoothly.
* All 9 color sets are used, the next figure/color set is never the same as the current one.

## Running

- Full screen, like a screen saver (exits on any key, mouse click or mouse move):
  `./gradlew :desktopApp:run`
- In a window (exits on Esc):
  `./gradlew :desktopApp:run --args="--window"`
- Build a macOS disk image: `./gradlew :desktopApp:packageDmg`

## Running tests

`./gradlew :shared:jvmTest`
