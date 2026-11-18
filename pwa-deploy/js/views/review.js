Aden.views.review = (() => {
  const { esc, fmt } = Aden.util;
  const PAGE = 15;
  let tab = "mistakes";
  let page = 1;

  const ids = () => Aden.store.state[tab === "mistakes" ? "mistakes" : "bookmarks"];

  const render = () => {
    const list = ids().map(Aden.bank.byId).filter(Boolean);
    const pages = Math.max(1, Math.ceil(list.length / PAGE));
    page = Math.min(page, pages);
    const slice = list.slice((page - 1) * PAGE, page * PAGE);
    const st = Aden.store.state;
    const body = !list.length
      ? Aden.ui.empty(tab === "mistakes" ? "target" : "bookmark",
          tab === "mistakes" ? "دفتر الأخطاء فارغ" : "لا توجد أسئلة محفوظة",
          tab === "mistakes" ? "الأسئلة التي تخطئ فيها تُضاف هنا تلقائيًا لتعود إليها." : "اضغط على أيقونة الحفظ في أي سؤال ليظهر هنا.",
          `<button class="btn btn-primary" data-act="rv.bank">تصفّح البنك</button>`)
      : `<div class="qlist">${slice.map((q, i) => `<div style="display:grid;gap:8px">${Aden.question.card(q, { mode: "reveal", index: (page - 1) * PAGE + i + 1 })}
          ${tab === "mistakes" ? `<div><button class="btn btn-ghost btn-sm" data-act="rv.master" data-qid="${esc(q.id)}">${I("checkcircle")}أتقنته، أزله من الدفتر</button></div>` : ""}</div>`).join("")}</div>
        ${Aden.ui.pager(page, pages, "rv.page")}`;
    return `
    <div class="page-head"><div><h1>دفتر الأخطاء</h1><p>راجع ما أخطأت فيه وما حفظته، ثم اختبر نفسك عليه.</p></div>
      <div style="display:flex;gap:10px;flex-wrap:wrap">
        <button class="btn btn-ghost" data-act="rv.due" ${Aden.learning.dueCount() ? "" : "disabled"}>${I("retry")}مراجعة اليوم (${Aden.learning.dueCount()})</button>
        <button class="btn btn-primary" data-act="rv.exam" ${list.length ? "" : "disabled"}>${I("play")}اختبار من هذه الأسئلة</button></div></div>
    <div class="seg" role="tablist">
      <button role="tab" aria-selected="${tab === "mistakes"}" data-act="rv.tab" data-tab="mistakes">أخطائي (${fmt(st.mistakes.length)})</button>
      <button role="tab" aria-selected="${tab === "saved"}" data-act="rv.tab" data-tab="saved">المحفوظة (${fmt(st.bookmarks.length)})</button>
    </div>${body}`;
  };

  Aden.actions.register({
    "rv.tab": (el) => { tab = el.dataset.tab; page = 1; Aden.router.refresh(); },
    "rv.page": (el) => { page = Number(el.dataset.page); Aden.router.refresh(); window.scrollTo(0, 0); },
    "rv.bank": () => Aden.router.go("bank"),
    "rv.master": (el) => { Aden.store.remove("mistakes", el.dataset.qid); Aden.shell.syncBadges(); Aden.router.refresh(); },
    "rv.due": () => { if (Aden.exam.startFromIds(Aden.learning.due(20), "مراجعة اليوم", 20)) Aden.router.go("exam"); },
    "rv.exam": () => {
      if (Aden.exam.startFromIds(ids(), tab === "mistakes" ? "اختبار من أخطائي" : "اختبار من المحفوظات")) Aden.router.go("exam");
    },
  });
  return { render };
})();
Aden.router.register("review", Aden.views.review);
