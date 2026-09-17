/* Survey graph checks use the active API topology, without creating incident reports. */
const assert = require('node:assert/strict');
const fs = require('node:fs');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const base = process.env.BASE_URL || 'http://localhost:3002';
const output = process.env.QA_OUTPUT || 'private/graph-qa';
(async () => {
  fs.mkdirSync(output, { recursive: true });
  const browser = await chromium.launch({ headless: true, ...(process.env.PLAYWRIGHT_EXECUTABLE ? { executablePath: process.env.PLAYWRIGHT_EXECUTABLE } : {}) });
  try {
    const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
    const errors = []; page.on('pageerror', error => errors.push(error.message));
    const topology = await (await page.request.get(base + '/api/survey/v1/landscape/health-map')).json();
    await page.goto(base + '/');
    await page.locator('.focus-graph .map-node.selected').waitFor();
    for (const system of topology.systems) {
      await page.locator('#system').selectOption(system.systemId);
      const expected = topology.integrations.filter(edge => edge.sourceSystemId === system.systemId || edge.targetSystemId === system.systemId);
      const paths = await page.locator('.focus-graph path[data-integration-id]').evaluateAll(elements => elements.map(el => ({ id: el.dataset.integrationId, source: el.dataset.source, target: el.dataset.target })));
      assert.deepEqual(paths.map(path => path.id).sort(), expected.map(edge => edge.integrationId).sort(), system.systemId + ' paths');
      for (const path of paths) {
        const edge = expected.find(edge => edge.integrationId === path.id);
        assert.equal(path.source, edge.sourceSystemId); assert.equal(path.target, edge.targetSystemId);
      }
      assert.equal(await page.locator('.focus-graph .map-node.selected').getAttribute('data-system-id'), system.systemId);
      assert.equal(await page.locator('.focus-graph .edge-label').count(), expected.length);
    }
    await page.locator('#system').selectOption('LEGACY_WMS');
    assert.equal(await page.locator('.focus-graph [data-system-id="CPI_INVENTORY_SYNC"]').count(), 2, 'Reciprocal neighbor appears in each direction');
    await page.getByText('Systems connected in both directions appear on both sides.', { exact: false }).waitFor();
    await page.screenshot({ path: output + '/survey-reciprocal.png', fullPage: true });
    await page.getByRole('button', { name: 'Full landscape', exact: true }).click();
    assert.equal(await page.locator('.landscape-graph .map-node').count(), topology.systems.length);
    assert.equal(await page.locator('.landscape-graph path[data-integration-id]').count(), topology.integrations.length);
    const highlights = await page.locator('.landscape-graph .focused-link').evaluateAll(paths => paths.map(path => path.dataset.integrationId));
    await page.getByRole('button', { name: 'Select Salesforce', exact: true }).hover();
    assert.deepEqual(await page.locator('.landscape-graph .focused-link').evaluateAll(paths => paths.map(path => path.dataset.integrationId)), highlights, 'Hover must not contradict the inspector');
    const ecc = page.getByRole('button', { name: 'Select SAP ECC', exact: true });
    await ecc.focus(); await page.keyboard.press('Space');
    assert.equal(await page.locator('#system').inputValue(), 'SAP_ECC');
    await page.getByRole('button', { name: 'Direct connections', exact: true }).click();
    const neighbor = page.getByRole('button', { name: 'Select SAP Ariba (upstream)', exact: true });
    await neighbor.focus(); await page.keyboard.press('Enter');
    assert.equal(await page.locator('#system').inputValue(), 'SAP_ARIBA');
    await page.waitForFunction(() => document.activeElement?.getAttribute('aria-pressed') === 'true' && document.activeElement?.getAttribute('data-system-id') === 'SAP_ARIBA');
    for (const [width, height] of [[1440,900], [1366,768], [768,1024], [390,844]]) {
      await page.setViewportSize({ width, height });
      for (const theme of ['light','dark']) {
        if (await page.locator('html').getAttribute('data-theme') !== theme) await page.getByRole('button', { name: 'Use ' + theme + ' theme' }).click();
        await page.locator('#system').selectOption('SALESFORCE');
        for (const view of ['Direct connections', 'Full landscape']) {
          await page.getByRole('button', { name: view, exact: true }).click();
          assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth), false, `${width} ${view} page overflow`);
          const clippedLabels = await page.locator('.map-node').evaluateAll(nodes => nodes.flatMap(node => {
            if (!node.getClientRects().length) return [];
            const card = node.querySelector('rect').getBBox();
            return [...node.querySelectorAll('text')].filter(text => { const box=text.getBBox(); return box.x + box.width > card.width - 5; }).map(text=>text.textContent);
          }));
          assert.deepEqual(clippedLabels, [], 'Node labels must fit');
          await page.screenshot({ path: `${output}/survey-${view === 'Full landscape' ? 'landscape' : 'focus'}-${width}-${theme}.png`, fullPage: true });
        }
      }
    }
    await page.getByRole('button', { name: 'Direct connections', exact: true }).click();
    assert.equal(await page.locator('.compact-topology').isVisible(), true, 'Mobile uses vertical connections');
    assert.equal(await page.locator('.direct-scroll').isVisible(), false, 'Mobile does not require horizontal graph scrolling');
    assert.equal(await page.locator('.compact-path').count(), 3);
    const mobileNeighbor = page.getByRole('button', { name: 'Select CPI Order Sync (upstream)', exact: true });
    await mobileNeighbor.focus(); await page.keyboard.press('Enter');
    assert.equal(await page.locator('#system').inputValue(), 'CPI_ORDER_SYNC');
    await page.waitForFunction(() => document.activeElement?.getAttribute('data-system-id') === 'CPI_ORDER_SYNC');
    assert.deepEqual(errors, []);
    console.log('PASS: all 16 system neighborhoods match API paths/direction; overview, reciprocal links, hover, keyboard focus, labels and both themes at four sizes');
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
