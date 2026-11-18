/* Page frame: side rail (wide screens), top bar and bottom tabs (phones), toast host. */
Aden.shell = (() => {
  const { esc } = Aden.util;
  const NAV = [
    ["home", "الرئيسية", "home"], ["syllabus", "المنهج", "book"], ["bank", "بنك الأسئلة", "db"],
    ["exam", "الاختبار", "exam"], ["review", "دفتر الأخطاء", "notebook"],
  ];
  const links = (cls) => NAV.map(([id, label, icon]) =>
    `<a href="#${id}" data-nav="${id}">${I(icon)}<span>${label}</span>${id === "review" ? '<span class="badge" data-badge hidden></span>' : ""}</a>`).join("");

  const mount = () => {
    document.getElementById("app").innerHTML = `
      <button class="skip" data-act="a11y.skip">تخطَّ إلى المحتوى</button>
      <aside class="rail">
        <div class="brand"><div class="brand-mark">${I("cap")}</div><div><b>بوابة القبول</b><small>جامعة عدن 2026 / 2027</small></div></div>
        <nav class="nav" aria-label="التنقل الرئيسي">${links()}</nav>
        <div class="rail-foot">
          <div class="rail-card" id="rail-ready"></div>
          <button class="btn btn-quiet" data-act="search.show">${I("search")}<span>بحث</span><kbd class="kbd-hint">Ctrl K</kbd></button>
          <button class="btn btn-quiet" data-act="pwa.install" data-install hidden>${I("download")}<span>تثبيت التطبيق</span></button>
          <button class="btn btn-quiet" data-act="theme.toggle">${I("moon")}<span id="theme-label"></span></button>
        </div>
      </aside>
      <div class="col" style="min-width:0">
        <header class="topbar">
          <div class="brand"><div class="brand-mark">${I("cap")}</div><div><b>بوابة القبول</b></div></div>
          <div style="display:flex;gap:2px">
            <button class="icon-btn" data-act="pwa.install" data-install hidden aria-label="تثبيت التطبيق">${I("download")}</button>
            <button class="icon-btn" data-act="search.show" aria-label="بحث">${I("search")}</button>
            <button class="icon-btn" data-act="theme.toggle" aria-label="تبديل المظهر">${I("moon")}</button>
          </div>
        </header>
        <main class="main"><div id="view" class="page" tabindex="-1"></div></main>
      </div>
      <nav class="tabbar" aria-label="التنقل">${links()}</nav>`;
    syncBadges();
  };

  const syncBadges = () => {
    const n = Aden.store.state.mistakes.length;
    document.querySelectorAll("[data-badge]").forEach((b) => { b.hidden = !n; b.textContent = n > 99 ? "99+" : n; });
    const r = Aden.progress.readiness(Aden.store.state.faculty);
    const el = document.getElementById("rail-ready");
    if (el) {
      el.innerHTML = `${Aden.ui.ring(r.pct, { size: 46, th: 6, label: null, color: "#36c9a0" })}
        <div><b>جاهزيتك ${r.pct}%</b><small>${r.done} من ${r.total} وحدة</small></div>`;
    }
    const t = document.getElementById("theme-label");
    if (t) t.textContent = Aden.theme.isDark() ? "المظهر النهاري" : "المظهر الليلي";
  };

  const sync = (route) => {
    document.querySelectorAll("[data-nav]").forEach((a) => (a.dataset.nav === route ? a.setAttribute("aria-current", "page") : a.removeAttribute("aria-current")));
    document.body.classList.toggle("focus", route === "exam" && Aden.exam.active());
    syncBadges();
  };

  /* Installable web app: keep the browser's prompt until the user asks for it. */
  let installEvent = null;
  window.addEventListener("beforeinstallprompt", (e) => {
    e.preventDefault();
    installEvent = e;
    document.querySelectorAll("[data-install]").forEach((b) => (b.hidden = false));
  });
  window.addEventListener("appinstalled", () => document.querySelectorAll("[data-install]").forEach((b) => (b.hidden = true)));

  Aden.actions.register({
    "theme.toggle": () => { Aden.theme.toggle(); syncBadges(); },
    "a11y.skip": () => document.getElementById("view").focus(),
    "pwa.install": async () => {
      if (!installEvent) return;
      installEvent.prompt();
      await installEvent.userChoice;
      installEvent = null;
      document.querySelectorAll("[data-install]").forEach((b) => (b.hidden = true));
    },
  });
  return { mount, sync, syncBadges };
})();
