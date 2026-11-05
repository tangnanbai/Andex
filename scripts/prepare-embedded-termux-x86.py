"""Bundle native x86_64 Termux and PRoot for Android emulators."""
import gzip
import hashlib
import io
import json
from pathlib import Path
import tarfile
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
ASSET = ROOT / "app/src/main/assets/runtimes/termux/x86_64"
ASSET.mkdir(parents=True, exist_ok=True)
REPO = "https://packages.termux.dev/apt/termux-main/"

def fetch(url):
    with urllib.request.urlopen(url, timeout=90) as response:
        return response.read()

def ar_members(data):
    assert data[:8] == b"!<arch>\n"
    offset = 8
    while offset + 60 <= len(data):
        header = data[offset:offset + 60]
        size = int(header[48:58])
        yield header[:16].decode().strip().rstrip("/"), data[offset + 60:offset + 60 + size]
        offset += 60 + size + size % 2

release = "bootstrap-2026.10.04-r1%2Bapt.android-7"
bootstrapPath = ASSET / "bootstrap.zip"
bootstrap = bootstrapPath.read_bytes() if bootstrapPath.exists() else fetch(
    f"https://github.com/termux/termux-packages/releases/download/{release}/bootstrap-x86_64.zip")
assert hashlib.sha256(bootstrap).hexdigest() == "9933e09342ec96df3f912483db622f7b7ba78d4e627c80951d0123b6b2fff750"
bootstrapPath.write_bytes(bootstrap)
index = gzip.decompress(fetch(REPO + "dists/stable/main/binary-x86_64/Packages.gz")).decode()
packages = {}
for block in index.split("\n\n"):
    entry = dict(line.split(": ", 1) for line in block.splitlines() if ": " in line and not line.startswith(" "))
    if "Package" in entry:
        packages[entry["Package"]] = entry
manifest = {"bootstrap_sha256": hashlib.sha256(bootstrap).hexdigest(), "packages": []}
manifestPath = ASSET / "manifest.json"
if manifestPath.exists():
    for pinned in json.loads(manifestPath.read_text(encoding="utf-8"))["packages"]:
        packages[pinned["Package"]] = pinned
for name in ("proot", "libtalloc", "libandroid-shmem"):
    entry = packages[name]
    data = fetch(REPO + entry["Filename"])
    assert hashlib.sha256(data).hexdigest() == entry["SHA256"]
    payload = next(payload for filename, payload in ar_members(data) if filename.startswith("data.tar"))
    with tarfile.open(fileobj=io.BytesIO(payload), mode="r:*") as tar:
        for member in tar:
            if not member.isfile():
                continue
            basename = Path(member.name).name
            if "/share/doc/" in member.name and basename in ("copyright", "LICENSE", "LICENSE.txt"):
                licensePath = ASSET / "licenses" / name / basename
                licensePath.parent.mkdir(parents=True, exist_ok=True)
                licensePath.write_bytes(tar.extractfile(member).read())
            target = None
            if basename == "proot":
                target = "proot.bin"
            elif basename == "loader":
                target = "loader.bin"
            elif basename.startswith("libtalloc.so."):
                target = "libtalloc.so.2"
            elif basename == "libandroid-shmem.so":
                target = basename
            if target:
                (ASSET / target).write_bytes(tar.extractfile(member).read())
    manifest["packages"].append({key: entry[key] for key in ("Package", "Version", "Filename", "SHA256")})
for name in ("proot.bin", "loader.bin", "libtalloc.so.2", "libandroid-shmem.so"):
    assert (ASSET / name).is_file(), name
    manifest[name + "_sha256"] = hashlib.sha256((ASSET / name).read_bytes()).hexdigest()
(ASSET / "manifest.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")
print(json.dumps(manifest, indent=2), flush=True)
