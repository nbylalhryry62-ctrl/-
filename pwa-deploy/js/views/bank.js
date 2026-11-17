Aden.views.bank = (() => {
  const { esc, fmt, debounce } = Aden.util;
  const PAGE = 20;
  const defaults = () => ({ tab: "list", search: "", fid: "all", sid: "all", unit: "all", diff: "all", src: "all", mode: "interactive", page: 1 });
  const qb = defaults();

  const filtered = () => Aden.bank.query(qb);

  const results = () => {
    const list = filtered();
    const pages = Math.max(1, Math.ceil(list.length / PAGE));
    qb.page = Math.min(qb.page, pages);
    const from = (qb.page - 1) * PAGE;
    const slice = list.slice(from, from + PAGE);
    if (!list.length) return Aden.ui.empty("search", "لا توجد نتائج", "غيّر كلمة البحث أو خفّف المرشحات.", `<button class="btn btn-ghost" data-act="bank.reset">مسح المرشحات</button>`);
    return `<div class="results-meta num"><span>${fmt(list.length)} سؤال • عرض ${fmt(from + 1)} إلى ${fmt(from + slice.length)}</span></div>
      <div class="qlist">${slice.map((q, i) => Aden.question.card(q, { mode: qb.mode, index: from + i + 1 })).join("")}</div>
      ${Aden.ui.pager(qb.page, pages, "bank.page")}`;
  };

  const select = (key, label, options, cls = "") => `<div class="field ${cls}"><label for="f-${key}">${label}</label>
    <select class="select" id="f-${key}" data-act="bank.filter" data-key="${key}">${options.map(([v, t]) => `<option value="${esc(v)}" ${qb[key] === v ? "selected" : ""}>${esc(t)}</option>`).join("")}</select></div>`;

  const list = () => {
    const subjects = Aden.bank.subjectsOf(qb.fid);
    const units = qb.sid === "all" ? [] : Aden.bank.unitsOf(qb.fid, qb.sid);
    return `
    <div class="card filters">
      <div class="search">${I("search")}<input class="input" type="search" id="f-search" data-act="bank.search" value="${esc(qb.search)}" placeholder="ابحث في نص السؤال أو الخيارات أو التعليل" aria-label="بحث"></div>
      ${select("fid", "الكلية", [["all", "كل الكليات"], ...Aden.syllabus.map((f) => [f.id, f.title])])}
      ${select("sid", "المادة", [["all", "كل المواد"], ...subjects.map((s) => [s.id, s.name])])}
      ${select("unit", "الوحدة", [["all", qb.sid === "all" ? "اختر مادة أولًا" : "كل الوحدات"], ...units.map((u) => [u, u])])}
      ${select("diff", "الصعوبة", [["all", "الكل"], ...Aden.bank.difficulties().map((d) => [d, d])], "w6")}
      ${select("src", "المصدر", [["all", "الكل"], ["unified_6000", "البنك الموحد"], ["reviewed_1687", "البنك المنقح"], ["bookmarks", "المحفوظة"], ["mistakes", "أخطائي"]], "w6")}
      <div class="tools">
        <div class="seg" role="group" aria-label="وضع العرض">
          <button data-act="bank.mode" data-mode="interactive" aria-pressed="${qb.mode === "interactive"}">أجب بنفسك</button>
          <button data-act="bank.mode" data-mode="reveal" aria-pressed="${qb.mode === "reveal"}">أظهر الإجابات</button>
        </div>
        <div style="display:flex;gap:8px;flex-wrap:wrap">
          <button class="btn btn-quiet btn-sm" data-act="bank.reset">مسح المرشحات</button>
          <button class="btn btn-ghost btn-sm" data-act="bank.export" data-fmt="csv">${I("download")}CSV</button>
          <button class="btn btn-ghost btn-sm" data-act="bank.export" data-fmt="json">${I("download")}JSON</button>
        </div>
      </div>
    </div>
    <div id="bank-results" style="display:grid;gap:16px">${results()}</div>`;
  };

  const map = () => {
    const groups = {};
    Aden.bank.all.forEach((q) => {
      const g = ((groups[q.fid] = groups[q.fid] || {})[q.sid + "|" + q.unit] = groups[q.fid][q.sid + "|" + q.unit] || { sid: q.sid, sname: q.sname, unit: q.unit, n: 0 });
      g.n++;
    });
    return Aden.syllabus.map((f) => {
      const rows = Object.values(groups[f.id] || {});
      const max = Math.max(...rows.map((r) => r.n));
      return `<section class="card" style="--fc:${Aden.theme.color(f.id)}">
        <div class="subject-head"><div class="fac-icon" style="--fc:${Aden.theme.color(f.id)}">${I(f.icon)}</div><div class="grow"><h3>${esc(f.title)}</h3></div><span class="chip num">${rows.length} محورًا • ${fmt(rows.reduce((s, r) => s + r.n, 0))} سؤال</span></div>
        <div class="map-group">${rows.map((r) => `<button class="map-row" data-act="bank.map" data-fid="${f.id}" data-sid="${esc(r.sid)}" data-unit="${esc(r.unit)}">
          <div><b>${esc(r.sname)}</b></div><div class="bar-col">${Aden.ui.bar(Math.round((r.n / max) * 100))}</div><span class="num" style="text-align:end">${r.n}</span>${I("chevron")}
          <small style="grid-column:1/-1">${esc(r.unit)}</small></button>`).join("")}</div></section>`;
    }).join("");
  };

  const render = () => `
    <div class="page-head"><div><h1>بنك الأسئلة</h1><p class="num">${fmt(Aden.bank.all.length)} سؤالًا بتعليل لكل إجابة</p></div>
      <div class="seg" role="tablist"><button role="tab" aria-selected="${qb.tab === "list"}" data-act="bank.tab" data-tab="list">الأسئلة</button><button role="tab" aria-selected="${qb.tab === "map"}" data-act="bank.tab" data-tab="map">خريطة المفردات</button></div></div>
    ${qb.tab === "list" ? list() : `<div class="subjects">${map()}</div>`}`;

  const draw = () => { const el = document.getElementById("bank-results"); if (el) el.innerHTML = results(); };
  const onSearch = debounce(() => { qb.page = 1; draw(); }, 200);

  Aden.actions.register({
    "bank.tab": (el) => { qb.tab = el.dataset.tab; Aden.router.refresh(); },
    "bank.search": (el) => { qb.search = el.value; onSearch(); },
    "bank.filter": (el) => {
      qb[el.dataset.key] = el.value;
      if (el.dataset.key === "fid") { qb.sid = "all"; qb.unit = "all"; }
      if (el.dataset.key === "sid") qb.unit = "all";
      qb.page = 1;
      Aden.router.refresh();
    },
    "bank.mode": (el) => { qb.mode = el.dataset.mode; Aden.router.refresh(); },
    "bank.page": (el) => { qb.page = Number(el.dataset.page); draw(); document.getElementById("bank-results")?.scrollIntoView({ block: "start" }); },
    "bank.reset": () => { Object.assign(qb, defaults(), { tab: qb.tab, mode: qb.mode }); Aden.router.refresh(); },
    "bank.map": (el) => { Object.assign(qb, defaults(), { fid: el.dataset.fid, sid: el.dataset.sid, unit: el.dataset.unit }); Aden.router.refresh(); window.scrollTo(0, 0); },
    "bank.export": (el) => {
      const rows = filtered();
      if (el.dataset.fmt === "csv") Aden.util.download(Aden.bank.toCSV(rows), "aden-questions.csv", "text/csv;charset=utf-8");
      else Aden.util.download(JSON.stringify(rows.map((q) => ({ id: q.id, subject: q.sname, unit: q.unit, difficulty: q.diff, question: q.text, options: q.opts, correctIndex: q.ci, explanation: q.expl })), null, 2), "aden-questions.json", "application/json");
      Aden.ui.toast(`تم تجهيز ${fmt(rows.length)} سؤال`);
    },
  });
  return { render };
})();
Aden.router.register("bank", Aden.views.bank);
