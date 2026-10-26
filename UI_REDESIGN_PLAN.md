# Dioxamine UI and flow redesign

## Direction

Use the supplied FlashPoint HTML as a visual reference, adapted to native Jetpack Compose. Keep Dioxamine's identity and working device tools. The design uses charcoal surfaces, cyan actions, muted green connection status, lavender secondary accents, generous rounded corners, readable sans-serif text, and monospace device identifiers.

## Implementation plan

1. **Design foundation:** replace the green palette with coordinated dark and light palettes; apply the existing typography intentionally; introduce shared shapes, section headings, icon containers, status chips, and action cards. Preserve System, Light, Dark, AMOLED, and dynamic-color preferences.
2. **App navigation:** provide a consistent branded header and five clear destinations: Overview, Mirror, Flasher, Plugins, and Settings. Preserve each tab's saved Compose state and handle system Back within tool screens before returning home. Use a rail on wide screens and a bottom bar on phones.
3. **Overview:** move connection setup into the scrolling dashboard, show real connection/device metadata, provide an obvious Connect device action, and group searchable tools into everyday management, diagnostics, and device controls. Give the terminal a direct entry point. Keep disconnected tools readable and explain how to enable them.
4. **Connection flow:** introduce a connection sheet for USB guidance, wireless discovery, QR pairing, and manual entry. Preserve existing TLS pairing, multi-device selection, daemon actions, and sideload/recovery handling. Surface connection progress and errors. Make forms and sheets scroll safely with the keyboard and large fonts.
5. **Feature consistency:** align Fastboot action cards and empty states with the theme, retain existing confirmations for device operations, and improve its Back behavior. Apply the shared typography, surfaces, and shapes across mirroring, plugins, settings, and tool details.
6. **Verification:** compile a debug APK, run the existing unit suite and Android lint, review the resulting layouts at compact and expanded widths where an emulator is available, and document any unverified device-dependent behavior.

## Acceptance criteria

- No fabricated telemetry, device names, connection success, or nonfunctional dashboard actions.
- Every existing ADB tool remains reachable, including terminal, recovery, and sideload.
- Offline, connecting, connected, and failed connection states have a clear presentation.
- Primary actions and icon buttons have at least 48 dp touch targets; status is expressed in text as well as color.
- Search has a clear action and an empty-result state. New text uses Android string resources.
- Scrollable content clears system bars, app navigation, and the keyboard; tablet content remains readable.
- Existing connection, plugin permission, and destructive-action safeguards remain intact.

## Verification results

Implemented and compiled successfully. All 78 unit tests passed. Native Android 15 emulator checks covered disconnected and connected states, connection entry, QR pairing, terminal commands, search, Fastboot navigation, reboot cancellation, dark/light themes, larger text, and the tablet layout.

The full lint report remains blocked by 140 existing findings, audited against the source snapshot. No errors were reported in the new UI files or string resources. Physical USB, flashing, second-device TLS pairing, and mirroring hardware still need device testing.

See [implementation and validation notes](docs/ui-redesign/VALIDATION.md) and the [visual review](docs/ui-redesign/preview.png).
