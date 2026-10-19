# Local API and downstream credentials

Survey and Restore allow public GETs and require `X-Faultline-Api-Key` for API mutations. Set `FAULTLINE_API_KEY` to the same operator key in the backends and dashboard; enabled security requires at least 16 characters. Non-sensitive development defaults are provided for localhost only. Invalid/missing keys return HTTP 401 with a problem-details body.

The dashboard's server-side proxy injects its configured service key on mutations. Browser JavaScript does not contain that configured key. The dashboard itself has no user login: any client able to access it can use its operator proxy. Read endpoints expose operational evidence. Keep the supplied Compose ports bound to loopback. Public hosting requires a separate access model, such as authenticated access or a restricted read-only demo.

Restore's built-in HTTP simulator is protected by the same mutation rule. For the default same-service simulator, `DownstreamDeliveryClient` adds the local operator key. When `RESTORE_DOWNSTREAM_BASE_URL` selects another service, it does **not** automatically forward the operator key; set `RESTORE_DOWNSTREAM_API_KEY` explicitly when that target requires a credential. A configured downstream still uses the simulator-shaped URL and payload contract; this is not a general SAP adapter.

The recovery benchmark sends the selected `FAULTLINE_API_KEY` on its HTTP requests, and its Compose environment passes the same key to Restore. The ordinary operations UI uses the proxy credential, while direct API examples must supply a header.

## Contract workflow

Both services expose OpenAPI JSON at `/v3/api-docs`. Static snapshots live in `docs/api/`. Generated deployment-specific `servers` entries are omitted from snapshots so the same contract can be checked across local/CI ports.

```powershell
$env:SURVEY_OPENAPI_URL = 'http://localhost:18081/v3/api-docs'
$env:RESTORE_OPENAPI_URL = 'http://localhost:18082/v3/api-docs'
npm run contracts:export --prefix dashboard
npm run contracts:check --prefix dashboard
```

Export only from a runtime built from the intended source revision, then review the snapshot diff. `qa:security` verifies the API boundary and proxy; backend tests also execute successful, transient, and permanent real HTTP delivery with security enabled. Another test verifies separately configured downstreams never receive the local operator key implicitly.

The installed springdoc dependency provides JSON, not a Swagger UI. See [Survey's snapshot](../api/survey-openapi.json) and [Restore's snapshot](../api/restore-openapi.json) for the full current schemas.
