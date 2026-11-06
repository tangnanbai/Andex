# Third-party notices

## Termux Android Runtime

The embedded Android runtime bundles the unmodified
`bootstrap-aarch64.zip` asset from
[termux/termux-packages](https://github.com/termux/termux-packages), release
`bootstrap-2026.10.04-r1+apt.android-7`, SHA-256
`c671bde0d9923ede9d9e972634a04042c93b1589b9a2ca9bc5c38848234e8c5b`.
Individual packages retain their licenses and copyright notices from the
bootstrap; package build recipes and corresponding-source references are
maintained in the pinned upstream release.

The terminal emulator and view are the existing vendored Termux modules under
`third_party/termux`; their license is `third_party/termux/LICENSE.md`.
The embedded runtime reuses Aether's existing PRoot, loader, and talloc assets
from `app/src/main/assets/runtimes/alpine/arm64-v8a` without changing them.

Source-only x86_64 emulator assets (excluded from Android distributions since 0.99) include `bootstrap-x86_64.zip` from the same
release, SHA-256 `9933e09342ec96df3f912483db622f7b7ba78d4e627c80951d0123b6b2fff750`.
Its unmodified PRoot 5.1.107.96 and libtalloc 2.5.0 packages come from the
official Termux repository. Package URLs and checksums are recorded in
`app/src/main/assets/runtimes/termux/x86_64/manifest.json`; corresponding sources
are linked from the upstream `packages/proot` and `packages/libtalloc` recipes.

The in-process Android API bridge is Aether integration code implementing the
limited commands documented in `docs/android-phone-control.md`; it does not
bundle the full Termux:API Android application.

Release distributors must preserve package license notices and provide the
corresponding sources required by each bundled package's license. The source
repository for the bootstrap build is
https://github.com/termux/termux-packages/tree/bootstrap-2026.10.04-r1%2Bapt.android-7.

## OpenMinis/ish-arm64

Aether's iOS runtime integrates [OpenMinis/ish-arm64](https://github.com/OpenMinis/ish-arm64)
using commit `89269e6fef7ab7aa61b133deae90d78e34a09ed1` as its upstream base.

- License: GNU General Public License v3.0. See `third_party/ish-arm64/LICENSE.md`.
- iOS distribution exception and additional terms: see
  `third_party/ish-arm64/LICENSE.IOS`.
- Corresponding source: the complete pinned source, its pinned submodules, and
  Aether's changes are included in this repository under
  `third_party/ish-arm64`.
- Aether integration changes against the upstream base:
  - `asbestos/guest-arm64/gadgets-aarch64/math.S` and
    `asbestos/guest-arm64/gen.c` add missing ARM64 SIMD conversion and move
    instructions needed by the bundled Alpine and Node runtime.
  - `fs/fd.c` and `fs/poll.c` correct descriptor-table locking, duplication,
    `close_range`, and polling lifetime behavior for concurrent guest
    processes.
  - `fs/sock.c` and `fs/sock.h` add the ARM64 control-message layout and
    `SO_PASSCRED`/`SCM_CREDENTIALS` behavior used by local Unix sockets.
  - The Objective-C/Swift host adapter is maintained in `iosApp/Runtime`; the
    reproducible build integration is in `scripts/build-ios-ish-runtime.sh`.

Release archives must include this notice, both iSH license files, the upstream
base commit above, the complete modified corresponding source, and a durable
link to the corresponding Aether source tag.

## Alpine Linux

The iOS runtime starts from Alpine Linux 3.21 AArch64 minirootfs. Individual
packages retain their own licenses. The build downloads the archive from the
official Alpine CDN and verifies SHA-256
`f31202c4070c4ef7de9e157e1bd01cb4da3a2150035d74ea5372c5e86f1efac1`.

## Android bundled ADB and 7zip

Andex includes unmodified official Termux `android-tools` and `7zip` packages
and their shared-library dependencies for ARM64. Historical x86_64 assets remain in source only and are excluded from APKs. Package versions,
download paths and SHA-256 checksums are recorded in
`app/src/main/assets/runtimes/native-tools/<abi>/manifest.json`.
`scripts/prepare-native-tools.py` reconstructs the bundles from those pinned
packages in `https://packages.termux.dev/apt/termux-main/`.
Package-provided copyright and license files are included under `licenses/`
inside each bundle.

ADB is derived from Android Open Source Project (Apache License 2.0).
7zip is by Igor Pavlov and contains code under GNU LGPL, BSD and the unRAR
restriction; see https://www.7-zip.org/license.txt.
Termux package recipes and source references are available at
https://github.com/termux/termux-packages/tree/master/packages.
Dependencies retain their respective upstream licenses.

## Frosted surfaces

Andex uses Haze 1.7.2 by Christopher Banes and contributors for Compose
background blur, licensed under Apache License 2.0.
Source and license: https://github.com/chrisbanes/haze/tree/1.7.2.

## Opus voice encoding

Android uses the pure Java Concentus Opus encoder,
`io.github.jaredmdobson:concentus:1.0.2`. Its BSD-style license and copyright
notice are included in `shared/src/commonMain/composeResources/files/licenses/concentus.txt`
and distributed with application resources.
Source: https://github.com/jaredmdobson/concentus and
https://github.com/lostromb/concentus.

The independently implemented online client was informed by the wire behavior
in `yangmoling/doubaoime-asr` and `melody0709/VoxType`, pinned in
`docs/doubao-ime-asr.md`. Neither reference exposed a clear root license when
checked; their public availability is not treated as permission to copy or
redistribute their code. No reference-client source is bundled.
