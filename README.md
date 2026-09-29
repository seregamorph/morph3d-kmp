# Morph3D KMP

A Kotlin Multiplatform (Compose) remake of **MORPH3D**, a DOS demo written in x86 assembler
by Courtney S. Sharp in November 1993. The original sources are in [dos/](./dos), see
[dos/screenshots](./dos/screenshots) for how it looked in DOSBox.

A figure of 200 colored dots rotates and roams in a black "room". The dots flow along the figure
leaving fading trails, the figure periodically morphs into another one (lissajous curves, torus,
sphere, cylinder, christmas tree, box, fountain, ...) while its color set cross-fades.

Supported platforms: macOS (desktop JVM), iPhone and iPad (a universal iOS app).

## Project structure

* [shared](./shared/src/commonMain/kotlin/com/github/seregamorph/morph3d_kmp) - platform independent code:
    - `Figures.kt`, `Palettes.kt` - figure generators and color sets ported from `MORPHSUB.ASM` / `3DTRANS.ASM`
    - `Morph3DEngine.kt` - the simulation: morphing, dot motion, rotation, perspective projection, trails
    - `Morph3DScene.kt` - Compose canvas rendering
* [desktopApp](./desktopApp/src/main/kotlin/com/github/seregamorph/morph3d_kmp) - desktop entry point
* [iosApp](./iosApp) - Xcode project of the iOS app for iPhone and iPad, it embeds the `Shared` framework
  built from [shared/src/iosMain](./shared/src/iosMain/kotlin/com/github/seregamorph/morph3d_kmp)

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
- iPhone / iPad: open [iosApp/iosApp.xcodeproj](./iosApp/iosApp.xcodeproj) in Xcode, pick an iPhone or iPad
  simulator (or a device, after selecting a development team in Signing & Capabilities) and run.
  The Kotlin framework is built by Gradle from an Xcode build phase. From the command line:
  ```
  xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 17' build
  xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPad Pro 13-inch (M5)' build
  ```
  The app runs full screen with the status bar hidden and keeps the display awake.

## Running tests

`./gradlew :shared:jvmTest`
