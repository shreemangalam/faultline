/* Regression checks for inspector context against real Restore data; no backend writes. */
const assert = require('node:assert/strict');
const path = require('node:path');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || path.join(__dirname, '../dashboard/node_modules/playwright'));
(async () => {
  const browser = await chromium.launch({ headless: true, ...(process.env.PLAYWRIGHT_EXECUTABLE ? { executablePath: process.env.PLAYWRIGHT_EXECUTABLE } : {}) });
  try {
    const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
    await page.goto((process.env.BASE_URL || 'http://localhost:3002') + '/restore');
    const rows = page.locator('.queue-table tbody button');
    const detail = page.locator('.evidence-panel');
    await rows.first().waitFor();
    await rows.first().click();
    assert.ok(await detail.evaluate(el => { el.scrollTop = 200; return el.scrollTop; }) > 0, 'Fixture needs scrollable evidence');
    await page.getByRole('button', { name: 'Refresh', exact: true }).click();
    await page.waitForLoadState('networkidle');
    assert.ok(await detail.evaluate(el => el.scrollTop) > 0, 'Refreshing the same message must preserve reading position');
    await rows.nth(1).click();
    await page.waitForFunction(() => document.querySelector('.evidence-panel').scrollTop === 0);
    await page.getByRole('searchbox', { name: 'Search messages' }).fill('no-matching-business-reference');
    await page.getByText('No matching messages', { exact: true }).waitFor();
    assert.equal(await detail.locator('h2').innerText(), 'Select a message');
    assert.equal(await detail.locator('.evidence-reference').count(), 0, 'Empty results must not retain another message');
    await page.getByRole('button', { name: 'Clear filters', exact: true }).click();
    await page.getByRole('button', { name: /^Final failure / }).click();
    assert.equal(await detail.locator('.evidence-reference .state-badge').innerText(), 'Final failure');
    assert.equal(await page.locator('.queue-table button[aria-pressed="true"]').count(), 1);
    await page.getByRole('button', { name: /^Blocked / }).click();
    await page.getByRole('searchbox', { name: 'Search messages' }).fill('Inventory reservation');
    await detail.getByRole('button').first().click();
    assert.equal(await page.getByRole('button', { name: /^All events / }).getAttribute('aria-pressed'), 'true', 'Following a prerequisite outside the filter clears the filter');
    assert.equal(await page.locator('.queue-table button[aria-pressed="true"]').count(), 1, 'Referenced prerequisite is visible in the queue');
    console.log('PASS: selection scroll reset, same-message refresh, empty search, filtered selection, prerequisite navigation');
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });


