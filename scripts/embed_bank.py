#!/usr/bin/env python3
"""Rebuild the compressed question bank embedded in pwa-deploy/index.html from data/*.json.

Edit the bank in data/ first, then run this script and scripts/validate_bank.py.
The embedded format is {"m": [...], "q": [...], "smap": [...]}; this script only swaps
question rows, options, explanations and the syllabus map, keeping the metadata table.
"""
import base64, glob, json, os, re, zlib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
html_path = os.path.join(ROOT, "pwa-deploy", "index.html")
qs = {q["id"]: q for q in json.load(open(glob.glob(os.path.join(ROOT, "data", "qbank_full_*.json"))[0], encoding="utf-8"))}
smap = json.load(open(glob.glob(os.path.join(ROOT, "data", "syllabus_map_*.json"))[0], encoding="utf-8"))
html = open(html_path, encoding="utf-8").read()
m = re.search(r'COMPRESSED_QBANK_B64 = "([^"]+)"', html)
emb = json.loads(zlib.decompress(base64.b64decode(m.group(1)), -15))
rows = []
for r in emb["q"]:
    q = qs.get(r[0])
    if q is None:
        continue
    r[13], r[14], r[15] = q["options"], q["correctIndex"], q["explanation"]
    r[11] = emb["m"].index(q["reviewStatus"])
    rows.append(r)
missing = set(qs) - {r[0] for r in rows}
if missing:
    raise SystemExit(f"{len(missing)} questions in data/ are not in the embedded bank (new questions need a full re-encode)")
emb["q"], emb["smap"] = rows, smap
blob = base64.b64encode(zlib.compress(json.dumps(emb, ensure_ascii=False, separators=(",", ":")).encode(), 9)[2:-4]).decode()
open(html_path, "w", encoding="utf-8").write(html[:m.start(1)] + blob + html[m.end(1):])
print(f"embedded {len(rows)} questions")
