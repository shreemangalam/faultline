# Browser SSE streaming fix

Browser QA exposed a proxy-only stream stall: requests with Accept-Encoding negotiated gzip and held the initial SSE frame in a compression buffer. Plain curl streams had previously passed, so they did not cover the browser path.

Added Cache-Control: no-cache, no-transform to the existing SSE response. Next.js now bypasses compression for the stream. Event names, payloads, transaction timing and debounced REST snapshot behavior are unchanged.

Verification: existing lifecycle streaming integration test passes with an additional header assertion. The rebuilt real proxy immediately returns stream-ready for curl --compressed without Content-Encoding: gzip.
