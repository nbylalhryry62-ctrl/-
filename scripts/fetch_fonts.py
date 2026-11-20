#!/usr/bin/env python3
"""Bundle IBM Plex Sans Arabic (SIL OFL) into flutter_app so Arabic renders identically on
Android, iOS and the web, with no font downloads at runtime. Run from the repo root before `flutter pub get`."""
import os, re, sys, urllib.request

BASE = "https://raw.githubusercontent.com/google/fonts/main/ofl/ibmplexsansarabic/"
WEIGHTS = {"Regular": 400, "Medium": 500, "SemiBold": 600, "Bold": 700}
root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
dest = os.path.join(root, "flutter_app", "assets", "fonts")
os.makedirs(dest, exist_ok=True)
for name in [f"IBMPlexSansArabic-{w}.ttf" for w in WEIGHTS] + ["OFL.txt"]:
    path = os.path.join(dest, name)
    if not os.path.exists(path):
        urllib.request.urlretrieve(BASE + name, path)
    assert os.path.getsize(path) > 1000, name

pub = os.path.join(root, "flutter_app", "pubspec.yaml")
s = open(pub, encoding="utf-8").read()
if "IBMPlexSansArabic" not in s:
    fonts = "  fonts:\n    - family: IBMPlexSansArabic\n      fonts:\n" + "".join(
        f"        - asset: assets/fonts/IBMPlexSansArabic-{w}.ttf\n          weight: {n}\n" for w, n in WEIGHTS.items())
    s = s.replace("  assets:\n    - assets/qbank.json.gz\n", "  assets:\n    - assets/qbank.json.gz\n" + fonts, 1)
    open(pub, "w", encoding="utf-8").write(s)
print("fonts ready")
