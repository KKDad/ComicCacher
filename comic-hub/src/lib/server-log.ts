// Timing lines for server-side calls to the API, tagged with the request id the API logs too
// (RequestLoggingFilter), so a slow page load in the comics-ui log can be matched to its comics-api lines.
// Shared by route handlers, proxy.ts and server components, so not 'server-only'.

import { documentOperationName } from './graphql-operation';

export const REQUEST_ID_HEADER = 'x-request-id';

const VALID_REQUEST_ID = /^[A-Za-z0-9-]{1,64}$/;
const DEFAULT_SLOW_FETCH_MS = 500;

/** A new 8-hex-digit id, the same shape the API generates. */
export function newRequestId(): string {
  const bytes = crypto.getRandomValues(new Uint8Array(4));
  return Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('');
}

/** The incoming id when it's well formed (the API applies the same rule), otherwise a new one. */
export function resolveRequestId(incoming: string | null | undefined): string {
  return incoming && VALID_REQUEST_ID.test(incoming) ? incoming : newRequestId();
}

function slowFetchMs(): number {
  const configured = Number(process.env.SLOW_FETCH_MS);
  return Number.isFinite(configured) && configured > 0 ? configured : DEFAULT_SLOW_FETCH_MS;
}

/** The GraphQL operation name from a request body: `operationName`, or the name in the query text. */
export function operationName(body: BodyInit | null | undefined): string {
  if (typeof body !== 'string') return 'unknown';
  try {
    const parsed = JSON.parse(body);
    if (typeof parsed.operationName === 'string' && parsed.operationName) return parsed.operationName;
    return (typeof parsed.query === 'string' && documentOperationName(parsed.query)) || 'anonymous';
  } catch {
    return 'unknown';
  }
}

// Keeps the caller's header shape: graphql-request passes a Headers object, our own calls a plain object
function withRequestId(headers: HeadersInit | undefined, requestId: string): HeadersInit {
  if (headers instanceof Headers) {
    const copy = new Headers(headers);
    copy.set(REQUEST_ID_HEADER, requestId);
    return copy;
  }
  if (Array.isArray(headers)) return [...headers, [REQUEST_ID_HEADER, requestId]];
  return { ...headers, [REQUEST_ID_HEADER]: requestId };
}

// A timing line must never fail the call it measures: Next's console patching can throw outside a request scope
function logTiming(line: string, ms: number) {
  try {
    if (ms >= slowFetchMs()) {
      console.warn(`Slow request: ${line}`);
    } else {
      console.log(line);
    }
  } catch {
    // Nothing to do: the call itself succeeded
  }
}

/**
 * `fetch` to the GraphQL API that sends the request id and logs one timing line per call:
 * `graphql GetComic -> 200 in 42ms req=ab12cd34`, at WARN from SLOW_FETCH_MS (default 500).
 */
export async function timedGraphqlFetch(url: string, init: RequestInit, requestId: string): Promise<Response> {
  const headers = withRequestId(init.headers, requestId);
  const op = operationName(init.body);
  const start = performance.now();
  let outcome = 'failed';
  try {
    const res = await fetch(url, { ...init, headers });
    outcome = String(res.status);
    return res;
  } finally {
    const ms = Math.round(performance.now() - start);
    logTiming(`graphql ${op} -> ${outcome} in ${ms}ms req=${requestId}`, ms);
  }
}
