Aden.views = Aden.views || {};
Aden.views.home = (() => {
  const { esc, fmt, pct } = Aden.util;

  const facultyCard = (f) => {
    const r = Aden.progress.readiness(f.id);
    const mine = Aden.store.state.faculty === f.id;
    const color = Aden.theme.color(f.id);
    const subjects = f.tracks.flatMap((t) => t.subjects).length;
    return `<article class="card fac" style="--fc:${color}">
      <div class="fac-top">
        <div class="fac-icon">${I(f.icon)}</div>
        <div class="grow" style="min-width:0"><h3>${esc(f.title)}</h3><p>${esc(f.subtitle)}</p></div>
        ${mine ? '<span class="chip good">كليتي</span>' : ""}
      </div>
      <div class="fac-meta"><span class="num">${fmt(Aden.bank.count(f.id))} سؤال • ${subjects} مواد</span><span class="num">${r.done}/${r.total} وحدة</span></div>
      ${Aden.ui.bar(r.pct, color)}
      <div class="fac-foot">
        <button class="btn btn-ghost btn-sm" data-act="home.syllabus" data-fid="${f.id}">${I("book")}المنهج</button>
        <button class="btn btn-primary btn-sm" data-act="home.exam" data-fid="${f.id}" style="background:${color};color:#fff">${I("play")}ابدأ اختبارًا</button>
      </div>
    </article>`;
  };

  const history = () => {
    const h = Aden.store.state.history.slice(0, 6);
    if (!h.length) return Aden.ui.empty("trophy", "لم تبدأ أي اختبار بعد", "ابدأ اختبارًا قصيرًا وستظهر نتائجك هنا مع المواد التي تحتاج مراجعة.");
    return `<div class="card history">${h.map((x) => `<div class="item">
      ${Aden.ui.pctChip(x.pct)}
      <div class="grow"><b>${esc(x.title)}</b><small class="num">${new Date(x.at).toLocaleDateString("ar-YE")} • ${esc(x.score)}</small></div>
    </div>`).join("")}</div>`;
  };

  const render = () => {
    const st = Aden.store.state;
    const fac = Aden.progress.faculty(st.faculty);
    const r = Aden.progress.readiness(st.faculty);
    const s = Aden.progress.summary();
    return `
    <section class="hero">
      <div class="card hero-main">
        <span class="eyebrow">امتحان القبول 2026 / 2027</span>
        <h1>جهّز نفسك لجامعة عدن بخطوات واضحة</h1>
        <p>${fmt(Aden.bank.all.length)} سؤالًا موزعة على ${Aden.syllabus.length} كليات، مع تصحيح فوري وتعليل لكل إجابة، ودفتر يجمع أخطاءك لتعود إليها.</p>
        <div class="hero-actions">
          <button class="btn btn-primary" data-act="home.quick">${I("bolt")}اختبار سريع (10 أسئلة)</button>
          <button class="btn btn-ghost" data-act="home.bank">${I("db")}تصفّح البنك</button>
        </div>
      </div>
      <div class="card hero-side">
        <span class="eyebrow">جاهزيتك في ${esc(fac.title)}</span>
        <div class="row">
          ${Aden.ui.ring(r.pct, { size: 116, color: Aden.theme.color(fac.id) })}
          <div><b class="num" style="font-size:20px">${r.done} / ${r.total}</b><p style="color:var(--muted)">وحدة أنهيتها من المنهج</p></div>
        </div>
        <button class="btn btn-ghost btn-sm" data-act="home.syllabus" data-fid="${fac.id}">متابعة المنهج</button>
      </div>
    </section>
    <section class="stats">
      <div class="card stat"><b class="num">${s.exams}</b><span>اختبارات منجزة</span></div>
      <div class="card stat"><b class="num">${s.exams ? s.avg + "%" : "-"}</b><span>متوسط النتائج</span></div>
      <div class="card stat"><b class="num">${s.mistakes}</b><span>أسئلة للمراجعة</span></div>
      <div class="card stat"><b class="num">${s.saved}</b><span>أسئلة محفوظة</span></div>
    </section>
    <section>
      <div class="section-title"><h2>الكليات</h2><span class="eyebrow">اختر كليتك لتحديد خطة المذاكرة</span></div>
      <div class="faculties">${Aden.syllabus.map(facultyCard).join("")}</div>
    </section>
    <section><div class="section-title"><h2>آخر النتائج</h2></div>${history()}</section>`;
  };

  Aden.actions.register({
    "home.syllabus": (el) => { Aden.store.set("faculty", el.dataset.fid); Aden.router.go("syllabus"); },
    "home.bank": () => Aden.router.go("bank"),
    "home.exam": (el) => {
      Aden.store.set("faculty", el.dataset.fid);
      Aden.exam.set({ fid: el.dataset.fid, tid: "", sid: "all", diff: "all" });
      Aden.exam.reset();
      Aden.router.go("exam");
    },
    "home.quick": () => {
      Aden.exam.set({ fid: Aden.store.state.faculty, tid: "", sid: "all", diff: "all", count: 10, minutes: 12 });
      if (Aden.exam.start()) Aden.router.go("exam");
    },
  });
  return { render };
})();
Aden.router.register("home", Aden.views.home);
