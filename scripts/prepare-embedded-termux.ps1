param(
    [string]$ArchivePath = ""
)

$ErrorActionPreference = "Stop"
$release = "bootstrap-2026.10.04-r1%2Bapt.android-7"
$expected = "c671bde0d9923ede9d9e972634a04042c93b1589b9a2ca9bc5c38848234e8c5b"
$root = Split-Path -Parent $PSScriptRoot
$destination = Join-Path $root "app/src/main/assets/runtimes/termux/arm64-v8a/bootstrap.zip"
$temporary = "$destination.download"
New-Item -ItemType Directory -Path (Split-Path -Parent $destination) -Force | Out-Null

try {
    if ($ArchivePath) {
        Copy-Item -LiteralPath $ArchivePath -Destination $temporary
    } else {
        & curl.exe --fail --location --retry 3 `
            "https://github.com/termux/termux-packages/releases/download/$release/bootstrap-aarch64.zip" `
            --output $temporary
        if ($LASTEXITCODE -ne 0) { throw "Bootstrap download failed." }
    }
    $actual = (Get-FileHash -LiteralPath $temporary -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actual -ne $expected) { throw "Bootstrap SHA-256 mismatch: $actual" }
    Move-Item -LiteralPath $temporary -Destination $destination -Force
    Write-Output "Verified Termux bootstrap: $destination"
} finally {
    if (Test-Path -LiteralPath $temporary) { Remove-Item -LiteralPath $temporary }
}
