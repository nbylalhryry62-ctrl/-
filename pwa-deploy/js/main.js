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
  const restored = Aden.exam.restore();   // an exam that was running when the page closed
  if (restored) location.hash = "exam";
  Aden.router.render();
  if (restored === "finished") Aden.ui.toast("انتهى وقت اختبارك أثناء غيابك، وهذه نتيجته");
  document.addEventListener("keydown", (e) => {
    const typing = /^(INPUT|SELECT|TEXTAREA)$/.test(e.target.tagName);
    if (((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "k") || (e.key === "/" && !typing && !Aden.exam.active())) {
      e.preventDefault();
      Aden.search.open();
    }
  });
  if ("serviceWorker" in navigator && /^https?:$/.test(location.protocol)) {
    window.addEventListener("load", () => navigator.serviceWorker.register("./sw.js").catch(() => {}));
  }
})();
