"""Use the debug-only, shell-permission-protected receiver to exercise the real controller."""
import base64
import json
import os
import shutil
import subprocess
import sys
import time
import uuid
from pathlib import Path

ADB = os.environ.get("ADB", shutil.which("adb") or "adb")
SERIAL = os.environ.get("ANDROID_SERIAL", "")
PACKAGE = "com.android.andex"

def adb(*args):
    return subprocess.check_output(
        [ADB, *(["-s", SERIAL] if SERIAL else []), *args], stderr=subprocess.STDOUT, timeout=35
    )

def probe(arguments):
    token = uuid.uuid4().hex
    encoded = base64.b64encode(json.dumps(arguments, ensure_ascii=False).encode()).decode()
    start = time.monotonic()
    adb("shell", "am", "broadcast", "-n", PACKAGE + "/.phonecontrol.PhoneControlProbeReceiver",
        "--es", "request_b64", encoded, "--es", "probe_id", token)
    deadline = start + 28
    while time.monotonic() < deadline:
        try:
            result = json.loads(adb("exec-out", "run-as", PACKAGE, "cat", "cache/phone-probe-result.json"))
            if result.get("probe_id") == token:
                result.pop("probe_id", None)
                result["probe_duration_ms"] = round((time.monotonic() - start) * 1000)
                return result
        except (ValueError, subprocess.SubprocessError):
            pass
        time.sleep(.12)
    raise TimeoutError("Phone control probe did not complete.")

if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    arguments = json.loads(sys.argv[1]) if len(sys.argv) > 1 else {"action": "status"}
    result = probe(arguments)
    if len(sys.argv) > 2:
        Path(sys.argv[2]).write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(result, ensure_ascii=False, indent=2))
