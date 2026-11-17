/* Theme (light / dark / follow system) and per-faculty accent colours. */
Aden.theme = (() => {
  const FACULTY_COLORS = {
    medical: "#0a8a6a", sports_nutrition: "#0b8394", law: "#a8323e", cs_it: "#2b67c9",
    economics_politics: "#b0700c", languages: "#c4502d", engineering: "#5a6578",
  };
  const color = (id) => FACULTY_COLORS[id] || "#0a7a62";
  const isDark = () => {
    const t = Aden.store.state.theme;
    return t === "dark" || (t === "system" && matchMedia("(prefers-color-scheme: dark)").matches);
  };
  const apply = () => {
    const root = document.documentElement;
    const t = Aden.store.state.theme;
    if (t === "system") root.removeAttribute("data-theme"); else root.setAttribute("data-theme", t);
    document.querySelector('meta[name="theme-color"]')?.setAttribute("content", isDark() ? "#07111c" : "#f2f5f8");
  };
  const toggle = () => { Aden.store.set("theme", isDark() ? "light" : "dark"); apply(); };
  return { color, isDark, apply, toggle };
})();
