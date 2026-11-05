"""Test the saved provider without printing or persisting its credentials."""
import json
import subprocess
import urllib.request
import urllib.error
from urllib.parse import urlparse

ADB = r"D:\vb\.android-sdk\platform-tools\adb.exe"
SERIAL = "emulator-5554"

def varint(data, pos):
    value = shift = 0
    while True:
        byte = data[pos]
        pos += 1
        value |= (byte & 127) << shift
        if byte < 128:
            return value, pos
        shift += 7

def fields(data):
    pos = 0
    result = []
    while pos < len(data):
        tag, pos = varint(data, pos)
        wire = tag & 7
        if wire == 2:
            size, pos = varint(data, pos)
            result.append((tag >> 3, data[pos:pos + size]))
            pos += size
        elif wire == 0:
            value, pos = varint(data, pos)
            result.append((tag >> 3, value))
        elif wire in (1, 5):
            size = 8 if wire == 1 else 4
            pos += size
        else:
            raise ValueError("Unsupported preference encoding")
    return result

data = subprocess.check_output([
    ADB, "-s", SERIAL, "exec-out", "run-as", "com.android.andex.debug",
    "cat", "files/datastore/andex_settings.preferences_pb",
])
prefs = {}
for number, entry in fields(data):
    if number != 1:
        continue
    record = dict(fields(entry))
    key = record[1].decode()
    value = dict(fields(record[2]))
    if 5 in value:
        prefs[key] = value[5].decode()

configs = []
for raw in prefs.values():
    if raw.startswith("[") and '"apiKey"' in raw:
        try:
            configs = json.loads(raw)
        except ValueError:
            pass
config = next((x for x in configs if "deepseek" in (x.get("piProviderId", "") + x.get("name", "")).lower()), None)
if not config:
    raise SystemExit("No saved DeepSeek provider found.")
base = config["baseUrl"].rstrip("/")
model = config.get("modelId") or next(iter(config.get("enabledModelIds", [])), "")
print(json.dumps({"provider": config.get("name"), "host": urlparse(base).hostname, "model": model}, ensure_ascii=False))

def request(path, body=None):
    req = urllib.request.Request(
        base + path,
        data=json.dumps(body).encode() if body else None,
        headers={"Authorization": "Bearer " + config["apiKey"], "Content-Type": "application/json"},
    )
    try:
        with urllib.request.urlopen(req, timeout=60) as response:
            return response.status, json.load(response)
    except urllib.error.HTTPError as failure:
        # Error bodies could echo authorization details. Report only status.
        return failure.code, {}

status, reply = request("/chat/completions", {
    "model": model, "messages": [{"role": "user", "content": "Reply with exactly Andex API OK."}],
    "max_tokens": 64, "stream": False,
})
print(json.dumps({"chat_http_status": status, "reply": reply.get("choices", [{}])[0].get("message", {}).get("content", ""), "usage": reply.get("usage", {})}, ensure_ascii=False))
status, balance = request("/user/balance")
print(json.dumps({"balance_http_status": status, "balance_available": bool(balance.get("balance_infos"))}, ensure_ascii=False))
