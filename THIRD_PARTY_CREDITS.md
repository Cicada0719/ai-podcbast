# Third-Party Credits

This project includes source snapshots for cooperation review and implementation reference. The upstream projects are not added to Android source sets, are not packaged into the APK, and are not used as runtime dependencies. XingYue English ports selected learning mechanics into native Kotlin/Core and Android Compose code.

## TypeWords

- Project: https://github.com/zyronon/TypeWords
- Snapshot branch: `master`
- Snapshot commit: `0de1e75d41f7438b738a5e83700b49e3d1e3c858`
- Local archive: `third_party/upstream/TypeWords/`
- License: GPL-3.0
- Referenced ideas: typing-based vocabulary practice, dictation, self-test modes, article/sentence dictation, wrong-word review, familiar/favorite word management, and FSRS-style learning loop.
- Integration boundary: Nuxt/Vue/TypeScript application code is archived for review only. The Android APK implements equivalent user-facing behavior with Kotlin engines and Compose UI.

## WordHub

- Project: https://github.com/stibiums/WordHub
- Snapshot branch: `main`
- Snapshot commit: `fa0c50c4acd28e857dba5d985edf8c052369dadd`
- Local archive: `third_party/upstream/WordHub/`
- License: MIT
- Referenced ideas: word library management, query history, learning/review flows, achievements, learning analytics, Wordle, and description-to-word games.
- Integration boundary: Qt/C++ desktop code is archived for review only. The Android APK implements mobile-native practice, analytics, achievement, and game flows.

## Cooperation Statement

The user/project side has stated that cooperation and reference permission has been obtained from the relevant project parties. This repository records that statement and the exact upstream sources used for review. Formal authorization records are retained by the project parties and are not fabricated in this repository.

## Packaging Boundary

- `third_party/upstream/` is not registered as an Android Gradle source set.
- The archived projects are not copied into `assets/`, native libraries, or runtime resources.
- XingYue English remains a GPLv3-oriented Android-first project; MIT components can be incorporated into GPLv3-compatible work, and the GPL-3.0 TypeWords reference is compatible with the current license direction.
