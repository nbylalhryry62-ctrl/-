/* Exam engine: builds a session, tracks answers and time, produces the result. No rendering here. */
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

  const stop = () => { clearInterval(timer); timer = null; };
  const tick = () => {
    if (!session) return;
    session.left -= 1;
    subs.forEach((fn) => fn(session));
    if (session.left <= 0) { Aden.ui.toast("انتهى الوقت"); submit(); }
  };

  const begin = (qs, title, minutes, instant) => {
    session = { title, qs, i: 0, answers: {}, flags: {}, left: minutes * 60, total: minutes * 60, instant, started: Date.now() };
    result = null;
    stop(); timer = setInterval(tick, 1000);
  };

  const start = () => {
    const list = Aden.util.shuffle(pool()).slice(0, cfg.count);
    if (!list.length) { Aden.ui.toast("لا توجد أسئلة بهذه الخيارات"); return false; }
    save();
    begin(list, faculty().title, cfg.minutes, cfg.instant);
    return true;
  };

  /* Practice exam made of the learner's own saved questions (mistakes or bookmarks). */
  const startFromIds = (ids, title) => {
    const list = Aden.util.shuffle(ids.map(Aden.bank.byId).filter(Boolean)).slice(0, 30);
    if (!list.length) return false;
    begin(list, title, Math.max(10, Math.ceil(list.length * 1.2)), true);
    return true;
  };

  const answer = (qid, idx) => {
    if (!session) return;
    if (session.instant && session.answers[qid] !== undefined) return;
    session.answers[qid] = idx;
  };
  const flag = (qid) => { session.flags[qid] = !session.flags[qid]; };
  const go = (i) => { session.i = Math.min(Math.max(i, 0), session.qs.length - 1); };

  const submit = () => {
    if (!session) return null;
    stop();
    const s = session;
    const by = {};
    const weak = new Set();
    let correct = 0, blank = 0;
    s.qs.forEach((q) => {
      const row = (by[q.sname] = by[q.sname] || { name: q.sname, total: 0, correct: 0 });
      row.total++;
      const a = s.answers[q.id];
      if (a === undefined) { blank++; weak.add(`${q.sname}: ${q.unit}`); }
      else if (a === q.ci) { correct++; row.correct++; Aden.store.remove("mistakes", q.id); }
      else { Aden.store.add("mistakes", q.id); weak.add(`${q.sname}: ${q.unit}`); }
    });
    const pct = Aden.util.pct(correct, s.qs.length);
    result = {
      title: s.title, pct, correct, blank, wrong: s.qs.length - correct - blank, total: s.qs.length,
      secs: Math.round((Date.now() - s.started) / 1000), breakdown: Object.values(by), weak: [...weak].slice(0, 10),
      qs: s.qs, answers: s.answers, minutes: s.total / 60, instant: s.instant,
    };
    Aden.store.pushHistory({ at: Date.now(), title: s.title, pct, score: `${correct}/${s.qs.length}` });
    session = null;
    return result;
  };

  const abandon = () => { stop(); session = null; };
  const reset = () => { result = null; };

  return {
    cfg, save, defaults, faculty, track, pool, start, startFromIds, answer, flag, go, submit, abandon, reset,
    get session() { return session; }, get result() { return result; },
    active: () => !!session,
    subscribe: (fn) => (subs.add(fn), () => subs.delete(fn)),
    set(patch) { Object.assign(cfg, patch); },
  };
})();
