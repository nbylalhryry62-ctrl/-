Aden.views.syllabus = (() => {
  const { esc, fmt } = Aden.util;
  let trackId = null;

  const unitRow = (u, color) => {
    const done = Aden.store.state.units.includes(u.id);
    return `<button class="unit" role="checkbox" aria-checked="${done}" data-act="syl.unit" data-uid="${u.id}">
      <span class="check" aria-checked="${done}">${I("check")}</span>
      <span class="grow"><span class="t">${esc(u.title)}</span><small>${esc(u.pages || "")}</small></span>
      <span class="chip">${esc(u.grade)}</span>
    </button>`;
  };

  const subjectCard = (f, t, s, color) => {
    const done = s.units.filter((u) => Aden.store.state.units.includes(u.id)).length;
    const n = Aden.bank.query({ fid: f.id, tid: t.id, sid: s.id }).length;
    return `<section class="card">
      <div class="subject-head">
        <div class="grow"><h3>${esc(s.name)}</h3><span class="eyebrow num">${done} من ${s.units.length} وحدة • ${fmt(n)} سؤال</span></div>
        <span class="chip">${s.lang === "en" ? "English" : "العربية"}</span>
        <button class="btn btn-ghost btn-sm" data-act="syl.practice" data-tid="${t.id}" data-sid="${s.id}">${I("play")}تدرّب</button>
      </div>
      ${s.units.map((u) => unitRow(u, color)).join("")}
    </section>`;
  };

  const render = () => {
    const f = Aden.progress.faculty(Aden.store.state.faculty);
    const color = Aden.theme.color(f.id);
    const t = f.tracks.find((x) => x.id === trackId) || f.tracks[0];
    trackId = t.id;
    const r = Aden.progress.readiness(f.id);
    return `
    <div class="page-head"><div><h1>المنهج الدراسي</h1><p>علّم الوحدات التي أنهيتها ليرتفع مؤشر جاهزيتك.</p></div></div>
    <div class="fac-tabs" role="tablist">${Aden.syllabus.map((x) => `<button style="--fc:${Aden.theme.color(x.id)}" data-act="syl.faculty" data-fid="${x.id}" aria-pressed="${x.id === f.id}">${I(x.icon)}${esc(x.title.replace(/^(الكليات|كلية)\s*/, ""))}</button>`).join("")}</div>
    <div class="card fac-banner" style="--fc:${color}">
      ${Aden.ui.ring(r.pct, { size: 84, th: 9, color })}
      <div class="grow"><h2>${esc(f.title)}</h2><p style="color:var(--muted)">${esc(f.subtitle)}</p></div>
      <button class="btn btn-primary" style="background:${color};color:#fff" data-act="syl.practice" data-tid="${t.id}" data-sid="all">${I("play")}اختبار على الكلية</button>
    </div>
    ${f.tracks.length > 1 ? `<div class="seg" role="tablist">${f.tracks.map((x) => `<button role="tab" aria-selected="${x.id === t.id}" data-act="syl.track" data-tid="${x.id}">${esc(x.name)}</button>`).join("")}</div>` : ""}
    <div class="subjects">${t.subjects.map((s) => subjectCard(f, t, s, color)).join("")}</div>`;
  };

  Aden.actions.register({
    "syl.faculty": (el) => { Aden.store.set("faculty", el.dataset.fid); trackId = null; Aden.router.refresh(); },
    "syl.track": (el) => { trackId = el.dataset.tid; Aden.router.refresh(); },
    "syl.unit": (el) => { Aden.store.toggle("units", el.dataset.uid); Aden.router.refresh(); },
    "syl.practice": (el) => {
      const fid = Aden.store.state.faculty;
      Aden.exam.set({ fid, tid: el.dataset.tid, sid: el.dataset.sid, diff: "all" });
      Aden.exam.reset();
      Aden.router.go("exam");
    },
  });
  return { render };
})();
Aden.router.register("syllabus", Aden.views.syllabus);
