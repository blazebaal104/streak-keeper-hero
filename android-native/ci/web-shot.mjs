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
// the other tabs
for (const [name, file] of [["Cal", "cal"], ["Trend", "trend"], ["Social", "social"]]) {
  await step(file, async () => {
    await page.getByRole("tab", { name }).click();
    await page.waitForTimeout(1200);
    await page.screenshot({ path: `shots/web-tab-${file}.png` });
    await page.screenshot({ path: `shots/web-tab-${file}-full.png`, fullPage: true });
  });
}
await browser.close();
