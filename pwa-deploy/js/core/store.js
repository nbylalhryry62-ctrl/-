/* One place for persisted learner progress. Views read it, features change it through actions below. */
Aden.store = (() => {
  const S = Aden.storage;
  const state = {
    units: S.get("units", S.legacy("aden_completed_units", [])),
    bookmarks: S.get("bookmarks", S.legacy("aden_bookmarked_questions", [])),
    mistakes: S.get("mistakes", S.legacy("aden_mistake_questions", [])),
    history: S.get("history", []),
    theme: S.get("theme", S.legacy("aden_dark_mode", null) === true ? "dark" : "system"),
    faculty: S.get("faculty", "medical"),
    stats: S.get("stats", {}),      // per question: { a: attempts, c: correct, b: Leitner box 0-5, t: last time, d: next review time }
    days: S.get("days", []),        // days with activity, "YYYY-MM-DD"
    daily: S.get("daily", { k: "", n: 0 }),
    goal: S.get("goal", 20),
    badges: S.get("badges", {}),    // badge id -> unlock time
  };
  const listeners = new Set();
  const emit = () => listeners.forEach((fn) => fn(state));
  const save = (key) => { S.set(key, state[key]); emit(); };
  const set = (key, value) => { state[key] = value; save(key); };
  const toggle = (key, id) => set(key, Aden.util.toggleIn(state[key], id));
  const add = (key, id) => { if (!state[key].includes(id)) set(key, [...state[key], id]); };
  const remove = (key, id) => { if (state[key].includes(id)) set(key, state[key].filter((x) => x !== id)); };
  const pushHistory = (entry) => set("history", [entry, ...state.history].slice(0, 20));
  return { state, set, toggle, add, remove, pushHistory, subscribe: (fn) => (listeners.add(fn), () => listeners.delete(fn)) };
})();
