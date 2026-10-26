# Dioxamine — Complete Architecture, Component Map & System Flows

**Dioxamine** is a production-grade, Android-native systems administration and device management suite. It allows Android devices to communicate directly with other devices (or itself in local loopback) via **ADB (Android Debug Bridge)**, **Fastboot**, **Scrcpy low-latency display mirroring**, and **sandboxed web-based plugins**—without needing a PC host.

---

## 1. High-Level Architectural Map

```
+===================================================================================+
|                              PRESENTATION LAYER (UI)                              |
|   Jetpack Compose | Material 3 ("Matter" Dark Blue React Style) | Edge-to-Edge    |
|                                                                                   |
|  [ ADB Hub ]        [ Scrcpy Mirror ]       [ Fastboot Hub ]     [ Settings ]     |
|  - Quick Connect     - Video Surface         - Partition Flashing - App Theme     |
|  - Discovery Sheet   - UHID Touch/Keys       - Bootloader Lock    - WakeLock Svc  |
|  - Built-in Actions  - Low-Latency Audio     - Flash Images       - Plugin Policy |
|  - Interactive Shell - Session Recording     - Variable Query     - Logs / About  |
+===================================================================================+
                                         │
                         State Observers │ Dispatch User Intents
                                         ▼
+===================================================================================+
|                            STATE & VIEWMODEL LAYER                                |
|                                                                                   |
|   • AdbViewModel: Manages device connections, active device, and commands         |
|   • FastbootViewModel: Manages USB endpoints, flashing state, and logging         |
|   • ShellViewModel: ANSI terminal state, input buffering, and scroll tracking     |
|   • PluginRepository: Manifest verification, permission checks, and runner       |
|   • ScrcpySession: Mirroring lifecycle, decoder pipeline, and control socket      |
+===================================================================================+
                                         │
                         Async Coroutine │ Channels & StateFlow
                                         ▼
+===================================================================================+
|                        COMPANION DAEMONS & RUNTIME AGENTS                         |
|                                                                                   |
|   1. scrcpy-server.jar: Pushed to target device for video/audio & UHID control    |
|   2. diox-agent.jar: Rootless Android app_process process manager on target       |
|   3. dxls (native ELF): Ultra-fast filesystem indexer communicating via sockets   |
+===================================================================================+
                                         │
                         Transports & IO │ Packet Encoding / Decoding
                                         ▼
+===================================================================================+
|                             PROTOCOL & TRANSPORT LAYER                            |
|                                                                                   |
|   • ADB Protocol Engine: Sync, Open, Stream, Shell, Sideload, Auth Keys          |
|   • Fastboot Protocol Engine: Download, Flash, Boot, Erase, Oem commands         |
|   • USB Host Transport: UsbManager, UsbDeviceConnection, Bulk Endpoints          |
|   • TCP/TLS Transport: Android 11+ Pairing, mDNS / NSD Discovery, Wi-Fi ADB      |
+===================================================================================+
                                         │
                         Physical Link   │ Wireless Socket
                                         ▼
+===================================================================================+
|                           HARDWARE & TARGET SYSTEMS                               |
|                                                                                   |
|   [ Target Device via USB OTG ]  [ Target Device via Wi-Fi ]  [ Local Loopback ] |
+===================================================================================+
```

---

## 2. Core System Flows

### Flow 1: ADB Device Discovery & Connection Lifecycle

```
[User / UI]                    [AdbViewModel]              [Transport]              [Target Device]
     │                               │                          │                          │
     ├─── Connect via TCP/USB ──────►│                          │                          │
     │                               ├── Check USB or Socket ──►│                          │
     │                               │                          ├── SYN / CONNECT packet ─►│
     │                               │                          │◄── AUTH Token (0x01) ────┤
     │                               ├── Sign with RSA Key ────►│                          │
     │                               │                          ├── AUTH Response (0x02) ─►│
     │                               │                          │◄── CNXN (Connected) ─────┤
     │◄── ConnectionState.Connected ─┤                          │                          │
     │                               ├── Retrieve Device Props ─┼─────────────────────────►│
     │◄── Updated Device Header ─────┤                          │                          │
```

1. **Discovery / Input**: The user initiates a connection via USB OTG auto-detection, manual TCP `IP:Port`, or mDNS discovery.
2. **Authentication**: The transport receives an `AUTH` challenge token from the target device. Dioxamine's keystore signs the challenge using the stored RSA 2048-bit key.
3. **Session Establishment**: Upon receiving `CNXN`, the target is registered in `AdbViewModel.devices` and state emits `ConnectionState.Connected`.
4. **Information Retrieval**: The app automatically queries `ro.product.model`, `ro.build.version.release`, battery stats, and ABI for display in the device selector card.

---

### Flow 2: Scrcpy Display Mirroring & Audio Pipeline

```
[ScrcpyScreen]                [ScrcpySession]             [Target Device]          [Hardware Decoder]
      │                              │                           │                          │
      ├── Start Mirroring ──────────►│                           │                          │
      │                              ├── Push scrcpy-server.jar ─┼─────────────────────────►│
      │                              ├── Launch app_process ─────┼─────────────────────────►│
      │                              │   (scrcpy-server)         │                          │
      │                              ├── Connect to Video Socket ┼─────────────────────────►│
      │                              │◄── H.264 / H.265 Stream ──┤                          │
      │                              ├───────────────────────────┼── Feed NAL Units ───────►│
      │                              │                           │                          │◄── Decoded Frames
      │◄── Render on SurfaceView ────┼───────────────────────────┼──────────────────────────┤
      │                              │                           │                          │
      ├── Touch Event (Down/Move) ──►├── UHID Input Packet ─────►│ (Injected at Kernel lvl) │
```

1. **Bootstrap**: `scrcpy-server.jar` is deployed to `/data/local/tmp/` on the target device via ADB sync.
2. **Execution**: The server is started in `app_process` with user-specified video bitrates, frame rates, and codec parameters.
3. **Tunneling**: Two abstract socket connections are established: a video/audio multiplex stream and a bidirectional control stream.
4. **Decoding & Rendering**: Incoming H.264/H.265 video packets are processed by Android's native `MediaCodec` and rendered directly onto `SurfaceView`.
5. **Virtual UHID Input**: Gestures and keyboard events are translated into USB HID reports and sent over the control socket, providing rootless, lag-free input injection.

---

### Flow 3: Fastboot Protocol & Partition Management

```
[FastbootScreen]              [FastbootViewModel]           [UsbManager]             [Fastboot Device]
      │                               │                          │                           │
      ├── User Selects "Flash Image" ─►│                          │                           │
      │                               ├── Open SAF Document ────►│                           │
      │                               │   (Read Image Bytes)     │                           │
      │                               ├── Claim USB Interface ──►│                           │
      │                               │                          ├── "download:XXXXXXXX" ───►│
      │                               │                          │◄── "DATAXXXXXXXX" ────────┤
      │                               │                          ├── Send Raw Payload ──────►│
      │                               │                          │◄── "OKAY" ────────────────┤
      │                               │                          ├── "flash:boot" ──────────►│
      │                               │                          │◄── "OKAY" ────────────────┤
      │◄── OperationStatus.Success ───┤                          │                           │
```

1. **Interface Claiming**: `FastbootViewModel` locates the USB device matching class `0xFF`, subclass `0x42`, protocol `0x03` and claims the bulk IN/OUT endpoints.
2. **Command Sequencing**: Fastboot commands (`getvar:all`, `download:<size>`, `flash:<partition>`, `erase:<partition>`, `boot`) are sent synchronously with strict status reading (`OKAY`, `DATA`, `INFO`, `FAIL`).
3. **Safe Streaming**: Firmware images are read directly via Android Storage Access Framework (`FileProvider` / SAF `Uri`) in buffered chunks to minimize RAM footprint.

---

### Flow 4: Interactive ANSI Terminal Shell

```
[ShellScreen]                  [ShellViewModel]              [AdbSession]             [Target PTY]
      │                               │                           │                        │
      ├── User Enters "ls -la" ──────►│                           │                        │
      │                               ├── Format Line + CRLF ────►│                        │
      │                               │                           ├── Send Shell Packet ──►│
      │                               │                           │◄── Raw PTY Bytes ──────┤
      │                               ├── AnsiParser.kt ──────────┤                        │
      │                               │   (Parses SGR colors,     │                        │
      │                               │    cursor movements)      │                        │
      │◄── Render AnnotatedString ────┤                           │                        │
```

1. **PTY Session**: ADB opens an interactive pseudo-terminal stream (`shell,v2,pty:`).
2. **ANSI Interpretation**: `AnsiParser.kt` processes ANSI SGR color codes (16-color, 256-color, and RGB truecolor) into Compose `AnnotatedString` spans.
3. **High-Performance Buffer**: `ShellBuffer.kt` maintains a rolling memory window (up to 5,000 lines) with efficient incremental list diffing.

---

## 3. Package Structure & Module Map

```
io.github.rhythmcache.dioxamine/
├── MainActivity.kt               # Main entry point & React-styled NavigationBar
├── DioxamineApplication.kt       # Application class & global crash / log init
├── adb/
│   ├── AdbViewModel.kt           # Central ADB state coordinator
│   ├── AdbScreen.kt              # ADB Hub dashboard & device connector
│   ├── discovery/                # mDNS & QR code Wi-Fi pairing
│   ├── builtin/                  # Built-in diagnostic action tiles
│   │   ├── AdbActionTile.kt      # Styled React Matter action cards
│   │   ├── filemanager/          # High-performance file browser (dxls)
│   │   ├── packagemanager/       # APK installer & application manager
│   │   ├── processmanager/       # Real-time task manager (diox-agent)
│   │   ├── screencap/            # Frame captures & screenshots
│   │   └── reboot/               # Power state actions (Recovery, Bootloader)
│   └── shell/                    # Interactive ANSI terminal emulator
├── fastboot/
│   ├── FastbootViewModel.kt      # Fastboot session & command orchestrator
│   ├── FastbootScreen.kt         # Fastboot UI, tabs & device card
│   └── FastbootComponents.kt     # Fastboot action tiles & status cards
├── scrcpy/
│   ├── ScrcpyScreen.kt           # Screen mirroring interface & configurator
│   ├── ScrcpyVideoPlayer.kt      # SurfaceView & MediaCodec renderer
│   ├── ScrcpyFloatingNav.kt      # On-screen navigation controls
│   └── ScrcpyConfig.kt           # Video/audio streaming configuration
├── plugin/
│   ├── PluginRepository.kt       # Web plugin management & validation
│   ├── PluginRunnerScreen.kt     # Sandboxed WebView runner
│   ├── PluginSafBridge.kt        # Scoped SAF file access bridge
│   └── PluginPermissionGate.kt   # Dynamic permission consent gate
├── core/
│   ├── Theme.kt                  # Dark Blue "React JS" color palette
│   ├── Typography.kt             # JetBrains Mono developer typography
│   ├── DioxForegroundService.kt  # Keep-alive background service
│   └── AppLogger.kt              # In-memory circular log ring buffer
└── net/
    └── TcpSocket.kt              # Non-blocking network sockets
```

---

## 4. Google Play Store Compliance Matrix

| Requirement | Status | Technical Implementation |
|---|---|---|
| **Target SDK** | Compliant (API 35/36) | `targetSdk = 36`, `compileSdk = 35` exceeding Google Play minimums. |
| **Storage Policy** | Compliant (Zero-Perm) | No `MANAGE_EXTERNAL_STORAGE` or broad `READ_EXTERNAL_STORAGE`. Uses Storage Access Framework (`ActivityResultContracts.OpenDocument`) and `FileProvider`. |
| **Foreground Service** | Compliant (Android 14+) | Declares `FOREGROUND_SERVICE_DATA_SYNC` in manifest and provides runtime type in `startForeground()`. |
| **Dynamic Code Loading** | Compliant | No runtime loading of `.dex` / `.so` files into host app; plugins execute via standard sandboxed WebViews. |
| **App Title Policy** | Compliant | "Dioxamine" (9 characters <= 30 character limit, no emojis or marketing buzzwords). |
| **64-bit Native Support** | Compliant | Native companion assets (`dxls`) are bundled with multi-ABI binaries (`arm64-v8a`, `armeabi-v7a`, `x86_64`). |
| **Security & Privacy** | Compliant | All keys stored in private app directory (`getFilesDir()`); no telemetry or analytics trackers. |
