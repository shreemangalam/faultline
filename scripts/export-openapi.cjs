const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");

const check = process.argv.includes("--check");
const contracts = [
  {
    name: "Survey",
    url: process.env.SURVEY_OPENAPI_URL || "http://localhost:8081/v3/api-docs",
    file: path.join(__dirname, "../docs/api/survey-openapi.json"),
    minimumOperations: 10
  },
  {
    name: "Restore",
    url: process.env.RESTORE_OPENAPI_URL || "http://localhost:8082/v3/api-docs",
    file: path.join(__dirname, "../docs/api/restore-openapi.json"),
    minimumOperations: 18
  }
];

(async () => {
  for (const contract of contracts) {
    const response = await fetch(contract.url);
    assert.equal(response.ok, true, `${contract.name} OpenAPI returned HTTP ${response.status}`);
    const document = await response.json();
    // Deployment addresses are not part of the versioned API contract.
    delete document.servers;
    validate(contract, document);
    if (check) {
      assert.deepEqual(document, JSON.parse(fs.readFileSync(contract.file, "utf8")),
        `${contract.name} OpenAPI snapshot is stale; run npm --prefix dashboard run contracts:export`);
    } else {
      fs.writeFileSync(contract.file, JSON.stringify(document, null, 2) + "\n");
    }
    console.log(`${check ? "verified" : "exported"} ${contract.name} OpenAPI: ${Object.keys(document.paths).length} paths`);
  }
})().catch(error => {
  console.error(error);
  process.exitCode = 1;
});

function validate(contract, document) {
  assert.match(document.openapi, /^3\./);
  assert.equal(document.components.securitySchemes.operatorApiKey.name, "X-Faultline-Api-Key");
  const operations = [];
  for (const [route, item] of Object.entries(document.paths)) {
    for (const method of ["get", "post", "put", "patch", "delete"]) {
      if (!item[method]) continue;
      operations.push([method, route, item[method]]);
      if (method === "get") {
        assert.equal(item[method].security, undefined, `${contract.name} read unexpectedly requires an API key: ${route}`);
      } else {
        assert.deepEqual(item[method].security, [{ operatorApiKey: [] }],
          `${contract.name} mutation lacks operator security: ${method.toUpperCase()} ${route}`);
      }
    }
  }
  assert.ok(operations.length >= contract.minimumOperations,
    `${contract.name} contract unexpectedly contains only ${operations.length} operations`);
}
