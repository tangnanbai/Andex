"""Bundle official Termux ADB/7zip binaries and runtime libraries, with checksums."""
import concurrent.futures
import gzip
import hashlib
import io
import json
from pathlib import Path
import re
import tarfile
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
REPOSITORY = "https://packages.termux.dev/apt/termux-main/"
CACHE = ROOT.parent / "native-tools-downloads"
CACHE.mkdir(exist_ok=True)

def download(relative):
    path = CACHE / relative.replace("/", "_")
    if not path.exists():
        with urllib.request.urlopen(REPOSITORY + relative, timeout=60) as response:
            path.write_bytes(response.read())
    return path.read_bytes()

def ar_members(data):
    assert data[:8] == b"!<arch>\n"
    offset = 8
    while offset + 60 <= len(data):
        header = data[offset:offset + 60]
        size = int(header[48:58])
        name = header[:16].decode().strip().rstrip("/")
        yield name, data[offset + 60:offset + 60 + size]
        offset += 60 + size + size % 2

def prepare(abi, arch):
    asset = ROOT / "app/src/main/assets/runtimes/native-tools" / abi
    manifest_path = asset / "manifest.json"
    index = gzip.decompress(download(f"dists/stable/main/binary-{arch}/Packages.gz")).decode()
    packages = {}
    for block in index.split("\n\n"):
        entry = {}
        for line in block.splitlines():
            if ": " in line and not line.startswith(" "):
                key, value = line.split(": ", 1)
                entry[key] = value
        if "Package" in entry:
            packages[entry["Package"]] = entry
    if manifest_path.exists():
        pinned = json.loads(manifest_path.read_text(encoding="utf-8"))
        for entry in pinned["packages"]:
            packages[entry["name"]] = {
                "Package": entry["name"], "Version": entry["version"],
                "Filename": entry["filename"], "SHA256": entry["sha256"],
            }
    required = set()
    def dependency(name):
        if name in required:
            return
        entry = packages[name]
        required.add(name)
        for dep in entry.get("Depends", "").split(","):
            dep = re.split(r"\s|\(", dep.strip())[0]
            if dep and dep in packages:
                dependency(dep)
    for name in ["android-tools", "7zip"]:
        dependency(name)
    if manifest_path.exists():
        required = {entry["name"] for entry in pinned["packages"]}
    def fetch(name):
        entry = packages[name]
        data = download(entry["Filename"])
        assert hashlib.sha256(data).hexdigest() == entry["SHA256"], name
        return name, data
    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
        blobs = list(pool.map(fetch, sorted(required)))
    asset.mkdir(parents=True, exist_ok=True)
    written = set()
    with zipfile.ZipFile(asset / "tools.zip", "w", zipfile.ZIP_DEFLATED) as output:
        for name, data in blobs:
            archive = next(payload for filename, payload in ar_members(data) if filename.startswith("data.tar"))
            with tarfile.open(fileobj=io.BytesIO(archive), mode="r:*") as tar:
                for member in tar:
                    path = member.name.removeprefix("./")
                    prefix = "data/data/com.termux/files/usr/"
                    if not path.startswith(prefix):
                        continue
                    relative = path[len(prefix):]
                    if member.isfile() and relative.startswith("share/doc/") and relative.endswith(("copyright", "LICENSE", "LICENSE.txt")):
                        output.writestr("licenses/" + name + "/" + Path(relative).name, tar.extractfile(member).read())
                    if member.isfile() and (relative in ("bin/adb", "bin/7zz", "bin/7z") or
                                            relative.startswith("lib/") and ".so" in relative and "/" not in relative[4:]):
                        if relative not in written:
                            output.writestr(relative, tar.extractfile(member).read())
                            written.add(relative)
                    elif member.issym() and relative.startswith("lib/") and ".so" in relative:
                        # Resolve symlink inside the package without host symlink privileges.
                        target = str(Path(member.name).parent / member.linkname).replace("\\", "/")
                        try:
                            member_by_path = {m.name.removeprefix("./"): m for m in tar.getmembers()}
                            linked = tar.extractfile(member_by_path[target.removeprefix("./")])
                            if linked and relative not in written:
                                output.writestr(relative, linked.read())
                                written.add(relative)
                        except (KeyError, ValueError):
                            pass
    assert "bin/adb" in written and ("bin/7z" in written or "bin/7zz" in written)
    manifest = {
        "abi": abi, "archive_sha256": hashlib.sha256((asset / "tools.zip").read_bytes()).hexdigest(),
        "packages": [{"name": name, "version": packages[name]["Version"],
                      "filename": packages[name]["Filename"], "sha256": packages[name]["SHA256"]} for name in sorted(required)]
    }
    (asset / "manifest.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")
    print(abi, "bundled", len(written), "files", (asset / "tools.zip").stat().st_size, flush=True)

with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
    list(pool.map(lambda pair: prepare(*pair), [("arm64-v8a", "aarch64"), ("x86_64", "x86_64")]))
