#!/usr/bin/env python3
"""Validate the question bank and every derived artifact. Exit code 1 on any failure.

Usage: python3 scripts/validate_bank.py
"""
import base64, csv, glob, json, os, re, sqlite3, sys, zlib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
P = lambda *a: os.path.join(ROOT, *a)
errors = []


def check(ok, msg):
    if not ok:
        errors.append(msg)
    print(("PASS " if ok else "FAIL ") + msg)


full_path = glob.glob(P("data", "qbank_full_*.json"))[0]
qs = json.load(open(full_path, encoding="utf-8"))
n = len(qs)
check(str(n) in os.path.basename(full_path), f"file name carries the real count ({n})")
check(len({q["id"] for q in qs}) == n, "question ids are unique")
check(all(len(set(q["options"])) == len(q["options"]) for q in qs), "no question has duplicate options")
check(all(0 <= q["correctIndex"] < len(q["options"]) and q["options"][q["correctIndex"]] == q["correctAnswerText"] for q in qs),
      "correctIndex matches correctAnswerText")
check(all(q["explanation"].strip() for q in qs), "every question has an explanation")
check(all(q["reviewStatus"] in ("ok", "fill") for q in qs), "no unresolved review status inside the bank")

fac = sum(len(json.load(open(f, encoding="utf-8"))) for f in glob.glob(P("data", "faculties", "*.json")))
check(fac == n, f"faculty files add up to {n} (found {fac})")
rows = sum(sum(1 for _ in csv.reader(open(f, encoding="utf-8-sig"))) - 1 for f in glob.glob(P("csv", "*.csv")))
check(rows == n, f"CSV files add up to {n} (found {rows})")

smap = json.load(open(glob.glob(P("data", "syllabus_map_*.json"))[0], encoding="utf-8"))
check(sum(x["count"] for x in smap) == n, f"syllabus map counts add up to {n}")

db_files = glob.glob(P("database", "*.sqlite"))
if db_files:
    db = sqlite3.connect(db_files[0])
    check(db.execute("select count(*) from questions").fetchone()[0] == n, "SQLite holds the same number of questions")
    check(db.execute("pragma integrity_check").fetchone()[0] == "ok", "SQLite integrity check")

js = open(P("pwa-deploy", "js", "data", "qbank-data.js"), encoding="utf-8").read()
blob = re.search(r'Aden\.qbankB64 = "([^"]+)"', js).group(1)
emb = json.loads(zlib.decompress(base64.b64decode(blob), -15))
check({r[0] for r in emb["q"]} == {q["id"] for q in qs}, "pwa-deploy/js/data/qbank-data.js has the same ids (run scripts/build_data.py if not)")
check(all(len(set(r[13])) == len(r[13]) for r in emb["q"]), "compiled bank has no duplicate options")

sys.exit(1 if errors else 0)
