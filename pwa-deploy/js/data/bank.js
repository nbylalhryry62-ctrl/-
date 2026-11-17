/* Question bank: decodes the compiled data file once, then answers queries. No DOM code here. */
Aden.bank = (() => {
  let all = [];
  const byId = new Map();

  const inflate = async (b64) => {
    const bin = Uint8Array.from(atob(b64), (c) => c.charCodeAt(0));
    const stream = new Blob([bin]).stream().pipeThrough(new DecompressionStream("deflate-raw"));
    return JSON.parse(await new Response(stream).text());
  };

  const load = async () => {
    const { m, q } = await inflate(Aden.qbankB64);
    all = q.map((r) => ({
      id: r[0], fid: m[r[1]], tid: m[r[2]], sid: m[r[3]], sname: m[r[4]], unit: m[r[5]], lesson: r[6], grade: m[r[7]],
      diff: m[r[8]], lang: m[r[9]], src: m[r[10]], status: m[r[11]], text: r[12], opts: r[13], ci: r[14], expl: r[15],
    }));
    all.forEach((x) => byId.set(x.id, x));
    Aden.qbankB64 = null;
    return all.length;
  };

  const matchesTrack = (q, tid) => {
    if (!tid || tid === "all" || q.tid === tid) return true;
    if (q.fid === "economics_politics" && q.tid === "econ_both") return tid === "econ_finance" || tid === "pol_science";
    if (q.fid === "languages" && q.tid === "lang_both") return tid === "bus_english" || tid === "translation";
    return false;
  };
  const matchesSubject = (q, sid) => {
    if (!sid || sid === "all" || q.sid === sid) return true;
    if (sid === "prin_econ_pol") return q.sid === "prin_econ";
    if (sid === "eng_bus") return ["eng_bus", "eng_func_lang", "eng_bus_lexis"].includes(q.sid);
    if (sid === "eng_trans") return ["eng_bus", "eng_func_lang", "eng_trans"].includes(q.sid);
    return false;
  };

  const query = (f = {}) => {
    const s = (f.search || "").trim().toLowerCase();
    const st = Aden.store.state;
    return all.filter((q) => {
      if (f.fid && f.fid !== "all" && q.fid !== f.fid) return false;
      if (f.tid && !matchesTrack(q, f.tid)) return false;
      if (f.sid && !matchesSubject(q, f.sid)) return false;
      if (f.unit && f.unit !== "all" && q.unit !== f.unit) return false;
      if (f.diff && f.diff !== "all" && q.diff !== f.diff) return false;
      if (f.src === "unified_6000" || f.src === "reviewed_1687") { if (q.src !== f.src) return false; }
      if (f.src === "bookmarks" && !st.bookmarks.includes(q.id)) return false;
      if (f.src === "mistakes" && !st.mistakes.includes(q.id)) return false;
      if (s && !(q.text + " " + q.opts.join(" ") + " " + q.unit + " " + q.lesson + " " + q.expl + " " + q.id).toLowerCase().includes(s)) return false;
      return true;
    });
  };

  const distinct = (list, key) => [...new Set(list.map((q) => q[key]))];
  const subjectsOf = (fid) => {
    const seen = new Map();
    all.forEach((q) => { if ((fid === "all" || q.fid === fid) && !seen.has(q.sid)) seen.set(q.sid, q.sname); });
    return [...seen].map(([id, name]) => ({ id, name }));
  };
  const unitsOf = (fid, sid) => distinct(query({ fid, sid }), "unit");
  const count = (fid) => (fid === "all" ? all.length : all.filter((q) => q.fid === fid).length);

  const toCSV = (list) => {
    const cell = (v) => `"${String(v ?? "").replace(/"/g, '""')}"`;
    const head = ["المعرف", "المادة", "الوحدة", "الصعوبة", "السؤال", "الخيار 1", "الخيار 2", "الخيار 3", "الخيار 4", "الإجابة الصحيحة", "التعليل"];
    const rows = list.map((q) => [q.id, q.sname, q.unit, q.diff, q.text, ...[0, 1, 2, 3].map((i) => q.opts[i] ?? ""), q.opts[q.ci], q.expl]);
    return "﻿" + [head, ...rows].map((r) => r.map(cell).join(",")).join("\r\n");
  };

  return {
    load, query, byId: (id) => byId.get(id), get all() { return all; }, count, subjectsOf, unitsOf, matchesTrack, matchesSubject, toCSV,
    difficulties: () => ["سهل", "متوسط", "متقدم", "نماذج القبول"],
  };
})();
