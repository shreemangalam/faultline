/* Records real UI recovery in an isolated 39-event operations demo.
 * This script changes that demo's simulator and executes its pending events.
 * Set BASE_URL explicitly; outputs default to private/walkthrough.
 */
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || path.join(__dirname, "../dashboard/node_modules/playwright"));
const base = process.env.BASE_URL;
const output = path.resolve(__dirname, "..", process.env.QA_OUTPUT || "private/walkthrough");
assert.ok(base, "Set BASE_URL to an isolated operations-demo stack.");
fs.mkdirSync(output, { recursive: true });

(async () => {
  const browser = await chromium.launch({ headless: true,
    ...(process.env.PLAYWRIGHT_EXECUTABLE ? { executablePath: process.env.PLAYWRIGHT_EXECUTABLE } : {}) });
  let context;
  try {
    context = await browser.newContext({ viewport: { width: 1440, height: 900 }, colorScheme: "light",
      recordVideo: { dir: output, size: { width: 1440, height: 900 } } });
    const page = await context.newPage();
    for (const module of ["survey", "restore"]) {
      let ready = false;
      for (let attempt = 0; attempt < 30; attempt++) {
        const response = await page.request.get(base + `/api/${module}/v1/${module}/status`);
        if (response.ok()) { ready = true; break; }
        await new Promise(resolve => setTimeout(resolve, 1000));
      }
      assert.ok(ready, `${module} must be ready before recording`);
    }
    async function frame(name) {
      await page.evaluate(() => window.scrollTo(0, 0));
      // Brief reading time belongs to the recording, not to application readiness.
      await new Promise(resolve => setTimeout(resolve, 2500));
      await page.screenshot({ path: output + "/" + name + ".png", fullPage: true });
    }
    const initial = await (await page.request.get(base + "/api/restore/v1/statistics")).json();
    assert.equal(initial.totalEvents, 39, "Recording needs the isolated 39-event fixture");
    assert.equal(initial.pendingEventCount, 24, "Record before recovering the fixture");
    const pageErrors = [];
    page.on("pageerror", error => pageErrors.push(error.message));
    await page.goto(base + "/");
    await page.locator("#system").selectOption("SAP_ECC");
    await page.getByRole("button", { name: "Analyze complete outage" }).click();
    await page.getByRole("heading", { name: "SAP ECC outage", exact: true }).waitFor();
    await frame("survey-incident");
    await page.goto(base + "/restore");
    await page.getByText("Live updates connected", { exact: true }).waitFor();
    await page.getByRole("heading", { name: "Recovery queue" }).waitFor();
    await frame("restore-before");

    // A second tab must update through SSE without manual refresh or navigation.
    const observer = await browser.newPage();
    await observer.goto(base + "/restore");
    await observer.getByText("Live updates connected", { exact: true }).waitFor();
    await page.locator(".demo-tools > summary").click();
    async function action(name, suffix) {
      await page.getByRole("button", { name, exact: true }).click();
      const response = page.waitForResponse(response => response.url().endsWith(suffix)
        && response.request().method() === "POST");
      await page.getByRole("button", { name: "Confirm action", exact: true }).click();
      assert.equal((await response).ok(), true, name);
      await page.getByRole("button", { name: "Run next batch", exact: true }).waitFor({ state: "visible" });
      await page.waitForFunction(() => [...document.querySelectorAll("button")]
        .some(button => button.textContent === "Run next batch" && !button.disabled));
    }
    await action("Recover Salesforce endpoint", "/scenarios/operations-demo/recover");
    let final;
    let batches = 0;
    for (; batches < 20; batches++) {
      const circuits = await (await page.request.get(base + "/api/restore/v1/circuit-breakers")).json();
      const cooldown = Math.max(0, ...circuits.filter(c => c.state === "OPEN" && c.openUntil)
        .map(c => Date.parse(c.openUntil) - Date.now() + 100));
      if (cooldown > 0) await new Promise(resolve => setTimeout(resolve, Math.min(cooldown, 31000)));
      await action("Run next batch", "/worker/run-once");
      await page.evaluate(() => window.scrollTo(0, 0));
      await new Promise(resolve => setTimeout(resolve, 500));
      final = await (await page.request.get(base + "/api/restore/v1/statistics")).json();
      if (final.pendingEventCount === 0) { batches++; break; }
    }
    assert.equal(final.pendingEventCount, 0);
    assert.equal(final.eventCounts.COMPLETED, 37);
    assert.equal(final.terminalFailureCount, 2, "Terminal evidence must remain visible");
    await observer.getByRole("heading", { name: "No pending recovery work", exact: true }).waitFor();
    await page.getByRole("heading", { name: "No pending recovery work", exact: true }).waitFor();
    await page.locator(".demo-tools > summary").click();
    await frame("restore-after");
    await page.getByRole("button", { name: /^Final failure / }).click();
    await frame("terminal-evidence");
    assert.deepEqual(pageErrors, []);
    const video = page.video();
    await context.close();
    context = null;
    const videoPath = await video.path();
    const namedVideo = path.join(output, "faultline-walkthrough.webm");
    fs.renameSync(videoPath, namedVideo);
    fs.writeFileSync(output + "/result.json", JSON.stringify({ initial, final, batches,
      observerUpdatedThroughSSE: true, pageErrors, video: namedVideo }, null, 2));
    console.log(JSON.stringify({ batches, completed: final.eventCounts.COMPLETED,
      terminalFailures: final.terminalFailureCount, observerUpdatedThroughSSE: true, video: namedVideo }));
  } finally {
    if (context) await context.close();
    await browser.close();
  }
})().catch(error => { console.error(error); process.exitCode = 1; });
