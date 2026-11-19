#!/usr/bin/env python3
"""Compile data/qbank_full_*.json into flutter_app/assets/qbank.json.gz (compact keys)."""
import glob, gzip, json, os
root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
src = glob.glob(os.path.join(root, "data", "qbank_full_*.json"))[0]
qs = json.load(open(src, encoding="utf-8"))
rows = [{"id": q["id"], "f": q["facultyId"], "ft": q["facultyTitle"], "s": q["subjectName"], "u": q["unit"],
         "d": q["difficulty"], "l": q["lang"], "q": q["question"], "o": q["options"], "c": q["correctIndex"],
         "e": q.get("explanation", "")} for q in qs]
out = os.path.join(root, "flutter_app", "assets", "qbank.json.gz")
with gzip.GzipFile(out, "wb", mtime=0) as f:
    f.write(json.dumps(rows, ensure_ascii=False, separators=(",", ":")).encode("utf-8"))
print(len(rows), "questions ->", out, os.path.getsize(out) // 1024, "KB")
