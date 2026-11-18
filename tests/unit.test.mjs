// Unit tests for the app logic. Loads the real scripts into a sandbox with a tiny DOM stub; no browser needed.
import { test } from "node:test";
import assert from "node:assert/strict";
import vm from "node:vm";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const WEB = path.join(path.dirname(fileURLToPath(import.meta.url)), "..", "pwa-deploy");
const store = {};
const noop = () => {};
const el = () => ({ addEventListener: noop, appendChild: noop, remove: noop, classList: { toggle: noop, add: noop }, querySelector: () => null, querySelectorAll: () => [] });
const sandbox = {
  console, Blob, Response, DecompressionStream, atob, URL, setTimeout, clearTimeout, setInterval, clearInterval,
  localStorage: { getItem: (k) => (k in store ? store[k] : null), setItem: (k, v) => { store[k] = String(v); }, removeItem: (k) => delete store[k] },
  document: { ...el(), getElementById: () => el(), createElement: el, body: el(), documentElement: el(), querySelector: () => null, querySelectorAll: () => [], hidden: false },
  matchMedia: () => ({ matches: false, addEventListener: noop }),
  location: { hash: "", protocol: "http:" },
  navigator: {}, CSS: { escape: (s) => s },
};
sandbox.window = sandbox;
sandbox.addEventListener = noop;
vm.createContext(sandbox);
const SCRIPTS = ["data/syllabus", "data/qbank-data", "core/util", "core/storage", "core/store", "core/actions", "core/router", "ui/icons", "ui/theme",
  "ui/mathtext", "ui/components", "data/bank", "features/progress", "features/learning", "features/exam"];
for (const s of SCRIPTS) vm.runInContext(fs.readFileSync(path.join(WEB, "js", s + ".js"), "utf8"), sandbox, { filename: s });
const A = vm.runInContext("Aden", sandbox);
await A.bank.load();

test("bank loads every question with a valid shape", () => {
  assert.equal(A.bank.all.length, A.qbankCount);
  for (const q of A.bank.all) {
    assert.ok(q.opts.length >= 2, q.id);
    assert.equal(new Set(q.opts).size, q.opts.length, `duplicate options in ${q.id}`);
    assert.ok(q.ci >= 0 && q.ci < q.opts.length, q.id);
  }
});

test("every faculty has questions and its subjects resolve", () => {
  for (const f of A.syllabus) {
    assert.ok(A.bank.count(f.id) > 0, f.id);
    for (const t of f.tracks) for (const s of t.subjects) {
      assert.ok(A.bank.query({ fid: f.id, tid: t.id, sid: s.id }).length > 0, `${f.id}/${t.id}/${s.id} has no questions`);
    }
  }
});

test("search and filters combine", () => {
  const all = A.bank.query({});
  const sub = A.bank.query({ fid: "law" });
  assert.ok(sub.length < all.length && sub.every((q) => q.fid === "law"));
  assert.ok(A.bank.query({ search: sub[0].id }).some((q) => q.id === sub[0].id));
  assert.equal(A.bank.query({ search: "zzzz-no-such-text" }).length, 0);
});

test("math text escapes HTML and renders $...$", () => {
  assert.equal(A.mathText("<b>x</b>"), "&lt;b&gt;x&lt;/b&gt;");
  assert.match(A.mathText("قيمة $x^2$"), /<span class="math">x²<\/span>/);
});

test("exam scores a session and records history, mistakes and review stats", () => {
  A.exam.set({ fid: "law", tid: "", sid: "all", diff: "all", count: 10, minutes: 10, instant: false });
  assert.ok(A.exam.start());
  const s = A.exam.session;
  assert.equal(s.qs.length, 10);
  s.qs.forEach((q, i) => A.exam.answer(q.id, i < 6 ? q.ci : (q.ci + 1) % q.opts.length)); // 6 right, 4 wrong
  const r = A.exam.submit();
  assert.equal(r.correct, 6); assert.equal(r.wrong, 4); assert.equal(r.blank, 0); assert.equal(r.pct, 60);
  assert.equal(A.store.state.history[0].pct, 60);
  assert.equal(A.store.state.mistakes.length, 4);
  assert.equal(A.exam.session, null);
  assert.equal(Object.keys(A.store.state.stats).length, 10);
  assert.equal(A.learning.todayCount(), 10);
});

test("unanswered questions count as blank, not wrong", () => {
  A.exam.set({ count: 10 });
  A.exam.start();
  const q = A.exam.session.qs[0];
  A.exam.answer(q.id, q.ci);
  const r = A.exam.submit();
  assert.equal(r.correct, 1); assert.equal(r.blank, 9); assert.equal(r.wrong, 0);
});

test("a running exam survives a reload and keeps its answers", () => {
  A.exam.start();
  const s = A.exam.session;
  A.exam.answer(s.qs[0].id, 1); A.exam.flag(s.qs[1].id); A.exam.go(2);
  const ids = s.qs.map((q) => q.id);
  // simulate a page reload: forget the in-memory session, then restore from storage
  assert.equal(A.exam.restore(), "running");
  const back = A.exam.session;
  assert.deepEqual(back.qs.map((q) => q.id), ids);
  assert.equal(back.answers[ids[0]], 1); assert.equal(back.flags[ids[1]], true); assert.equal(back.i, 2);
  assert.ok(back.left > 0);
  A.exam.submit();
});

test("an exam that ran out of time while closed is finished on restore", () => {
  A.exam.start();
  const saved = JSON.parse(store["aden.v2.session"]);
  saved.endsAt = Date.now() - 1000;
  store["aden.v2.session"] = JSON.stringify(saved);
  assert.equal(A.exam.restore(), "finished");
  assert.ok(A.exam.result);
});

test("Leitner boxes: right answers climb, a wrong answer resets to box 1", () => {
  const id = A.bank.all[100].id;
  const box = () => A.store.state.stats[id].b;
  A.learning.recordMany([[id, true]]); assert.equal(box(), 1);
  A.learning.recordMany([[id, true]]); assert.equal(box(), 2);
  A.learning.recordMany([[id, false]]); assert.equal(box(), 1);
  const s = A.store.state.stats[id];
  assert.ok(s.d > Date.now(), "next review is in the future");
  A.store.state.stats[id].d = Date.now() - 1;
  assert.ok(A.learning.due(50).includes(id), "an overdue question is due");
});

test("streak counts consecutive days and survives a missed today", () => {
  const day = 864e5;
  const key = (ago) => A.learning.dayKey(Date.now() - ago * day);
  A.store.set("days", [key(3), key(2), key(1)]);
  assert.equal(A.learning.streak(), 3);          // today not studied yet, yesterday still counts
  A.store.set("days", [key(3), key(2), key(1), key(0)]);
  assert.equal(A.learning.streak(), 4);
  A.store.set("days", [key(5), key(4), key(0)]);
  assert.equal(A.learning.streak(), 1);          // a gap breaks it
});

test("badges unlock once", () => {
  const first = A.learning.check();
  assert.ok(A.store.state.badges.first);
  assert.equal(A.learning.check().length, 0);
  assert.ok(Array.isArray(first));
});

test("util helpers", () => {
  assert.equal(A.util.clock(125), "02:05");
  assert.equal(A.util.pct(1, 3), 33);
  assert.equal(A.util.pct(1, 0), 0);
  assert.deepEqual(A.util.toggleIn(["a"], "a"), []);
});
