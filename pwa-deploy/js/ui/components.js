/* Reusable fragments: buttons, rings, empty states, toast, confirm dialog. */
Aden.ui = (() => {
  const { esc } = Aden.util;
  const ring = (p, { size = 120, th = 11, color = "var(--brand)", label = `${p}%`, sub = "" } = {}) => `
    <div class="ring-wrap" style="--size:${size}px">
      <div class="ring" style="--p:${p};--size:${size}px;--th:${th}px;--ringc:${color}"></div>
      ${label === null ? "" : `<div class="ring-label"><b class="num">${label}</b>${sub ? `<small>${esc(sub)}</small>` : ""}</div>`}
    </div>`;
  const bar = (p, color) => `<div class="bar" role="progressbar" aria-valuenow="${p}" aria-valuemin="0" aria-valuemax="100"><i style="width:${p}%${color ? `;--fc:${color}` : ""}"></i></div>`;
  const empty = (icon, title, text, actionHtml = "") => `<div class="empty card">${I(icon)}<h3>${esc(title)}</h3><p>${esc(text)}</p>${actionHtml}</div>`;
  const pctClass = (p) => (p >= 70 ? "good" : p >= 50 ? "gold" : "bad");
  const pctChip = (p) => `<span class="pct ${pctClass(p)} chip" style="min-height:0">${p}%</span>`;
  const pager = (page, pages, act) => {
    if (pages <= 1) return "";
    const nums = [];
    for (let i = 1; i <= pages; i++) if (i === 1 || i === pages || Math.abs(i - page) <= 1) nums.push(i);
    const out = []; let last = 0;
    nums.forEach((n) => { if (n - last > 1) out.push("…"); out.push(n); last = n; });
    return `<nav class="pager" aria-label="الصفحات">
      <button data-act="${act}" data-page="${page - 1}" ${page === 1 ? "disabled" : ""} aria-label="السابق">${I("chevron")}</button>
      ${out.map((n) => (n === "…" ? "<span>…</span>" : `<button data-act="${act}" data-page="${n}" class="num" ${n === page ? 'aria-current="page"' : ""}>${n}</button>`)).join("")}
      <button data-act="${act}" data-page="${page + 1}" ${page === pages ? "disabled" : ""} aria-label="التالي" style="transform:scaleX(-1)">${I("chevron")}</button>
    </nav>`;
  };
  const toast = (msg) => {
    const host = document.getElementById("toasts");
    const el = Object.assign(document.createElement("div"), { className: "toast", textContent: msg });
    host.appendChild(el);
    setTimeout(() => el.remove(), 2600);
  };
  const confirm = ({ title, text, ok = "تأكيد", cancel = "إلغاء", danger = false }) => new Promise((resolve) => {
    const back = document.createElement("div");
    back.className = "modal-back";
    back.innerHTML = `<div class="modal card" role="dialog" aria-modal="true" aria-label="${esc(title)}">
      <h3>${esc(title)}</h3><p style="color:var(--muted)">${esc(text)}</p>
      <div class="row"><button class="btn btn-ghost" data-x="0">${esc(cancel)}</button><button class="btn ${danger ? "btn-danger" : "btn-primary"}" data-x="1">${esc(ok)}</button></div></div>`;
    const onKey = (e) => { if (e.key === "Escape") done(false); };
    const done = (v) => { document.removeEventListener("keydown", onKey); back.remove(); resolve(v); };
    document.addEventListener("keydown", onKey);
    back.addEventListener("click", (e) => { const b = e.target.closest("[data-x]"); if (b) done(b.dataset.x === "1"); else if (e.target === back) done(false); });
    document.body.appendChild(back);
    back.querySelector("[data-x='1']").focus();
  });
  return { ring, bar, empty, pctClass, pctChip, pager, toast, confirm };
})();
