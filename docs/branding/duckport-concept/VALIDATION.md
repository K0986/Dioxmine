# DuckPort branding verification

Verified 8 October 2026.

- `:app:assembleDebug` completed successfully. Build log: `build/duckport-build.log`.
- All six application string sets resolve `app_name` to `DuckPort`.
- New Android vector resources parse successfully; resource compilation and Kotlin compilation passed.
- Packaged APK badging confirms the `DuckPort` display label and the existing `io.github.rhythmcache.dioxamine` application ID.
- Legacy launcher PNGs, adaptive foreground/background resources, Android 13+ monochrome resources, splash artwork, and the in-app header were updated.
- `adb -s BLZXFO24GB000049 install -r deliverables/DuckPort-debug.apk` succeeded on the connected LXX513 phone. No uninstall or data clearing was performed.
- Starting `.MainActivity` returned `Status: ok`; Android subsequently reported it as the top resumed activity.
- The physical-phone screenshot `phone-duckport.png` was visually reviewed and shows the DuckPort name and duck mark in the Overview header.
- The Play Store icon is a 512 x 512 PNG. Store listing text is drafted in `../PLAY_STORE_NAMING.md`; no listing was published.

APK: `deliverables/DuckPort-debug.apk`

SHA-256: `5fb4e992216c4b648df1e75c31225692a599ab04ca4baed66c32efdea8ad1e54`
