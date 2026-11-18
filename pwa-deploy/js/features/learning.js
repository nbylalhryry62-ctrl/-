/* Learning engine: per-question statistics, spaced review (Leitner boxes), daily goal, streak and badges.
   Everything is derived from Aden.store.state; nothing here touches the DOM except toast notifications. */
Aden.learning = (() => {
  const { state } = Aden.store;
  const DAY = 864e5;
  const GAP_DAYS = [0, 1, 2, 4, 8, 16]; // days until the next review, by box (a wrong answer goes back to box 1)

  const dayKey = (ts) => {
    const d = new Date(ts);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
  };

  /* Record final answers: list of [questionId, wasCorrect]. One save for the whole batch. */
  const recordMany = (list) => {
    if (!list.length) return [];
    const now = Date.now();
    const stats = { ...state.stats };
    list.forEach(([id, ok]) => {
      const s = { a: 0, c: 0, b: 0, ...stats[id] };
      s.a++; if (ok) s.c++;
      s.b = ok ? Math.min(5, s.b + 1) : 1;
      s.t = now;
      s.d = now + GAP_DAYS[s.b] * DAY;
      stats[id] = s;
    });
    const key = dayKey(now);
    Aden.store.set("stats", stats);
    Aden.store.set("daily", { k: key, n: (state.daily.k === key ? state.daily.n : 0) + list.length });
    if (!state.days.includes(key)) Aden.store.set("days", [...state.days, key].slice(-400));
    return check();
  };

  const todayCount = () => (state.daily.k === dayKey(Date.now()) ? state.daily.n : 0);

  const streak = () => {
    const days = new Set(state.days);
    let t = Date.now();
    if (!days.has(dayKey(t))) t -= DAY; // today not studied yet: the streak is still alive until midnight
    let n = 0;
    while (days.has(dayKey(t))) { n++; t -= DAY; }
    return n;
  };

  const totals = () => {
    const rows = Object.values(state.stats);
    return {
      answered: rows.reduce((s, r) => s + r.a, 0),
      correct: rows.reduce((s, r) => s + r.c, 0),
      mastered: rows.filter((r) => r.b >= 5).length,
    };
  };

  /* Questions whose review date has arrived, oldest first. */
  const due = (limit = 20) => Object.entries(state.stats)
    .filter(([id, s]) => s.d <= Date.now() && Aden.bank.byId(id))
    .sort((a, b) => a[1].d - b[1].d).slice(0, limit).map(([id]) => id);
  const dueCount = () => Object.entries(state.stats).filter(([id, s]) => s.d <= Date.now() && Aden.bank.byId(id)).length;

  /* Accuracy per subject (only subjects with at least 3 answers), weakest first. */
  const mastery = () => {
    const by = new Map();
    Object.entries(state.stats).forEach(([id, s]) => {
      const q = Aden.bank.byId(id);
      if (!q) return;
      const key = q.fid + "|" + q.sid;
      const row = by.get(key) || { fid: q.fid, sid: q.sid, name: q.sname, a: 0, c: 0 };
      row.a += s.a; row.c += s.c;
      by.set(key, row);
    });
    return [...by.values()].filter((r) => r.a >= 3).map((r) => ({ ...r, acc: Math.round((r.c / r.a) * 100) })).sort((x, y) => x.acc - y.acc);
  };

  const BADGES = [
    { id: "first", name: "البداية", text: "أجبت عن أول سؤال", icon: "play", ok: (s) => s.answered >= 1 },
    { id: "exam", name: "أول اختبار", text: "أنهيت اختبارًا كاملًا", icon: "exam", ok: (s) => s.exams >= 1 },
    { id: "q100", name: "مئة سؤال", text: "أجبت عن 100 سؤال", icon: "target", ok: (s) => s.answered >= 100 },
    { id: "q500", name: "خمسمئة سؤال", text: "أجبت عن 500 سؤال", icon: "db", ok: (s) => s.answered >= 500 },
    { id: "goal", name: "هدف اليوم", text: "حققت هدفك اليومي", icon: "checkcircle", ok: (s) => s.today >= state.goal },
    { id: "streak3", name: "ثلاثة أيام", text: "ذاكرت 3 أيام متتالية", icon: "bolt", ok: (s) => s.streak >= 3 },
    { id: "streak7", name: "أسبوع كامل", text: "ذاكرت 7 أيام متتالية", icon: "bolt", ok: (s) => s.streak >= 7 },
    { id: "perfect", name: "العلامة الكاملة", text: "100% في اختبار من 10 أسئلة فأكثر", icon: "trophy", ok: (s) => s.perfect },
    { id: "master", name: "إتقان", text: "أتقنت 20 سؤالًا في أعلى مستوى حفظ", icon: "layers", ok: (s) => s.mastered >= 20 },
    { id: "units", name: "مجتهد المنهج", text: "أنهيت 10 وحدات من المنهج", icon: "book", ok: (s) => s.units >= 10 },
  ];

  /* Unlock any badge whose condition is now true; announce each one. */
  const check = () => {
    const t = totals();
    const snap = {
      ...t, exams: state.history.length, today: todayCount(), streak: streak(), units: state.units.length,
      perfect: state.history.some((h) => h.pct === 100 && (h.n || 0) >= 10),
    };
    const fresh = BADGES.filter((b) => !state.badges[b.id] && b.ok(snap));
    if (fresh.length) {
      Aden.store.set("badges", { ...state.badges, ...Object.fromEntries(fresh.map((b) => [b.id, Date.now()])) });
      fresh.forEach((b) => Aden.ui?.toast(`شارة جديدة: ${b.name}`));
    }
    return fresh;
  };

  return { recordMany, todayCount, streak, totals, due, dueCount, mastery, check, BADGES, dayKey };
})();
