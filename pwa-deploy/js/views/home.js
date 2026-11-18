Aden.views = Aden.views || {};
Aden.views.home = (() => {
  const { esc, fmt } = Aden.util;
  const L = Aden.learning;

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

  const today = () => {
    const goal = Aden.store.state.goal;
    const n = L.todayCount();
    const p = Math.min(100, Aden.util.pct(n, goal));
    const due = L.dueCount();
    const streak = L.streak();
    const s = Aden.progress.summary();
    return `<section class="stats">
      <div class="card stat"><span class="stat-ico gold">${I("bolt")}</span><b class="num">${streak}</b><span>${streak === 1 ? "يوم متتالٍ" : "أيام متتالية"}</span></div>
      <div class="card stat"><span class="stat-ico">${I("target")}</span><b class="num">${n}<small>/${goal}</small></b><span>هدف اليوم</span>${Aden.ui.bar(p)}
        <button class="stat-link" data-act="home.goal">تغيير الهدف</button></div>
      <div class="card stat"><span class="stat-ico">${I("retry")}</span><b class="num">${due}</b><span>أسئلة حان موعد مراجعتها</span>
        <button class="btn btn-primary btn-sm" data-act="home.review" ${due ? "" : "disabled"}>راجع الآن</button></div>
      <div class="card stat"><span class="stat-ico">${I("trophy")}</span><b class="num">${s.exams ? s.avg + "%" : "-"}</b><span>متوسط ${s.exams} اختبار</span></div>
    </section>`;
  };

  const analytics = () => {
    const h = Aden.store.state.history.slice(0, 10).reverse().map((x) => ({ pct: x.pct, label: x.title }));
    const trend = h.length >= 2
      ? Aden.charts.trend(h)
      : `<div class="empty">${I("trophy")}<p>أنهِ اختبارين على الأقل ليظهر منحنى تطور نتائجك.</p></div>`;
    const m = L.mastery();
    const weak = m[0];
    const rows = m.slice(0, 5).map((r) => `<div class="mrow"><div><span>${esc(r.name)}</span><b class="num">${r.acc}%</b></div>${Aden.ui.bar(r.acc, r.acc >= 70 ? "var(--good)" : r.acc >= 50 ? "var(--gold)" : "var(--bad)")}</div>`).join("");
    return `<section class="two analytics">
      <div class="card card-pad"><div class="section-title"><h2>تطور نتائجك</h2><span class="eyebrow">آخر ${h.length || 0} اختبارات</span></div>${trend}</div>
      <div class="card card-pad"><div class="section-title"><h2>مستواك حسب المادة</h2><span class="eyebrow">الأضعف أولًا</span></div>
        ${rows || `<div class="empty">${I("target")}<p>أجب عن بعض الأسئلة ليظهر مستواك في كل مادة.</p></div>`}
        ${weak && weak.acc < 85 ? `<button class="btn btn-ghost btn-sm" style="margin-top:14px" data-act="home.weak" data-fid="${weak.fid}" data-sid="${esc(weak.sid)}" data-name="${esc(weak.name)}">${I("play")}تدرّب على ${esc(weak.name)}</button>` : ""}
      </div></section>`;
  };

  const badges = () => `<section><div class="section-title"><h2>الإنجازات</h2><span class="eyebrow num">${Object.keys(Aden.store.state.badges).length} من ${L.BADGES.length}</span></div>
    <div class="badges">${L.BADGES.map((b) => {
      const on = Aden.store.state.badges[b.id];
      return `<div class="card badge-card${on ? " on" : ""}"><span class="badge-ico">${I(b.icon)}</span><div><b>${esc(b.name)}</b><small>${esc(b.text)}</small></div></div>`;
    }).join("")}</div></section>`;

  const render = () => {
    const st = Aden.store.state;
    const fac = Aden.progress.faculty(st.faculty);
    const r = Aden.progress.readiness(st.faculty);
    const resume = Aden.exam.active();
    return `
    <section class="hero">
      <div class="card hero-main">
        <span class="eyebrow">امتحان القبول 2026 / 2027</span>
        <h1>جهّز نفسك لجامعة عدن بخطوات واضحة</h1>
        <p>${fmt(Aden.bank.all.length)} سؤالًا موزعة على ${Aden.syllabus.length} كليات، مع تصحيح فوري وتعليل لكل إجابة، ومراجعة متباعدة تعيد إليك ما نسيته في الوقت المناسب.</p>
        <div class="hero-actions">
          ${resume ? `<button class="btn btn-primary" data-act="home.resume">${I("play")}متابعة اختبارك الجاري</button>` : `<button class="btn btn-primary" data-act="home.quick">${I("bolt")}اختبار سريع (10 أسئلة)</button>`}
          <button class="btn btn-ghost" data-act="home.bank">${I("db")}تصفّح البنك</button>
          <button class="btn btn-ghost" data-act="search.show">${I("search")}بحث</button>
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
    ${today()}
    ${analytics()}
    <section>
      <div class="section-title"><h2>الكليات</h2><span class="eyebrow">اختر كليتك لتحديد خطة المذاكرة</span></div>
      <div class="faculties">${Aden.syllabus.map(facultyCard).join("")}</div>
    </section>
    ${badges()}
    <section><div class="section-title"><h2>آخر النتائج</h2></div>${history()}</section>`;
  };

  Aden.actions.register({
    "home.syllabus": (el) => { Aden.store.set("faculty", el.dataset.fid); Aden.router.go("syllabus"); },
    "home.bank": () => Aden.router.go("bank"),
    "home.resume": () => Aden.router.go("exam"),
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
    "home.review": () => { if (Aden.exam.startFromIds(L.due(20), "مراجعة اليوم", 20)) Aden.router.go("exam"); },
    "home.weak": (el) => {
      const ids = Aden.bank.query({ fid: el.dataset.fid, sid: el.dataset.sid }).map((q) => q.id);
      if (Aden.exam.startFromIds(ids, "تدريب: " + el.dataset.name, 15)) Aden.router.go("exam");
    },
    "home.goal": () => {
      const steps = [10, 20, 30, 50];
      const next = steps[(steps.indexOf(Aden.store.state.goal) + 1) % steps.length];
      Aden.store.set("goal", next);
      Aden.ui.toast(`هدفك اليومي ${next} سؤالًا`);
      Aden.router.refresh();
    },
  });
  return { render };
})();
Aden.router.register("home", Aden.views.home);
