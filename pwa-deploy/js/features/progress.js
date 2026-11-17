/* Derived learner statistics. Pure functions over the store and the syllabus. */
Aden.progress = (() => {
  const unitsOf = (f) => f.tracks.flatMap((t) => t.subjects.flatMap((s) => s.units));
  const faculty = (id) => Aden.syllabus.find((f) => f.id === id) || Aden.syllabus[0];
  const readiness = (fid) => {
    const ids = new Set(unitsOf(faculty(fid)).map((u) => u.id));
    const done = Aden.store.state.units.filter((u) => ids.has(u)).length;
    return { done, total: ids.size, pct: Aden.util.pct(done, ids.size) };
  };
  const summary = () => {
    const h = Aden.store.state.history;
    return {
      exams: h.length,
      avg: h.length ? Math.round(h.reduce((s, x) => s + x.pct, 0) / h.length) : 0,
      mistakes: Aden.store.state.mistakes.length,
      saved: Aden.store.state.bookmarks.length,
    };
  };
  return { faculty, readiness, summary, unitsOf };
})();
