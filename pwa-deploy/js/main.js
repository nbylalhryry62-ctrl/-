/* Boot: apply theme, draw the frame, decode the bank, then show the first route. */
(async () => {
  Aden.theme.apply();
  matchMedia("(prefers-color-scheme: dark)").addEventListener("change", () => { Aden.theme.apply(); Aden.shell.syncBadges(); });
  Aden.shell.mount();
  const view = document.getElementById("view");
  view.innerHTML = `<div class="empty card">${I("db")}<h3>جاري تحميل بنك الأسئلة</h3><p>لحظات ويكون كل شيء جاهزًا.</p></div>`;
  try {
    await Aden.bank.load();
  } catch (e) {
    view.innerHTML = `<div class="empty card">${I("alert")}<h3>تعذر تحميل بنك الأسئلة</h3><p>حدّث الصفحة، وإن استمرت المشكلة فجرّب متصفحًا أحدث.</p></div>`;
    throw e;
  }
  Aden.store.subscribe(Aden.shell.syncBadges);
  Aden.router.render();
  if ("serviceWorker" in navigator && /^https?:$/.test(location.protocol)) {
    window.addEventListener("load", () => navigator.serviceWorker.register("./sw.js").catch(() => {}));
  }
})();
