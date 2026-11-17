/* Question card shared by the bank, the mistakes notebook and the exam review. */
Aden.question = (() => {
  const { esc } = Aden.util;
  const M = Aden.mathText;
  const picks = {}; // answers given while browsing (not in an exam): { questionId: optionIndex }

  const letters = (q) => (q.lang === "en" ? ["A", "B", "C", "D", "E"] : ["أ", "ب", "ج", "د", "هـ"]);

  /* state: { picked, reveal } -> which options show right/wrong marks and whether they can still be clicked */
  const options = (q, { picked, reveal = false, locked = false, act = "q.pick" } = {}) => {
    const L = letters(q);
    const done = reveal || (picked !== undefined && locked);
    return `<div class="opts" role="group" aria-label="الخيارات">${q.opts.map((o, i) => {
      let cls = "opt" + (q.lang === "en" ? " en" : "");
      if (done && i === q.ci) cls += " right";
      else if (done && picked === i) cls += " wrong";
      else if (picked === i) cls += " picked";
      return `<button class="${cls}" data-act="${act}" data-qid="${esc(q.id)}" data-opt="${i}" ${done ? "disabled" : ""}><span class="k latin">${L[i]}</span><span>${M(o)}</span></button>`;
    }).join("")}</div>`;
  };

  const feedback = (q, picked) => {
    const L = letters(q);
    const ok = picked === q.ci;
    const head = picked === undefined
      ? `<h4>${I("eye")}<span>الإجابة الصحيحة: ${L[q.ci]}</span></h4>`
      : ok
        ? `<h4 style="color:var(--good)">${I("checkcircle")}<span>إجابة صحيحة</span></h4>`
        : `<h4 style="color:var(--bad)">${I("xcircle")}<span>إجابتك ${L[picked]}، والصحيحة ${L[q.ci]}</span></h4>`;
    const cls = picked === undefined ? "" : ok ? "good" : "bad";
    return `<div class="explain ${cls}">${head}<p>${M(q.expl || "وفق المقرر المعتمد في دليل جامعة عدن.")}</p>
      <p class="ref">${esc(q.sname)} • ${esc(q.unit)}${q.lesson ? " • " + esc(q.lesson) : ""}</p></div>`;
  };

  const meta = (q, { index } = {}) => {
    const saved = Aden.store.state.bookmarks.includes(q.id);
    return `<div class="q-meta">
      ${index ? `<span class="chip num">${index}</span>` : ""}
      <span class="chip dot" style="--fc:${Aden.theme.color(q.fid)}">${esc(q.sname)}</span>
      <span class="chip">${esc(q.diff)}</span>
      <span class="grow"></span>
      <button class="icon-btn ${saved ? "on" : ""}" data-act="q.bookmark" data-qid="${esc(q.id)}" aria-pressed="${saved}" aria-label="حفظ السؤال">${I("bookmark")}</button>
    </div>`;
  };

  /* mode: "interactive" (click to answer) | "reveal" (answer always visible) */
  const card = (q, { mode = "interactive", index } = {}) => {
    const picked = picks[q.id];
    const reveal = mode === "reveal";
    const showFeedback = reveal || picked !== undefined;
    return `<article class="card q" data-qcard="${esc(q.id)}" data-mode="${mode}">
      ${meta(q, { index })}
      <p class="q-text${q.lang === "en" ? " en" : ""}">${M(q.text)}</p>
      ${options(q, { picked, reveal, locked: true })}
      ${showFeedback ? feedback(q, reveal ? undefined : picked) : ""}
    </article>`;
  };

  const refresh = (id) => {
    const el = document.querySelector(`[data-qcard="${CSS.escape(id)}"]`);
    if (el) el.outerHTML = card(Aden.bank.byId(id), { mode: el.dataset.mode });
  };

  Aden.actions.register({
    "q.pick": (el) => {
      const q = Aden.bank.byId(el.dataset.qid);
      const idx = Number(el.dataset.opt);
      if (picks[q.id] !== undefined) return;
      picks[q.id] = idx;
      if (idx !== q.ci) Aden.store.add("mistakes", q.id);
      refresh(q.id);
    },
    "q.bookmark": (el) => {
      Aden.store.toggle("bookmarks", el.dataset.qid);
      const on = Aden.store.state.bookmarks.includes(el.dataset.qid);
      el.classList.toggle("on", on);
      el.setAttribute("aria-pressed", on);
      Aden.ui.toast(on ? "تم حفظ السؤال" : "أُزيل من المحفوظات");
      Aden.shell.syncBadges();
    },
  });

  return { card, options, feedback, letters, picks, refresh };
})();
