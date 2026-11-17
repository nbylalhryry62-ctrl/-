/* Hash routes with plain ASCII names. Views register a renderer per route. */
Aden.router = (() => {
  const views = {};
  let current = "home";
  const ROUTES = ["home", "syllabus", "bank", "exam", "review"];
  const register = (name, view) => { views[name] = view; };
  const fromHash = () => {
    const name = location.hash.replace(/^#/, "");
    return ROUTES.includes(name) ? name : "home";
  };
  const render = () => {
    current = fromHash();
    const host = document.getElementById("view");
    host.innerHTML = views[current].render();
    views[current].mount?.(host);
    Aden.shell.sync(current);
    window.scrollTo(0, 0);
  };
  const go = (name) => {
    if (location.hash === "#" + name) render(); else location.hash = name;
  };
  const refresh = () => {
    const y = window.scrollY;
    const host = document.getElementById("view");
    host.innerHTML = views[current].render();
    views[current].mount?.(host);
    Aden.shell.sync(current);
    window.scrollTo(0, y);
  };
  window.addEventListener("hashchange", render);
  return { register, render, go, refresh, get current() { return current; } };
})();
