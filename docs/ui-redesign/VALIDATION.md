# Dioxamine redesign: implementation and verification

Implemented on 8 October 2026. The supplied FlashPoint HTML informed the visual direction; the result is native Jetpack Compose UI.

## What changed

- Charcoal surfaces, cyan primary actions, soft green connection indicators, lavender accents, shared rounded shapes, and an applied typography scale. A fresh installation defaults to Dark; saved theme preferences, Light, System, AMOLED, and dynamic colors remain available.
- Overview replaces the long built-in tools list. It has a real device connection card, copyable address, searchable tools in three groups, and a direct terminal entry. Device actions remain disabled while disconnected.
- Connection setup uses a scrollable bottom sheet with USB guidance, wireless discovery, QR pairing, manual address entry, active-device selection, and existing daemon controls.
- Wireless forms validate IPv4/localhost addresses, ports 1–65535, and six-digit pairing codes. Both typed and pasted `host:port` input are supported. QR pairing opens in its own full-screen dialog.
- Navigation uses five destinations, saved tab state, and a rail above 720 dp. Tool search and scroll state survive opening and returning from a tool. The grid uses one to three columns and switches to wider cards at larger font sizes.
- Fastboot has a guided empty state, consistent action cards, and system Back handling for its shell and detail screens. Reboot actions now require confirmation naming the target device.
- Shared styling extends to settings, mirroring, plugins, and tool details. Mirroring has a Connect device shortcut.
- All 69 new strings are supplied in English and the five existing translated locales: German, Hindi, Russian, Persian, and Simplified Chinese. Format placeholders were checked for consistency.

## Build and unit tests

`gradlew.bat :app:assembleDebug :app:testDebugUnitTest` completed successfully.

**78 tests passed, with zero failures and zero errors:** 74 existing tests and four added connection validation tests. The added tests cover port boundaries, valid and invalid endpoints, complete pairing codes including leading zeroes, and parsing pasted addresses.

The application was built with the project's existing SDK, Gradle, and Kotlin configuration. No framework or dependency migration was required.

## Runtime checks

Test environment: Android 15 / API 35 AOSP x86_64 emulator. Phone viewport: 1080 × 2340 px at 440 dpi, approximately 393 dp wide. Expanded viewport: 1920 × 1200 px at 240 dpi, approximately 1280 × 800 dp.

| Check | Result |
| --- | --- |
| Launch and disconnected Overview | Passed; setup action and disabled tools are visible |
| Dark and light themes | Passed; captured both appearances |
| USB / wireless connection sheet | Passed; setup choices and guidance visible |
| Manual address entry with keyboard | Passed; fields remain reachable; typing `127.0.0.1:1` populates the two fields correctly |
| Unavailable endpoint | Passed; connecting to port 1 displayed a real connection error |
| QR pairing | Passed; QR generated; Back returns to Overview |
| Empty tool search and clear action | Passed |
| Search state after opening and returning from a tool | Passed on the packaged APK |
| Fastboot shell → Actions → Overview with system Back | Passed |
| Local ADB connection | Passed; connected to the emulator's real ADB daemon |
| Connected metadata | Passed; actual model, Android 15, API 35, and `127.0.0.1:5555` displayed |
| ADB terminal | Passed; `echo redesign-ok` returned `redesign-ok` |
| Reboot confirmation and Cancel | Passed; cancellation left the device connected; no reboot executed |
| 150% font scale | Reviewed; scrolling retains access to content |
| Expanded layout | Reviewed; navigation rail and three-column tool grid displayed |

Screenshots in this directory come from the running Android app. The connected device shown is the emulator, not sample data.

## Lint and remaining limits

The first lint run encountered an internal Kotlin analyzer failure. A later run completed and produced a report. It reports **140 existing errors**: 76 resource-access checks, 30 missing translations in older resources, 17 Android API compatibility checks, 14 string-format issues, two receiver flag checks, and one JavaScript interface check.

Every reported error location was compared with the pre-redesign source snapshot; all were present in that source. The report contains **no errors in the new design-system, Overview, connection UI, or new string-resource files**. These existing errors have not been hidden by a lint baseline or disabled checks. See [lint-report.html](lint-report.html) for the complete report.

Physical USB OTG, TLS pairing with a second Android device, mirroring codecs, partition flashing, and device-specific recovery operations need hardware validation. No destructive device operation was performed. Layout review used Android 15; minimum-version compatibility remains subject to the existing lint findings. The translations have not had a native-speaker review.

## Files

- [Design and execution plan](../../UI_REDESIGN_PLAN.md)
- [Visual review](preview.png)
- [Installable debug APK](../../deliverables/Dioxamine-redesign-debug.apk)
- [Connected Overview](overview-connected.png)
- [Connection sheet](connection-sheet-dark.png)
- [Fastboot](flasher.png)
- [Tablet layout](tablet.png)
- [Large text](large-text.png)
- [Terminal](terminal.png)

The original `app/src/main` was copied to `../.redesign-backup/main` outside the project directory before editing, because this workspace was not a Git checkout.

APK SHA-256: `d173d2bc3143dcd7f70685ff51a72ed3c87c9c4e85224772acec9968e189fb78`.

The QA emulator was stopped. Automatic approval review blocked deletion of the remaining generated emulator files with “blocked by policy.” The downloaded ZIP was removed, but emulator support files and disk images remain in `../.android-ui-qa` outside this project. They are not included in the APK.
