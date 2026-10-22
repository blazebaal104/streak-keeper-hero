// Screenshot of the current web app (dev site) at phone size: the "before" for comparisons.
import { chromium } from "playwright";
const browser = await chromium.launch();
const ctx = await browser.newContext({ viewport: { width: 412, height: 915 }, deviceScaleFactor: 2.625, isMobile: true, hasTouch: true });
await ctx.addInitScript(() => { try { localStorage.setItem("sbo:devHoliday", "none"); } catch {} });
const page = await ctx.newPage();
await page.goto("https://sbo-dev.perlospam696.workers.dev/", { waitUntil: "networkidle" });
await page.waitForTimeout(7000);
for (let i = 1; i <= 3; i++) { await page.screenshot({ path: `shots/web-home-${i}.png` }); await page.waitForTimeout(350); }
await browser.close();
