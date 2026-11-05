# Android phone control

This addition is Android-only. Existing Alpine, Shizuku, Root, chat, providers,
extensions, and iOS behavior are retained.

## Embedded Termux

- Fresh installations initialize the bundled ARM64 Termux bootstrap automatically,
  enable the Termux runtime, and select it when no default runtime is saved.
- The bootstrap is part of the APK. Initial extraction does not need a download,
  an external Termux APK, `RUN_COMMAND`, or `allow-external-apps`.
- Files live in Andex's private `files/runtimes/termux` directory. PRoot maps
  that directory to the official `/data/data/com.termux` prefix, including the
  package-manager cache. This is path compatibility, not privilege elevation.
- The interactive terminal reuses Andex's existing Termux terminal emulator and
  view. The existing managed bash/file/runtime bridge uses the same embedded home.
- `pkg`/`apt` package installation needs Internet access. ADB is an optional
  `android-tools` package installed using the button on the Phone control page.
- Existing external Termux files are not migrated or modified. Turning off
  **Use embedded Termux** restores the original external-Termux transport.
- Existing saved default-runtime choices are not overwritten at every launch.
  **Use Termux as default runtime** explicitly changes an existing choice.
- Andex's existing Pi AI kernel still runs in Alpine. Embedded mode also
  prepares its bundled root filesystem automatically, while commands and files
  default to Termux. The kernel's existing first-use Node/dependency installation
  and model configuration still need network access; they are not a second
  external app or a manual runtime-initialization step.

Pinned bootstrap:

- Upstream: `termux/termux-packages`
- Release: `bootstrap-2026.10.04-r1+apt.android-7`
- Asset: `bootstrap-aarch64.zip`
- SHA-256: `c671bde0d9923ede9d9e972634a04042c93b1589b9a2ca9bc5c38848234e8c5b`
- APK asset: `runtimes/termux/arm64-v8a/bootstrap.zip`
- Re-download: `powershell -File scripts/prepare-embedded-termux.ps1`

The Android build still targets API 28, as upstream does for executable
app-private runtimes. Only ARM64 is currently bundled, matching the existing APK.

## Native Android APIs

The following commands are supplied by an authenticated, loopback-only,
in-process API bridge and do not need the separate Termux:API APK:

```sh
termux-battery-status
termux-clipboard-get
printf 'example' | termux-clipboard-set
termux-toast 'example'
termux-vibrate 200
termux-setup-storage
```

This is a limited compatibility implementation, **not the complete termux-api
project**. In particular, camera, microphone, location, contacts, SMS, and
telephony APIs are not implemented or requested. Installing the upstream
`termux-api` CLI package does not supply its companion Android service.

All processes run with Andex's UID. Its manifest declares Internet and vibration
permissions; shared storage is requested by Andex when the user selects the
permission button or runs `termux-setup-storage`. Android 11+ uses the system's
all-files-access page; older versions use runtime storage permission requests.
The `~/storage` symlinks do not circumvent permission checks. Clipboard access
remains subject to Android's foreground-app restrictions.
Clipboard shell commands additionally require the Phone control consent switch,
so an ordinary AI shell call cannot expose clipboard content before that consent.

## AI control

The `phone_control` host tool is available by default in embedded mode. Its
ordinary Termux shell, app launch, and battery actions work out of the box.

Settings > Phone control contains a separate **Allow AI screen and clipboard
control** switch. It defaults to off, with an explicit disclosure before enabling.
The switch gates ADB, accessibility, and native clipboard shell commands.
Switching it off rejects subsequent enhanced-control actions, including calls
from a conversation that already obtained the tool definition. Ordinary Termux
shell tools and the interactive terminal remain available.
This switch gates Andex's host tools and native API bridge, not a security
sandbox around arbitrary shell code. Once the user has paired ADB, terminal
commands can invoke its client using that runtime's private keys. Use Disconnect
and revoke Andex's pairing in Android Wireless debugging to revoke that
transport itself. Android permission revocation remains the authority for
storage and accessibility.

The tool defaults to `backend: "termux"` and never silently switches backend.

| Backend | Actions |
| --- | --- |
| `termux` | `shell`, `launch`, `battery` |
| `adb` | `shell`, `snapshot`, `screenshot`, `launch`, `tap`, `swipe`, `text`, `key` |
| `accessibility` | `snapshot`, `screenshot`, `launch`, `tap`, `swipe`, `click`, `text`, `key` |
| any | `status` |

Termux has ordinary app privileges. `input tap` in a local Termux shell cannot
inject touches into other apps without an authorized privileged backend.
Screen input requires the user to enable accessibility or connect ADB.
Screen coordinates are normalized 0..1000, not raw device pixels.
Accessibility selectors must uniquely identify a visible node; ambiguous
selectors fail. Password-node text is redacted from accessibility snapshots.
ADB text input is limited to ASCII without percent signs; accessibility supports
Unicode replacement text.

Screenshots are returned as model image content rather than base64 conversation
text. Accessibility screenshots require Android 11+. Secure windows may deny
capture. Enabling control can disclose visible screen content to the selected
model provider.

## Wireless ADB

1. Install ADB tools on the Phone control page.
2. Open Developer settings and enable Wireless debugging (Android 11+).
3. Choose pairing with a pairing code in Android. Enter the **pairing** IPv4
   address/port and six-digit code in Andex, then select Pair.
4. Enter the separate **connection** IPv4 address/port from the Wireless debugging
   page, then select Connect. Pairing and connection ports are usually different.
5. Use Refresh status to check the transport. Ports may change after restarting
   Wireless debugging. ADB does not auto-pair or silently select another device.

Every control command uses an explicit `adb -s <IPv4:port>` transport. Pairing
codes are not persisted in preferences or diagnostic logs; the temporary command
script is removed when the dispatch returns. Private ADB keys live in the
embedded home and are excluded from backup and device transfer, along with
phone-control consent and connection state.

## Verification

The Android app is named Andex. Embedded Termux controls are located in the
existing Termux settings page; Phone control contains ADB and accessibility.
First launch automatically prepares Termux and its `~/storage` links and requests
storage access. Android 11+ uses the system all-files-access settings page, which
still requires the user's approval. No manual `termux-setup-storage` is needed.
Root automatic setup follows the selected Termux transport. Embedded mode
verifies su authorization, initializes Andex's own runtime, and probes bash
without requiring `com.termux`, extracting an external APK, or granting the
external RUN_COMMAND permission. A failed embedded command probe is reported as
an initialization failure rather than a missing Termux installation.

LDPlayer 9 (Android 9, x86_64 with ARM translation) was used for installation and
UI checks on October 10, 2026. The Android 9 direct PRoot launch path was verified,
but command execution was blocked by `ptrace(TRACEME): Permission denied`.
This emulator does not establish ARM64 phone runtime compatibility.

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug --no-daemon
adb devices -l
adb -s SERIAL install -r app/build/outputs/apk/debug/app-debug.apk
adb -s SERIAL shell pm path com.android.andex.debug
```

The upstream debug APK has `.debug` as its application ID suffix. Verify the
actual variant identifier rather than assuming the release package name.

Device acceptance checks:

- Fresh install, offline first launch: Termux initializes and is the default
  runtime, with no external Termux installed.
- Embedded terminal: `printf test`, `pkg list-installed`, `pwd`, Ctrl-C, Unicode
  input, and terminal resize work.
- Long shell command: managed streaming, fetch, cancel, and background task
  retention work without leaving a foreground dispatch waiting on the tracer.
- Native bridge: battery, clipboard (foreground), toast, vibration, and storage
  permissions follow Andex; another local client without the token is rejected.
- Optional ADB: pair, connect, inspect state, explicit transport, Unicode error,
  screenshot, rotation-aware coordinates, disconnect, and port changes.
- Accessibility: user grant, snapshot, unique-node click, gesture callback,
  Unicode text, screenshot failure for secure windows, service disconnect.
- Consent off: subsequent ADB/accessibility/clipboard calls fail; Termux shell,
  launch, and battery actions remain available; original Agent Mode is unchanged.
- Existing install: saved defaults remain intact and external transport is
  available when embedded mode is switched off.

No connected Android device is assumed by these unit tests. PRoot/package
compatibility, OEM accessibility differences, and wireless ADB need the device
acceptance checks before distributing this build.

## Bundled tools and Agent skills

ADB and standalone `7zz` are included for ARM64 and x86_64, with pinned package
checksums and licenses. Startup extracts them into private storage. Their host
tools work independently of the PRoot runtime and package mirrors. ADB uses
private keys and server port 5038; its runtime directory is excluded from backup.

The enabled built-in `archive-7zip` skill discovers `andex_archive` for listing,
compression and extraction. Extraction checks entry paths and rejects links,
more than 10,000 entries, truncated inspections and expanded sizes above 512 MiB.
Use a new or empty destination; preserve original uploaded files.

The enabled `github-web-research` skill discovers native public GitHub, search
and page tools. Search supports Baidu, Bing and Google without API credentials,
subject to connectivity, CAPTCHA and HTML changes. Pages are fetched as HTTP
source up to 8 MiB, cached for full-text finding and paginated reading; dynamic
JavaScript content requires browser tools. Retrieved content is untrusted.

October 10, 2026 LDPlayer app-process checks passed ADB version, 7zip compression
and extraction with content comparison, accessibility Settings launch, snapshot,
tap, Back and Home, GitHub API, full-page fetch/find and Bing search. Baidu
returned no parseable results; Google failed to connect in this network. These
checks do not establish complete Pi/Termux Agent execution on this emulator.
