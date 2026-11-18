/* Exam engine: builds a session, tracks answers and time, produces the result. No rendering here.
   The running session is saved after every change, so a reload (or a killed browser tab) resumes it. */
Aden.exam = (() => {
  const S = Aden.storage;
  let session = null;   // running exam
  let result = null;    // last finished exam
  let timer = null;
  const subs = new Set();

  const defaults = () => ({ fid: Aden.store.state.faculty, tid: "", sid: "all", diff: "all", count: 20, minutes: 25, instant: true });
  const cfg = { ...defaults(), ...S.get("examcfg", {}) };
  const save = () => S.set("examcfg", cfg);

  const faculty = () => Aden.progress.faculty(cfg.fid);
  const track = () => faculty().tracks.find((t) => t.id === cfg.tid) || faculty().tracks[0];
  const pool = () => Aden.bank.query({ fid: cfg.fid, tid: track().id, sid: cfg.sid, diff: cfg.diff });

  const persist = () => S.set("session", session && {
    ids: session.qs.map((q) => q.id), i: session.i, answers: session.answers, flags: session.flags,
    endsAt: session.endsAt, total: session.total, instant: session.instant, title: session.title, started: session.started,
  });

  const stop = () => { clearInterval(timer); timer = null; };
  /* The clock is derived from the end time, not counted tick by tick, so a throttled background tab cannot lose or gain time. */
  const tick = () => {
    if (!session) return;
    session.left = Math.max(0, Math.round((session.endsAt - Date.now()) / 1000));
    subs.forEach((fn) => fn(session));
    if (session.left <= 0) { Aden.ui.toast("انتهى الوقت"); submit(); Aden.router.refresh(); }
  };
  const run = () => { stop(); timer = setInterval(tick, 1000); };
  document.addEventListener("visibilitychange", () => { if (!document.hidden) tick(); });

  const begin = (qs, title, minutes, instant) => {
    const now = Date.now();
    session = { title, qs, i: 0, answers: {}, flags: {}, total: minutes * 60, left: minutes * 60, endsAt: now + minutes * 60000, instant, started: now };
    result = null;
    persist(); run();
  };

  const start = () => {
    const list = Aden.util.shuffle(pool()).slice(0, cfg.count);
    if (!list.length) { Aden.ui.toast("لا توجد أسئلة بهذه الخيارات"); return false; }
    save();
    begin(list, faculty().title, cfg.minutes, cfg.instant);
    return true;
  };

  /* Practice exam made of chosen questions (mistakes, bookmarks, due reviews, a weak subject). */
  const startFromIds = (ids, title, max = 30) => {
    const list = Aden.util.shuffle(ids.map(Aden.bank.byId).filter(Boolean)).slice(0, max);
    if (!list.length) return false;
    begin(list, title, Math.max(10, Math.ceil(list.length * 1.2)), true);
    return true;
  };

  /* Bring back an exam that was running when the page closed. Returns "running", "finished" or false. */
  const restore = () => {
    const d = S.get("session", null);
    if (!d) return false;
    const qs = d.ids.map(Aden.bank.byId).filter(Boolean);
    if (!qs.length) { S.set("session", null); return false; }
    session = { title: d.title, qs, i: Math.min(d.i, qs.length - 1), answers: d.answers, flags: d.flags, total: d.total, endsAt: d.endsAt, instant: d.instant, started: d.started,
      left: Math.max(0, Math.round((d.endsAt - Date.now()) / 1000)) };
    if (session.left <= 0) { submit(); return "finished"; }
    run();
    return "running";
  };

  const answer = (qid, idx) => {
    if (!session) return;
    if (session.instant && session.answers[qid] !== undefined) return;
    session.answers[qid] = idx;
    persist();
  };
  const flag = (qid) => { session.flags[qid] = !session.flags[qid]; persist(); };
  const go = (i) => { session.i = Math.min(Math.max(i, 0), session.qs.length - 1); persist(); };

  const submit = () => {
    if (!session) return null;
    stop();
    const s = session;
    const by = {};
    const weak = new Set();
    const records = [];
    let correct = 0, blank = 0;
    s.qs.forEach((q) => {
      const row = (by[q.sname] = by[q.sname] || { name: q.sname, total: 0, correct: 0 });
      row.total++;
      const a = s.answers[q.id];
      if (a === undefined) { blank++; weak.add(`${q.sname}: ${q.unit}`); return; }
      records.push([q.id, a === q.ci]);
      if (a === q.ci) { correct++; row.correct++; Aden.store.remove("mistakes", q.id); }
      else { Aden.store.add("mistakes", q.id); weak.add(`${q.sname}: ${q.unit}`); }
    });
    const pct = Aden.util.pct(correct, s.qs.length);
    result = {
      title: s.title, pct, correct, blank, wrong: s.qs.length - correct - blank, total: s.qs.length,
      secs: Math.round((Date.now() - s.started) / 1000), breakdown: Object.values(by), weak: [...weak].slice(0, 10),
      qs: s.qs, answers: s.answers, minutes: s.total / 60, instant: s.instant,
    };
    Aden.store.pushHistory({ at: Date.now(), title: s.title, pct, n: s.qs.length, score: `${correct}/${s.qs.length}` });
    session = null;
    persist();
    Aden.learning.recordMany(records);
    Aden.learning.check();
    return result;
  };

  const reset = () => { result = null; };

  return {
    cfg, save, defaults, faculty, track, pool, start, startFromIds, restore, answer, flag, go, submit, reset,
    get session() { return session; }, get result() { return result; },
    active: () => !!session,
    subscribe: (fn) => (subs.add(fn), () => subs.delete(fn)),
    set(patch) { Object.assign(cfg, patch); },
  };
})();
