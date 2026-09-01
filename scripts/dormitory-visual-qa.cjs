const { chromium } = require("C:/Users/12778/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright");
const fs = require("node:fs");
const path = require("node:path");

(async () => {
  const output = path.resolve("qa-output/dormitory");
  fs.mkdirSync(output, { recursive: true });
  const browser = await chromium.launch({
    headless: true,
    executablePath: "C:/Program Files/Google/Chrome/Application/chrome.exe",
  });
  const page = await browser.newPage({ viewport: { width: 2048, height: 953 }, deviceScaleFactor: 1 });
  const errors = [];
  page.on("console", (message) => {
    if (message.type() === "error" && !message.text().includes("404")) errors.push(`console: ${message.text()}`);
  });
  page.on("pageerror", (error) => errors.push(`page: ${error.message}`));

  const reference = await browser.newPage({ viewport: { width: 2048, height: 953 }, deviceScaleFactor: 1 });
  await reference.goto("file:///D:/download/%E5%AE%BF%E8%88%8D%E7%AE%A1%E7%90%86%E7%B3%BB%E7%BB%9F/8.6/%E6%A0%B7%E5%BC%8F%E9%A2%84%E8%A7%88.html", { waitUntil: "load" });
  await reference.screenshot({ path: path.join(output, "0-8.6参考页面.png"), fullPage: false });
  await reference.close();

  await page.goto("http://127.0.0.1:8088/visitor/login?redirect=/dormitory", { waitUntil: "networkidle" });
  await page.getByPlaceholder("请输入账号").fill("dorm");
  await page.getByPlaceholder("请输入密码").fill("11111111");
  await page.getByRole("button", { name: "登录", exact: true }).click();
  await page.waitForURL(/\/visitor\/dormitory/);
  await page.waitForLoadState("networkidle");

  const sections = ["统计总览", "可视化平面图", "预警看板", "入住台账", "水电报表", "历史档案", "设置"];
  for (const name of sections) {
    await page.locator(".dorm-nav button", { hasText: name }).click();
    await page.evaluate(() => window.scrollTo(0, 0));
    await page.waitForTimeout(250);
    await page.screenshot({ path: path.join(output, `${sections.indexOf(name) + 1}-${name}.png`), fullPage: false });
  }

  await page.locator(".dorm-nav button", { hasText: "可视化平面图" }).click();
  const bed = page.locator(".bed-list button").first();
  if (await bed.count()) {
    await bed.click();
    await page.locator(".modal-card").waitFor({ state: "visible" });
    await page.screenshot({ path: path.join(output, "8-入住表单弹窗.png"), fullPage: false });
    await page.locator(".drawer-close").click();
  }
  await page.locator(".dorm-nav button", { hasText: "设置" }).click();
  await page.getByRole("button", { name: "新增楼栋", exact: true }).click();
  await page.locator(".modal-card").waitFor({ state: "visible" });
  await page.screenshot({ path: path.join(output, "9-资源表单弹窗.png"), fullPage: false });
  await page.locator(".drawer-close").click();

  await page.setViewportSize({ width: 1366, height: 768 });
  await page.locator(".dorm-nav button", { hasText: "统计总览" }).click();
  await page.evaluate(() => window.scrollTo(0, 0));
  await page.screenshot({ path: path.join(output, "10-统计总览-1366.png"), fullPage: false });
  const desktopOverflow = await page.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth);
  await page.setViewportSize({ width: 390, height: 844 });
  await page.screenshot({ path: path.join(output, "11-统计总览-手机.png"), fullPage: false });
  const mobileOverflow = await page.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth);
  await page.setViewportSize({ width: 2048, height: 953 });

  await page.locator(".region-primary button", { hasText: "全集团" }).click();
  const sub = page.locator(".region-sub button");
  if (await sub.count()) {
    await sub.first().click();
    await page.waitForTimeout(200);
  }
  await page.locator(".dorm-nav button", { hasText: "统计总览" }).click();
  const layout = await page.evaluate(() => Object.fromEntries(
    [".dorm-head", ".dorm-title", ".region-tabs", ".region-primary", ".dorm-account", ".dorm-nav", ".dorm-content", ".dorm-stats article"]
      .map((selector) => {
        const element = document.querySelector(selector);
        if (!element) return [selector, null];
        const box = element.getBoundingClientRect();
        const css = getComputedStyle(element);
        return [selector, { x: box.x, y: box.y, width: box.width, height: box.height, display: css.display, order: css.order }];
      }),
  ));
  fs.writeFileSync(path.join(output, "result.json"), JSON.stringify({ errors, url: page.url(), desktopOverflow, mobileOverflow, layout }, null, 2));
  await browser.close();
  if (errors.length) process.exitCode = 2;
})().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
