#!/usr/bin/env python3
"""Stamp pwa-deploy/sw.js with the file list and a content hash.

The service worker serves app files cache-first, so its VERSION must change whenever any app file changes,
otherwise returning visitors keep the old code. Run this after every change to pwa-deploy/ (CI runs it with --check).
"""
import hashlib, os, re, sys

WEB = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "pwa-deploy")
html = open(os.path.join(WEB, "index.html"), encoding="utf-8").read()
assets = re.findall(r'<script src="([^"]+)"', html) + [h for h in re.findall(r'<link rel="stylesheet" href="([^"]+)"', html) if not h.startswith("http")]
extra = ["manifest.json", "icons/icon-192.png", "icons/icon-512.png", "icons/icon-maskable-512.png", "icons/apple-touch-icon.png"]
files = ["index.html"] + extra + assets

digest = hashlib.sha1()
for f in files:
    digest.update(f.encode())
    digest.update(open(os.path.join(WEB, f), "rb").read())
version = "v" + digest.hexdigest()[:10]

precache = "var PRECACHE = ['./', " + ", ".join(f"'./{f}'" for f in files) + "];"
path = os.path.join(WEB, "sw.js")
old = open(path, encoding="utf-8").read()
new = re.sub(r"var VERSION = '[^']*';", f"var VERSION = '{version}';", old, count=1)
new = re.sub(r"var PRECACHE = \[.*?\];", lambda m: precache, new, count=1, flags=re.S)

if "--check" in sys.argv:
    if new != old:
        sys.exit("sw.js is stale: run python3 scripts/build_sw.py")
    print("sw.js is up to date", version)
else:
    open(path, "w", encoding="utf-8").write(new)
    print("sw.js stamped", version, f"({len(files)} files)")
