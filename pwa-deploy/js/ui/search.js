/* Global search palette (Ctrl/Cmd+K or "/"): finds questions anywhere in the bank. */
Aden.search = (() => {
  const { esc, debounce, fmt } = Aden.util;
  let back = null;

  const close = () => { back?.remove(); back = null; document.removeEventListener("keydown", onKey, true); };
  const onKey = (e) => { if (e.key === "Escape") { e.preventDefault(); close(); } };

  const rows = (term) => {
    if (term.trim().length < 2) return `<p class="search-hint">اكتب كلمتين على الأقل من نص السؤال أو الوحدة أو التعليل.</p>`;
    const all = Aden.bank.query({ search: term });
    if (!all.length) return `<p class="search-hint">لا توجد نتائج لـ «${esc(term)}».</p>`;
    return all.slice(0, 8).map((q) => `<button class="search-row" data-act="search.open" data-id="${esc(q.id)}">
        <span class="chip dot" style="--fc:${Aden.theme.color(q.fid)}">${esc(q.sname)}</span>
        <span class="search-text">${esc(q.text.replace(/\$/g, "").slice(0, 110))}</span></button>`).join("")
      + `<button class="btn btn-ghost btn-sm" data-act="search.all" data-term="${esc(term)}">عرض كل النتائج (${fmt(all.length)}) في بنك الأسئلة</button>`;
  };

  const open = () => {
    if (back) return;
    back = document.createElement("div");
    back.className = "modal-back top";
    back.innerHTML = `<div class="modal card search-box" role="dialog" aria-modal="true" aria-label="بحث في بنك الأسئلة">
      <div class="search">${I("search")}<input class="input" id="gsearch" type="search" placeholder="ابحث في ${fmt(Aden.bank.all.length)} سؤالًا" autocomplete="off"></div>
      <div id="gsearch-results" class="search-results">${rows("")}</div></div>`;
    back.addEventListener("click", (e) => { if (e.target === back) close(); });
    document.body.appendChild(back);
    const input = back.querySelector("#gsearch");
    const draw = debounce(() => { back.querySelector("#gsearch-results").innerHTML = rows(input.value); }, 120);
    input.addEventListener("input", draw);
    document.addEventListener("keydown", onKey, true);
    input.focus();
  };

  Aden.actions.register({
    "search.open": (el) => { close(); Aden.views.bank.open({ search: el.dataset.id }); },
    "search.all": (el) => { close(); Aden.views.bank.open({ search: el.dataset.term }); },
    "search.show": open,
  });
  return { open, close };
})();
