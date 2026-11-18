// Browser end-to-end checks for the newer features. Usage: node tests/e2e.mjs [baseUrl]
// Needs Playwright and a server on pwa-deploy (default http://localhost:8766/).
import { createRequire } from "node:module";
const { chromium } = createRequire(import.meta.url)("playwright");
const base = process.argv[2] || "http://localhost:8766/";
const fails = []; const ok = (c, m) => { console.log(c ? "ok  " : "FAIL", m); if (!c) fails.push(m); };
const b = await chromium.launch(); const p = await (await b.newContext({ viewport: { width: 1280, height: 900 } })).newPage();
const errs = []; p.on("pageerror", (e) => errs.push(e.message));
await p.goto(base); await p.waitForSelector(".home-hero, .hero, [data-view=home]", { timeout: 20000 }).catch(() => {});
await p.waitForFunction(() => window.Aden && Aden.bank && Aden.bank.all && Aden.bank.all.length > 7000, null, { timeout: 30000 });
ok(true, "bank loaded");
// search palette
await p.keyboard.press("Control+k"); await p.waitForTimeout(300);
ok(await p.locator("input[type=search], .search input, #search-input").first().isVisible(), "Ctrl+K opens search");
await p.keyboard.type("قانون"); await p.waitForTimeout(500);
await p.keyboard.press("Escape"); await p.waitForTimeout(200);
// exam resume
await p.evaluate(() => { Aden.exam.set({ fid: "law", sid: "all", diff: "all", count: 10, minutes: 10 }); Aden.exam.start(); Aden.exam.answer(Aden.exam.session.qs[0].id, 1); location.hash = "#/exam"; });
await p.waitForTimeout(400);
await p.reload(); await p.waitForFunction(() => window.Aden && Aden.exam && Aden.exam.session, null, { timeout: 30000 }).catch(() => {});
ok(await p.evaluate(() => !!(Aden.exam.session && Object.keys(Aden.exam.session.answers).length === 1)), "running exam restored after reload");
await p.evaluate(() => { Aden.exam.submit(); location.hash = "#/"; });
await p.waitForTimeout(500);
ok(await p.evaluate(() => document.body.scrollWidth <= innerWidth + 1), "no horizontal overflow on home");
ok(await p.evaluate(() => Aden.store.state.history.length >= 1), "history recorded");
ok(await p.evaluate(() => document.querySelectorAll("[hidden]").length >= 0 && [...document.querySelectorAll("[hidden]")].every((e) => getComputedStyle(e).display === "none")), "[hidden] elements are really hidden");
await p.setViewportSize({ width: 390, height: 800 }); await p.waitForTimeout(300);
ok(await p.evaluate(() => document.body.scrollWidth <= innerWidth + 1), "no horizontal overflow on mobile");
ok(errs.length === 0, "no page errors " + errs.join("|"));
await b.close(); process.exit(fails.length ? 1 : 0);
