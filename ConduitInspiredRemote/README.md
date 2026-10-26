# TV Remote — Conduit-inspired Android prototype

An original Kotlin + Jetpack Compose Android project with a dark remote-control UI and local-network Apple TV service discovery.

## Important status note

**This is a prototype source project, not a finished functional Apple TV remote.** This environment has no Android SDK/Gradle installation and cannot reach the internet to download build dependencies, so an installable APK could not be compiled here. The project ZIP is provided so it can be opened and built in Android Studio.

### Implemented in this prototype
- Native Android app shell using Kotlin and Jetpack Compose.
- Local Bonjour/DNS-SD discovery for `_mediaremotetv._tcp.` and `_companion-link._tcp.` services.
- Displays discovered service name, resolved IP address, port, and service type.
- Rescan UI and discovery timeout/status messages.
- Original remote-control layout, pairing-code dialog, and keyboard-entry dialog.

### Not implemented yet (do not treat as working controls)
- Secure Apple TV pairing / credential generation and storage.
- Authenticated MRP or Companion Link session establishment.
- Sending navigation, home/back, playback, volume, power, or keyboard commands.
- Validation against a physical Apple TV or different tvOS versions.

The pairing and control buttons intentionally report that the secure protocol layer is missing; they do not pretend to control a TV. Service discovery only proves that a compatible service was advertised on the local network.

## Build an APK in Android Studio

1. Install the latest stable Android Studio.
2. Extract this ZIP and choose **Open** on the `ConduitInspiredRemote` folder.
3. Allow Android Studio to install the required Android SDK Platform 35 and Gradle/plugin dependencies.
4. If Android Studio asks to create/update the Gradle wrapper, accept its recommendation.
5. Connect an Android phone with USB debugging enabled, or create an emulator.
6. Choose **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
7. The debug APK will be placed at `app/build/outputs/apk/debug/app-debug.apk`.

For a command-line build after Android Studio has configured the wrapper:

```bash
./gradlew assembleDebug
```

## Testing discovery

- Put the Android phone and Apple TV on the same Wi-Fi/LAN.
- Make sure client/AP isolation is disabled on the router.
- Keep the Apple TV awake and open the app, then tap **Find Apple TV**.
- Discovery can fail if the router blocks multicast DNS or the Apple TV does not advertise these services in its current state.

## Next engineering step for real control

Implement the secure protocol client (pairing, credential handling, encrypted session, protobuf framing and command mapping) for the advertised MRP and/or Companion services. These protocols are reverse-engineered and can change across tvOS versions. Consult the pyatv protocol documentation and feature matrix before selecting the supported device/protocol combinations:

- https://pyatv.dev/documentation/protocols/
- https://pyatv.dev/documentation/supported_features/
- https://pyatv.dev/documentation/scan_pair_and_connect/

Do not ship this prototype as a working remote until secure pairing, navigation, playback and keyboard commands have been implemented and tested on real hardware.
