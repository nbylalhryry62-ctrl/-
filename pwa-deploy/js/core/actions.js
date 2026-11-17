/* Event delegation: any element with data-act="group.name" calls Aden.actions["group.name"](el, event).
   Views register handlers; there are no inline onclick attributes anywhere. */
Aden.actions = (() => {
  const handlers = {};
  const register = (map) => Object.assign(handlers, map);
  const run = (el, ev) => {
    const fn = handlers[el.dataset.act];
    if (fn) fn(el, ev);
  };
  document.addEventListener("click", (ev) => {
    const el = ev.target.closest("[data-act]");
    if (el && !el.matches("input, select") && !el.disabled) { run(el, ev); }
  });
  document.addEventListener("change", (ev) => {
    const el = ev.target.closest("select[data-act], input[type=checkbox][data-act]");
    if (el) run(el, ev);
  });
  document.addEventListener("input", (ev) => {
    const el = ev.target.closest("input[data-act][type=search], input[data-act][type=text]");
    if (el) run(el, ev);
  });
  return { register };
})();
