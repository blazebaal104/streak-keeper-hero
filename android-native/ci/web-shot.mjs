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
await browser.close();
