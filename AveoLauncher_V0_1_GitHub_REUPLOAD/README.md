# Aveo Launcher V0.1

Android 10 (API 29) launcher prototype for Diamond / Allwinner A133 head unit.

## Cloud build (no Android Studio)
1. Create a new GitHub repository.
2. Upload the *contents* of this folder to the root of the repository, including `.github/workflows/build-apk.yml`.
3. Go to Actions > Build Aveo Launcher APK > Run workflow (or push to main).
4. Once the run succeeds, download the `AveoLauncher-debug-apk` artifact from the run summary, unzip it, and install `app-debug.apk` on the head unit.

Note: GitHub artifact download requires appropriate repository access/login. APK is debug-signed for testing. Do not remove the original Diamond launcher or Nova Launcher.

## Implemented
- Android HOME launcher activity, landscape
- Dark three-column layout, live clock
- Installed apps list and shortcuts (TLink, Maps, Radio, Music) via labels
- Non-fabricated vehicle-data placeholders

## Not implemented yet
- Actual Aveo 3D model, vehicle data, CAN/MCU bridge, media metadata

## Limitations
- Some proprietary Diamond apps may not be exposed as normal launcher activities.
- Bottom proprietary Diamond bar remains system-managed; available layout space may differ by firmware.
- Launcher defaults can be changed in Android settings if supported.
