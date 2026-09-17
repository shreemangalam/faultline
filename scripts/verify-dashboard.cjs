/* Read-only Restore browser checks against a running stack.
 * Set PLAYWRIGHT_MODULE and PLAYWRIGHT_EXECUTABLE when using a bundled runtime.
 * BASE_URL defaults to the isolated demo. Survey analysis creates a report.
 */
const assert = require("node:assert/strict");
const fs = require("node:fs");
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || "playwright");
const base = process.env.BASE_URL || "http://localhost:3002";
const output = process.env.QA_OUTPUT || "private/dashboard-qa";
fs.mkdirSync(output, { recursive: true });

(async () => {
  const browser = await chromium.launch({ headless: true, ...(process.env.PLAYWRIGHT_EXECUTABLE ? { executablePath: process.env.PLAYWRIGHT_EXECUTABLE } : {}) });
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, colorScheme: "light" });
  const page = await context.newPage();
  const errors = [];
  page.on("pageerror", error => errors.push(error.message));
  const before = await (await context.request.get(base + "/api/restore/v1/statistics")).json();
  const results = [];
  try {
    for (const [width, height] of [[1440, 900], [1366, 768], [768, 1024], [390, 844]]) {
      await page.setViewportSize({ width, height });
      for (const theme of ["light", "dark"]) {
        await page.goto(base + "/restore");
        await page.getByRole("heading", { name: "Recovery queue" }).waitFor();
        if (await page.locator("html").getAttribute("data-theme") !== theme)
          await page.getByRole("button", { name: "Use " + theme + " theme" }).click();
        await page.getByText("Live updates connected", { exact: true }).waitFor({ timeout: 15000 });
        assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth), false, "Restore horizontal overflow");
        await page.screenshot({ path: output + "/restore-" + width + "-" + theme + ".png", fullPage: true });
        results.push("Restore " + width + " × " + height + " " + theme);
        const more = page.getByRole("button", { name: "Show 30 more", exact: true });
        if (await more.isVisible()) { const count = await page.locator(".queue-table tbody tr").count(); await more.click(); assert.ok(await page.locator(".queue-table tbody tr").count() > count, "Queue expansion failed"); }
        await page.getByRole("searchbox", { name: "Search messages" }).fill("no-matching-business-reference");
        await page.getByText("No matching messages", { exact: true }).waitFor();
        await page.getByRole("button", { name: "Clear filters" }).click();
        await page.locator(".demo-tools > summary").click();
        await page.getByRole("button", { name: "Run next batch", exact: true }).click();
        await page.getByRole("dialog").waitFor();
        assert.equal(await page.getByRole("button", { name: "Cancel", exact: true }).evaluate(el => el === document.activeElement), true, "Dialog initial focus");
        await page.keyboard.press("Escape");
        assert.equal(await page.getByRole("dialog").isVisible(), false);
        await page.goto(base + "/");
        await page.getByRole("heading", { name: "Integration landscape", exact: true }).waitFor();
        assert.equal(await page.locator("html").getAttribute("data-theme"), theme, "Theme persisted across navigation");
        assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth), false, "Survey horizontal overflow");
        await page.screenshot({ path: output + "/survey-" + width + "-" + theme + ".png", fullPage: true });
        await page.goto(base + "/help");
        await page.getByRole("heading", { name: "Your first five minutes" }).waitFor();
        assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth), false, "Guide horizontal overflow");
        await page.screenshot({ path: output + "/help-" + width + "-" + theme + ".png", fullPage: true });
      }
    }
    await page.setViewportSize({ width: 1440, height: 900 });
    await page.goto(base + "/");
    const node = page.getByRole("button", { name: "Select SAP ECC", exact: true });
    await node.focus();
    await page.keyboard.press("Space");
    assert.equal(await page.locator("#system").inputValue(), "SAP_ECC");
    await page.getByRole("button", { name: "Analyze complete outage" }).click();
    await page.getByRole("heading", { name: "SAP ECC outage", exact: true }).waitFor();
    await page.getByText("Manual review required", { exact: true }).waitFor();
    await page.screenshot({ path: output + "/survey-incident-dark.png", fullPage: true });
    // Fail only the stream transport, then reconnect to the actual backend.
    await page.route("**/api/restore/v1/stream", route => route.abort("connectionfailed"));
    await page.goto(base + "/restore");
    await page.getByRole("heading", { name: "Recovery queue" }).waitFor();
    await page.getByText("Reconnecting to updates", { exact: true }).waitFor();
    const snapshot = await page.locator(".snapshot-note").innerText();
    await page.unroute("**/api/restore/v1/stream");
    await page.getByText("Live updates connected", { exact: true }).waitFor({ timeout: 15000 });
    await page.waitForFunction(old => document.querySelector(".snapshot-note")?.textContent !== old, snapshot);
    results.push("SSE reconnect rebuilt the REST snapshot");

    // Transport faults are browser-local; no API writes or production mocks.
    let releaseLoading;
    const loadingGate = new Promise(resolve => { releaseLoading = resolve; });
    await page.route("**/api/restore/v1/events", async route => { await loadingGate; await route.continue(); });
    await page.goto(base + "/restore");
    await page.getByText("Loading recovery queue, target health and event evidence…", { exact: true }).waitFor();
    releaseLoading();
    await page.getByRole("heading", { name: "Recovery queue" }).waitFor();
    await page.unroute("**/api/restore/v1/events");
    await page.route("**/api/restore/v1/statistics", route => route.abort("connectionfailed"));
    await page.getByRole("button", { name: "Refresh", exact: true }).click();
    await page.getByRole("alert").filter({ hasText: "Restore is unavailable" }).waitFor();
    assert.equal(await page.getByRole("heading", { name: "Recovery queue" }).isVisible(), true, "Failure discarded the last snapshot");
    await page.unroute("**/api/restore/v1/statistics");
    await page.getByRole("button", { name: "Refresh", exact: true }).click();
    await page.getByRole("alert").filter({ hasText: "Restore is unavailable" }).waitFor({ state: "hidden" });
    await page.locator(".demo-tools > summary").click();
    await page.route("**/api/restore/v1/worker/run-once", route => route.fulfill({ status: 503, body: "QA transport failure" }));
    await page.getByRole("button", { name: "Run next batch", exact: true }).click();
    await page.getByRole("button", { name: "Confirm action", exact: true }).click();
    await page.getByRole("alert").filter({ hasText: "Run next batch could not complete" }).waitFor();
    await page.getByRole("button", { name: "Refresh", exact: true }).click();
    assert.equal(await page.getByRole("alert").filter({ hasText: "Run next batch could not complete" }).isVisible(), true, "Snapshot refresh hid action failure");
    await page.unroute("**/api/restore/v1/worker/run-once");
    results.push("Loading, retained snapshot on failure, and persistent action error");
    const after = await (await context.request.get(base + "/api/restore/v1/statistics")).json();
    assert.equal(after.totalEvents, before.totalEvents, "Read-only checks changed Restore row count");
    assert.equal(errors.length, 0, errors.join("\n"));
    fs.writeFileSync(output + "/results.json", JSON.stringify({ base, results, restoreEvents: after.totalEvents, pageErrors: errors }, null, 2));
    console.log(JSON.stringify({ passed: results, restoreEvents: after.totalEvents, pageErrors: errors }));
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
