const assert = require("node:assert/strict");

const survey = process.env.SURVEY_API_URL || "http://localhost:8081";
const restore = process.env.RESTORE_API_URL || "http://localhost:8082";
const dashboard = process.env.BASE_URL || "http://localhost:3002";
const apiKey = process.env.FAULTLINE_API_KEY || "local-development-only";

(async () => {
  for (const [name, base, statusPath, mutationPath] of [
    ["Survey", survey, "/api/v1/survey/status", "/api/v1/landscape/import/validate"],
    ["Restore", restore, "/api/v1/restore/status", "/api/v1/events"]
  ]) {
    const read = await fetch(base + statusPath);
    assert.equal(read.status, 200, `${name} public read`);
    assert.equal(read.headers.get("x-content-type-options"), "nosniff");
    assert.equal(read.headers.get("x-frame-options"), "DENY");
    assert.equal(read.headers.get("referrer-policy"), "no-referrer");

    const missing = await fetch(base + mutationPath, jsonRequest());
    assert.equal(missing.status, 401, `${name} mutation without key`);
    assert.match(missing.headers.get("content-type") || "", /^application\/problem\+json/);
    assert.equal((await missing.json()).status, 401);

    const wrong = await fetch(base + mutationPath, jsonRequest("incorrect-api-key-value"));
    assert.equal(wrong.status, 401, `${name} mutation with wrong key`);

    const authenticated = await fetch(base + mutationPath, jsonRequest(apiKey));
    assert.notEqual(authenticated.status, 401, `${name} mutation with operator key`);

    const proxied = await fetch(dashboard + "/api/" + name.toLowerCase() + mutationPath.slice(4), jsonRequest());
    assert.notEqual(proxied.status, 401, `${name} dashboard proxy injects its server-side credential`);
  }

  const dashboardPage = await fetch(dashboard);
  assert.equal(dashboardPage.status, 200);
  assert.match(dashboardPage.headers.get("content-security-policy") || "", /frame-ancestors 'none'/);
  assert.equal(dashboardPage.headers.get("x-content-type-options"), "nosniff");
  console.log("PASS: public reads, authenticated mutations, problem responses, server-side proxy and security headers");
})().catch(error => {
  console.error(error);
  process.exitCode = 1;
});

function jsonRequest(key) {
  const headers = { "content-type": "application/json" };
  if (key) headers["X-Faultline-Api-Key"] = key;
  return { method: "POST", headers, body: "{}" };
}
