/* localStorage wrapper that never throws (private windows, blocked storage, previews). */
Aden.storage = (() => {
  const PREFIX = "aden.v2.";
  const mem = {};
  const get = (key, fallback) => {
    try {
      const raw = localStorage.getItem(PREFIX + key);
      if (raw !== null) return JSON.parse(raw);
    } catch (e) { /* fall through to memory */ }
    return key in mem ? mem[key] : fallback;
  };
  const set = (key, val) => {
    mem[key] = val;
    try { localStorage.setItem(PREFIX + key, JSON.stringify(val)); } catch (e) { /* memory only */ }
  };
  /* Progress saved by the first version of the app lives under aden_* keys. */
  const legacy = (oldKey, fallback) => {
    try { const raw = localStorage.getItem(oldKey); return raw ? JSON.parse(raw) : fallback; } catch (e) { return fallback; }
  };
  return { get, set, legacy };
})();
