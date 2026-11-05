"""Advance Andex fix releases by 0.01; keep Android/iOS build numbers increasing."""
from decimal import Decimal
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
android = root / "app/build.gradle.kts"
ios = root / "iosApp/Configuration/Base.xcconfig"
source = android.read_text(encoding="utf-8")
match = re.search(r'\?: "(\d+\.\d+)"\nval piBridgeProjectDir', source)
if not match:
    raise SystemExit("Cannot find the default Android version")
version = str((Decimal(match[1]) + Decimal("0.01")).quantize(Decimal("0.01")))
source = source[:match.start(1)] + version + source[match.end(1):]
source = re.sub(r"versionCode = (\d+)", lambda m: f"versionCode = {int(m[1]) + 1}", source, count=1)
ios_source = ios.read_text(encoding="utf-8")
ios_source = re.sub(r"MARKETING_VERSION = [^\n]+", f"MARKETING_VERSION = {version}", ios_source, count=1)
ios_source = re.sub(r"CURRENT_PROJECT_VERSION = (\d+)", lambda m: f"CURRENT_PROJECT_VERSION = {int(m[1]) + 1}", ios_source, count=1)
android.write_text(source, encoding="utf-8")
ios.write_text(ios_source, encoding="utf-8")
print(version)
