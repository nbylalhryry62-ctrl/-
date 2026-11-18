/* Small SVG charts coloured through the theme tokens, so they follow light and dark mode. */
Aden.charts = (() => {
  const { esc } = Aden.util;

  /* Line chart of exam scores (0-100). points: [{ pct, label }] oldest first. */
  const trend = (points) => {
    const W = 600, H = 210, L = 34, R = 14, T = 14, B = 28;
    const x = (i) => L + (points.length === 1 ? (W - L - R) / 2 : (i * (W - L - R)) / (points.length - 1));
    const y = (v) => T + ((100 - v) / 100) * (H - T - B);
    const line = points.map((p, i) => `${i ? "L" : "M"}${x(i).toFixed(1)} ${y(p.pct).toFixed(1)}`).join(" ");
    const area = `${line} L${x(points.length - 1).toFixed(1)} ${y(0)} L${x(0).toFixed(1)} ${y(0)} Z`;
    const grid = [0, 50, 70, 100].map((v) => `<line x1="${L}" x2="${W - R}" y1="${y(v)}" y2="${y(v)}" stroke="var(--line)" stroke-dasharray="${v === 70 ? "5 4" : "0"}"/><text x="${L - 8}" y="${y(v) + 4}" text-anchor="end" font-size="11" fill="var(--muted)">${v}</text>`).join("");
    const dots = points.map((p, i) => `<circle cx="${x(i).toFixed(1)}" cy="${y(p.pct).toFixed(1)}" r="4.5" fill="var(--surface)" stroke="var(--brand)" stroke-width="2.5"><title>${esc(p.label)}: ${p.pct}%</title></circle>`).join("");
    return `<svg viewBox="0 0 ${W} ${H}" style="width:100%;height:auto;direction:ltr" role="img" aria-label="تطور نتائج الاختبارات">
      <defs><linearGradient id="trendfill" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="var(--brand)" stop-opacity=".28"/><stop offset="1" stop-color="var(--brand)" stop-opacity="0"/></linearGradient></defs>
      ${grid}<path d="${area}" fill="url(#trendfill)"/><path d="${line}" fill="none" stroke="var(--brand)" stroke-width="2.5" stroke-linejoin="round" stroke-linecap="round"/>${dots}
      <text x="${L}" y="${H - 6}" font-size="11" fill="var(--muted)">الأقدم</text><text x="${W - R}" y="${H - 6}" text-anchor="end" font-size="11" fill="var(--muted)">الأحدث</text></svg>`;
  };

  return { trend };
})();
