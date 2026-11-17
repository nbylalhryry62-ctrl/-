Aden.views.exam = (() => {
  const { esc, fmt, clock } = Aden.util;
  const E = Aden.exam;
  let reviewFilter = "all";

  /* ---------- setup ---------- */
  const seg = (act, key, values, labelFn) => `<div class="seg" role="group">${values.map((v) =>
    `<button data-act="${act}" data-v="${v}" aria-pressed="${E.cfg[key] === v}">${labelFn(v)}</button>`).join("")}</div>`;

  const setup = () => {
    const c = E.cfg;
    const f = E.faculty();
    const t = E.track();
    const n = E.pool().length;
    const take = Math.min(c.count, n);
    const color = Aden.theme.color(f.id);
    const subjects = t.subjects;
    return `
    <div class="page-head"><div><h1>اختبار جديد</h1><p>اختر الكلية والمادة، وسنجهّز لك أسئلة عشوائية بتوقيت محدد.</p></div></div>
    <div class="setup">
      <div class="card setup-form">
        <div class="field"><span class="label">الكلية</span>
          <div class="pick">${Aden.syllabus.map((x) => `<button style="--fc:${Aden.theme.color(x.id)}" data-act="ex.faculty" data-fid="${x.id}" aria-pressed="${x.id === f.id}"><span class="fac-icon">${I(x.icon)}</span><span>${esc(x.title)}</span></button>`).join("")}</div></div>
        ${f.tracks.length > 1 ? `<div class="field"><span class="label">المسار</span><div class="seg">${f.tracks.map((x) => `<button data-act="ex.track" data-tid="${x.id}" aria-pressed="${x.id === t.id}">${esc(x.name)}</button>`).join("")}</div></div>` : ""}
        <div class="row2">
          <div class="field"><label for="ex-sid">المادة</label><select class="select" id="ex-sid" data-act="ex.subject">
            <option value="all">كل المواد</option>${subjects.map((s) => `<option value="${s.id}" ${c.sid === s.id ? "selected" : ""}>${esc(s.name)}</option>`).join("")}</select></div>
          <div class="field"><label for="ex-diff">الصعوبة</label><select class="select" id="ex-diff" data-act="ex.diff">
            <option value="all">الكل</option>${Aden.bank.difficulties().map((d) => `<option ${c.diff === d ? "selected" : ""}>${esc(d)}</option>`).join("")}</select></div>
        </div>
        <div class="row2">
          <div class="field"><span class="label">عدد الأسئلة</span>${seg("ex.count", "count", [10, 20, 30, 50], (v) => v)}</div>
          <div class="field"><span class="label">المدة بالدقائق</span>${seg("ex.minutes", "minutes", [12, 25, 45, 60], (v) => v)}</div>
        </div>
        <div class="toggle-row"><div><b>تصحيح فوري</b><small>يظهر الجواب والتعليل بعد كل سؤال، ولا يمكن تغيير الإجابة.</small></div>
          <button class="switch" role="switch" aria-checked="${c.instant}" data-act="ex.instant" aria-label="تصحيح فوري"></button></div>
      </div>
      <aside class="card summary" style="--fc:${color}">
        <span class="eyebrow">ملخص الاختبار</span>
        <dl class="num">
          <div><dt>الكلية</dt><dd>${esc(f.title)}</dd></div>
          <div><dt>الأسئلة المتاحة</dt><dd>${fmt(n)}</dd></div>
          <div><dt>أسئلة الاختبار</dt><dd>${take}</dd></div>
          <div><dt>المدة</dt><dd>${c.minutes} دقيقة</dd></div>
        </dl>
        <button class="btn btn-primary" style="background:${color};color:#fff" data-act="ex.start" ${n ? "" : "disabled"}>${I("play")}ابدأ الاختبار</button>
        ${n < c.count && n ? `<p class="eyebrow">المتاح أقل من العدد المطلوب، وسيُستخدم ${n} سؤالًا.</p>` : ""}
      </aside>
    </div>`;
  };

  /* ---------- running ---------- */
  const run = () => {
    const s = E.session;
    const q = s.qs[s.i];
    const picked = s.answers[q.id];
    const answered = Object.keys(s.answers).length;
    const showFb = s.instant && picked !== undefined;
    return `
    <div class="card run-bar">
      <b>${esc(s.title)}</b>
      <span class="num eyebrow">سؤال ${s.i + 1} من ${s.qs.length}</span>
      ${Aden.ui.bar(Aden.util.pct(answered, s.qs.length))}
      <span class="timer num${s.left <= 60 ? " low" : ""}" id="timer">${I("clock")}<span>${clock(s.left)}</span></span>
      <button class="btn btn-danger btn-sm" data-act="ex.finish">إنهاء الاختبار</button>
    </div>
    <div class="run">
      <div style="display:grid;gap:16px;min-width:0">
        <article class="card q">
          <div class="q-meta"><span class="chip dot" style="--fc:${Aden.theme.color(q.fid)}">${esc(q.sname)}</span><span class="chip">${esc(q.unit)}</span><span class="grow"></span>
            <button class="btn btn-quiet btn-sm ${s.flags[q.id] ? "on" : ""}" data-act="ex.flag" aria-pressed="${!!s.flags[q.id]}" style="${s.flags[q.id] ? "color:var(--gold)" : ""}">${I("flag")}${s.flags[q.id] ? "علّمت للمراجعة" : "علّم للمراجعة"}</button></div>
          <p class="q-text${q.lang === "en" ? " en" : ""}">${Aden.mathText(q.text)}</p>
          ${Aden.question.options(q, { picked, locked: s.instant, act: "ex.pick" })}
          ${showFb ? Aden.question.feedback(q, picked) : ""}
        </article>
        <div class="run-nav">
          <button class="btn btn-ghost" data-act="ex.go" data-i="${s.i - 1}" ${s.i === 0 ? "disabled" : ""}>${I("prev")}السابق</button>
          ${s.i === s.qs.length - 1
            ? `<button class="btn btn-primary" data-act="ex.finish">إنهاء وعرض النتيجة</button>`
            : `<button class="btn btn-primary" data-act="ex.go" data-i="${s.i + 1}">التالي${I("next")}</button>`}
        </div>
      </div>
      <aside class="card palette">
        <span class="eyebrow">خريطة الأسئلة</span>
        <div class="grid num">${s.qs.map((x, i) => `<button data-act="ex.go" data-i="${i}" class="${s.answers[x.id] !== undefined ? "done" : ""} ${i === s.i ? "cur" : ""} ${s.flags[x.id] ? "flag" : ""}" aria-label="سؤال ${i + 1}">${i + 1}</button>`).join("")}</div>
        <div class="legend"><span>مجاب: ${answered}</span><span>متبقٍ: ${s.qs.length - answered}</span></div>
      </aside>
    </div>`;
  };

  /* ---------- result ---------- */
  const message = (p) => (p >= 85 ? "أداء ممتاز" : p >= 70 ? "مستوى جيد جدًا" : p >= 50 ? "مستوى مقبول، وتحتاج مزيدًا من التدريب" : "تحتاج مراجعة المنهج ثم إعادة المحاولة");
  const reviewItem = (q, i, a) => {
    const ok = a === q.ci;
    return `<article class="card q"><div class="q-meta"><span class="chip num">${i + 1}</span><span class="chip dot" style="--fc:${Aden.theme.color(q.fid)}">${esc(q.sname)}</span>
      <span class="chip ${a === undefined ? "gold" : ok ? "good" : "bad"}">${a === undefined ? "بلا إجابة" : ok ? "صحيحة" : "خاطئة"}</span></div>
      <p class="q-text${q.lang === "en" ? " en" : ""}">${Aden.mathText(q.text)}</p>
      ${Aden.question.options(q, { picked: a, reveal: true })}${Aden.question.feedback(q, a)}</article>`;
  };
  const result = () => {
    const r = E.result;
    const color = r.pct >= 70 ? "var(--good)" : r.pct >= 50 ? "var(--gold)" : "var(--bad)";
    const items = r.qs.map((q, i) => [q, i, r.answers[q.id]]).filter(([q, , a]) => reviewFilter === "all" || a !== q.ci);
    return `
    <div class="card score">
      ${Aden.ui.ring(r.pct, { size: 150, th: 14, color })}
      <div><span class="eyebrow">${esc(r.title)}</span><h1>${message(r.pct)}</h1>
        <div class="score-nums num"><div><b style="color:var(--good)">${r.correct}</b><span>صحيحة</span></div><div><b style="color:var(--bad)">${r.wrong}</b><span>خاطئة</span></div><div><b>${r.blank}</b><span>بلا إجابة</span></div><div><b>${clock(r.secs)}</b><span>الوقت المستغرق</span></div></div>
        <div style="display:flex;gap:10px;flex-wrap:wrap;margin-top:18px">
          <button class="btn btn-primary" data-act="ex.again">${I("retry")}إعادة بنفس الإعدادات</button>
          <button class="btn btn-ghost" data-act="ex.new">اختبار جديد</button>
          <button class="btn btn-ghost" data-act="ex.notebook">${I("notebook")}دفتر الأخطاء</button>
        </div>
      </div>
    </div>
    <div class="two">
      <section class="card breakdown"><h3>النتيجة حسب المادة</h3>${r.breakdown.map((b) => { const p = Aden.util.pct(b.correct, b.total); return `<div class="row"><div><span>${esc(b.name)}</span><b class="num">${b.correct}/${b.total}</b></div>${Aden.ui.bar(p, p >= 70 ? "var(--good)" : p >= 50 ? "var(--gold)" : "var(--bad)")}</div>`; }).join("")}</section>
      <section class="card weak"><h3>محاور تحتاج مراجعة</h3>${r.weak.length ? `<ul>${r.weak.map((w) => `<li>${esc(w)}</li>`).join("")}</ul>` : `<p style="color:var(--muted)">لا توجد محاور ضعيفة. أحسنت.</p>`}</section>
    </div>
    <section><div class="section-title"><h2>مراجعة الإجابات</h2>
      <div class="seg"><button data-act="ex.filter" data-v="all" aria-pressed="${reviewFilter === "all"}">الكل</button><button data-act="ex.filter" data-v="wrong" aria-pressed="${reviewFilter === "wrong"}">الأخطاء فقط</button></div></div>
      <div class="qlist">${items.length ? items.map(([q, i, a]) => reviewItem(q, i, a)).join("") : Aden.ui.empty("trophy", "لا أخطاء", "أجبت عن كل الأسئلة بشكل صحيح.")}</div></section>`;
  };

  const render = () => (E.session ? run() : E.result ? result() : setup());

  /* live clock: only touches the timer element, never re-renders the question */
  E.subscribe((s) => {
    const el = document.getElementById("timer");
    if (!el) return;
    el.querySelector("span").textContent = clock(s.left);
    el.classList.toggle("low", s.left <= 60);
  });

  const again = () => Aden.router.refresh();
  const patch = (obj) => (el) => { E.set(obj(el)); E.save(); again(); };

  Aden.actions.register({
    "ex.faculty": patch((el) => ({ fid: el.dataset.fid, tid: "", sid: "all" })),
    "ex.track": patch((el) => ({ tid: el.dataset.tid, sid: "all" })),
    "ex.subject": patch((el) => ({ sid: el.value })),
    "ex.diff": patch((el) => ({ diff: el.value })),
    "ex.count": patch((el) => ({ count: Number(el.dataset.v) })),
    "ex.minutes": patch((el) => ({ minutes: Number(el.dataset.v) })),
    "ex.instant": () => { E.set({ instant: !E.cfg.instant }); E.save(); again(); },
    "ex.start": () => { E.reset(); if (E.start()) Aden.router.refresh(); },
    "ex.pick": (el) => { E.answer(el.dataset.qid, Number(el.dataset.opt)); again(); },
    "ex.flag": () => { E.flag(E.session.qs[E.session.i].id); again(); },
    "ex.go": (el) => { E.go(Number(el.dataset.i)); again(); window.scrollTo(0, 0); },
    "ex.finish": async () => {
      const s = E.session;
      const left = s.qs.length - Object.keys(s.answers).length;
      const ok = await Aden.ui.confirm({ title: "إنهاء الاختبار؟", text: left ? `بقي ${left} سؤالًا بلا إجابة وسيُحتسب كل منها خطأً.` : "ستظهر نتيجتك مع مراجعة الإجابات.", ok: "إنهاء وعرض النتيجة" });
      if (ok) { reviewFilter = "all"; E.submit(); again(); window.scrollTo(0, 0); }
    },
    "ex.filter": (el) => { reviewFilter = el.dataset.v; again(); },
    "ex.again": () => { if (E.start()) again(); window.scrollTo(0, 0); },
    "ex.new": () => { E.reset(); again(); },
    "ex.notebook": () => Aden.router.go("review"),
  });
  return { render };
})();
Aden.router.register("exam", Aden.views.exam);
