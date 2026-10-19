import { NextRequest } from "next/server";

export const dynamic = "force-dynamic";
export const runtime = "nodejs";

type RouteContext = { params: Promise<{ service: string; path: string[] }> };
const safeMethods = new Set(["GET", "HEAD", "OPTIONS"]);
const forwardedRequestHeaders = ["accept", "content-type", "last-event-id"];
const forwardedResponseHeaders = ["cache-control", "content-type", "location", "retry-after"];

async function proxy(request: NextRequest, context: RouteContext) {
  const { service, path } = await context.params;
  if (service !== "survey" && service !== "restore") return problem(404, "Unknown backend service.");

  const base = service === "survey"
    ? process.env.SURVEY_API_URL ?? "http://localhost:8081"
    : process.env.RESTORE_API_URL ?? "http://localhost:8082";
  const backendPath = path.length === 1 && path[0] === "openapi"
    ? "/v3/api-docs"
    : "/api/" + path.map(encodeURIComponent).join("/");
  const target = new URL(backendPath, base);
  target.search = request.nextUrl.search;

  const headers = new Headers();
  for (const name of forwardedRequestHeaders) {
    const value = request.headers.get(name);
    if (value) headers.set(name, value);
  }
  if (!safeMethods.has(request.method)) {
    const apiKey = service === "survey"
      ? process.env.SURVEY_API_KEY ?? process.env.FAULTLINE_API_KEY ?? "local-development-only"
      : process.env.RESTORE_API_KEY ?? process.env.FAULTLINE_API_KEY ?? "local-development-only";
    if (!apiKey) return problem(503, "The dashboard API proxy is missing its operator credential.");
    headers.set("X-Faultline-Api-Key", apiKey);
  }

  try {
    const body = safeMethods.has(request.method) ? undefined : await request.arrayBuffer();
    const upstream = await fetch(target, {
      method: request.method,
      headers,
      body,
      cache: "no-store",
      redirect: "manual"
    });
    const responseHeaders = new Headers();
    for (const name of forwardedResponseHeaders) {
      const value = upstream.headers.get(name);
      if (value) responseHeaders.set(name, value);
    }
    if (responseHeaders.get("content-type")?.startsWith("text/event-stream")) {
      responseHeaders.set("cache-control", "no-cache, no-transform");
    }
    return new Response(upstream.body, { status: upstream.status, headers: responseHeaders });
  } catch {
    return problem(502, `The ${service} service is unavailable.`);
  }
}

function problem(status: number, detail: string) {
  return Response.json({ type: "about:blank", title: status === 404 ? "Not Found" : "Bad Gateway", status, detail }, {
    status,
    headers: { "content-type": "application/problem+json", "cache-control": "no-store" }
  });
}

export const GET = proxy;
export const HEAD = proxy;
export const OPTIONS = proxy;
export const POST = proxy;
export const PUT = proxy;
export const PATCH = proxy;
export const DELETE = proxy;
