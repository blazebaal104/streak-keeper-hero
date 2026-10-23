// Screenshots of the current web app (dev site) at phone size: the "before" for comparisons.
import { chromium } from "playwright";
const browser = await chromium.launch();
const ctx = await browser.newContext({ viewport: { width: 412, height: 915 }, deviceScaleFactor: 2.625, isMobile: true, hasTouch: true });
await ctx.addInitScript(() => { try { localStorage.setItem("sbo:devHoliday", "none"); } catch {} });
const page = await ctx.newPage();
await page.goto("https://sbo-dev.perlospam696.workers.dev/", { waitUntil: "networkidle" });
await page.waitForTimeout(7000);
for (let i = 1; i <= 3; i++) { await page.screenshot({ path: `shots/web-home-${i}.png` }); await page.waitForTimeout(350); }
const step = async (name, fn) => { try { await fn(); } catch (e) { console.log(`web ${name} failed: ${e.message}`); } };
// medal inventory (tap the first slot) and the "locked medal" preview
await step("inventory", async () => {
  await page.getByText("slot", { exact: true }).first().click();
  await page.waitForTimeout(900);
  await page.screenshot({ path: "shots/web-inventory.png" });
  await page.getByRole("dialog").evaluate((d) => d.scrollTo(0, 420));
  await page.waitForTimeout(400);
  await page.screenshot({ path: "shots/web-inventory-2.png" });
  await page.getByRole("dialog").evaluate((d) => d.scrollTo(0, d.scrollHeight));
  await page.waitForTimeout(400);
  await page.screenshot({ path: "shots/web-inventory-titles.png" });
  await page.getByRole("dialog").evaluate((d) => d.scrollTo(0, d.scrollHeight - 1400));
  await page.waitForTimeout(300);
  await page.screenshot({ path: "shots/web-inventory-titles-2.png" });
  await page.getByRole("dialog").evaluate((d) => d.scrollTo(0, 420));
  await page.getByText("Gelo", { exact: true }).first().click();
  await page.waitForTimeout(900);
  await page.screenshot({ path: "shots/web-preview.png" });
  await page.keyboard.press("Escape"); await page.waitForTimeout(300);
  await page.keyboard.press("Escape"); await page.waitForTimeout(600);
});
// falling drops right after a press (dev account; the event is undone right after)
await step("drops", async () => {
  await page.getByLabel("Aggiungi 1 al contatore").click();
  for (const t of [250, 450, 450]) { await page.waitForTimeout(t); await page.screenshot({ path: `shots/web-drops-${t}-${Date.now() % 1000}.png` }); }
  await page.waitForTimeout(2500);
  await page.getByText("Annulla ultimo").first().click();
  await page.waitForTimeout(1500);
});
// header dialogs: profile, notifications (bell), settings
for (const [label, file] of [["Apri profilo", "profile"], ["Notifiche", "invites"], ["Impostazioni", "settings"]]) {
  await step(file, async () => {
    await page.getByLabel(label).first().click();
    await page.waitForTimeout(900);
    await page.screenshot({ path: `shots/web-${file}.png` });
    await page.getByRole("dialog").evaluate((d) => d.scrollTo(0, d.scrollHeight)).catch(() => {});
    await page.locator("[role=dialog] .overflow-y-auto").first().evaluate((d) => d.scrollTo(0, d.scrollHeight)).catch(() => {});
    await page.waitForTimeout(300);
    await page.screenshot({ path: `shots/web-${file}-2.png` });
    await page.keyboard.press("Escape"); await page.waitForTimeout(600);
  });
}
// the other tabs
for (const [name, file] of [["Cal", "cal"], ["Trend", "trend"], ["Social", "social"]]) {
  await step(file, async () => {
    await page.getByRole("tab", { name }).click();
    await page.waitForTimeout(1200);
    await page.screenshot({ path: `shots/web-tab-${file}.png` });
    await page.screenshot({ path: `shots/web-tab-${file}-full.png`, fullPage: true });
    if (file === "trend") {
      // tap two points: tooltip of the 30-day and the 90-day chart
      const charts = page.locator(".recharts-wrapper");
      const b1 = await charts.nth(0).boundingBox(); await page.mouse.click(b1.x + b1.width * 0.66, b1.y + b1.height * 0.5);
      await page.waitForTimeout(500); await page.screenshot({ path: "shots/web-trend-tip30.png" });
      const b2 = await charts.nth(1).boundingBox(); await page.mouse.click(b2.x + b2.width * 0.66, b2.y + b2.height * 0.5);
      await page.waitForTimeout(500); await page.screenshot({ path: "shots/web-trend-tip90.png" });
    }
    if (file === "social") {
      await page.getByText("amico_test", { exact: true }).last().click();
      await page.waitForTimeout(1500);
      await page.screenshot({ path: "shots/web-friend.png" });
      await page.locator("[role=dialog]").first().evaluate((d) => d.scrollTo(0, d.scrollHeight));
      await page.waitForTimeout(400);
      await page.screenshot({ path: "shots/web-friend-2.png" });
      await page.keyboard.press("Escape"); await page.waitForTimeout(500);
    }
  });
}
// Progetto Zero: the dev account has it off, so mock its rows (no data is written)
await step("pz", async () => {
  const z = await browser.newContext({ viewport: { width: 412, height: 915 }, deviceScaleFactor: 2.625, isMobile: true, hasTouch: true });
  await z.addInitScript(() => { try { localStorage.setItem("sbo:devHoliday", "none"); } catch {} });
  const day = (n) => { const d = new Date(Date.now() - n * 86400000); return d.toISOString().slice(0, 10); };
  const started = new Date(Date.now() - 9 * 86400000).toISOString();
  const pzRow = { user_id: "x", active: true, started_at: started, league: "mid", start_limit: 4, theme: "obsidian",
    answers: { age: 20, pushups: 25, plank: 60, pullups: 4, squats: 40, weeklyNow: 5, realName: "Tester", dumbbellKg: 8, hasBar: true, gymDays: [6] },
    last_assessment_at: started, limit_set_at: null };
  const wds = [3, 5, 6, 8].map((n) => ({ user_id: "x", day: day(n), done_ids: [], plan_size: 4, custom: [{ name: "Corsa", note: "", at: started }], completed: true }));
  const msgs = [
    { id: "m1", user_id: "x", kind: "daily", content: "Buongiorno Tester. Oggi si lavora: niente scuse.", created_at: new Date(Date.now() - 2 * 3600e3).toISOString(), read_at: null },
    { id: "m2", user_id: "x", kind: "user", content: "Ok coach, ci provo", created_at: new Date(Date.now() - 3600e3).toISOString(), read_at: new Date().toISOString() },
    { id: "m3", user_id: "x", kind: "rant", content: "PROVARE NON BASTA. FALLO.", created_at: new Date(Date.now() - 3500e3).toISOString(), read_at: null },
  ];
  await z.route(/\/rest\/v1\/project_zero/, (r) => r.request().method() === "GET" ? r.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(r.request().headers()["accept"]?.includes("object") ? pzRow : [pzRow]) }) : r.fulfill({ status: 204, body: "" }));
  await z.route(/\/rest\/v1\/workout_days/, (r) => r.request().method() === "GET" ? r.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(wds) }) : r.fulfill({ status: 204, body: "" }));
  await z.route(/\/rest\/v1\/agent_messages/, (r) => r.request().method() === "GET" ? r.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(msgs) }) : r.fulfill({ status: 204, body: "" }));
  await z.route(/_serverFn/, (r) => r.fulfill({ status: 200, contentType: "application/json", body: "{}" }));
  const pg = await z.newPage();
  await pg.goto("https://sbo-dev.perlospam696.workers.dev/", { waitUntil: "networkidle" });
  await pg.waitForTimeout(6000);
  await pg.screenshot({ path: "shots/web-pz-plan.png" });
  await pg.getByText("Vai alla home").click().catch(() => {});
  await pg.waitForTimeout(800);
  await pg.screenshot({ path: "shots/web-pz-home.png" });
  await pg.screenshot({ path: "shots/web-pz-home-full.png", fullPage: true });
  await pg.getByLabel("Impostazioni").first().click(); await pg.waitForTimeout(900);
  await pg.screenshot({ path: "shots/web-pz-settings.png" });
  await pg.keyboard.press("Escape"); await pg.waitForTimeout(500);
  for (const [name, file] of [["Cal", "cal"], ["Trend", "trend"], ["Social", "social"]]) {
    await pg.getByRole("tab", { name }).click(); await pg.waitForTimeout(1000);
    await pg.screenshot({ path: `shots/web-pz-${file}.png` });
  }
  await pg.getByText("Coach Zero").first().click(); await pg.waitForTimeout(900);
  await pg.screenshot({ path: "shots/web-pz-coach.png" });
  await pg.keyboard.press("Escape"); await pg.waitForTimeout(400);
  await z.close();
});
// questionnaire (intro + first question) with Progetto Zero off: open settings and flip the switch
await step("quiz", async () => {
  await page.getByRole("tab", { name: "Home" }).click(); await page.waitForTimeout(500);
  await page.getByLabel("Impostazioni").first().click(); await page.waitForTimeout(800);
  await page.locator("[role=dialog] button[role=switch]").first().click(); await page.waitForTimeout(900);
  await page.screenshot({ path: "shots/web-quiz-1.png" });
  await page.getByText("Iniziamo").click(); await page.waitForTimeout(600);
  await page.screenshot({ path: "shots/web-quiz-2.png" });
  await page.keyboard.press("Escape"); await page.waitForTimeout(400);
});
// the native app's coach API on the dev worker: without a token it must answer 401 JSON
await step("api", async () => {
  const fs = await import("node:fs");
  const r = await page.request.post("https://sbo-dev.perlospam696.workers.dev/api/public/native/agent-tick", { data: {} });
  fs.writeFileSync("shots/api-check.txt", `${r.status()} ${await r.text()}`.slice(0, 500));
});
await browser.close();
