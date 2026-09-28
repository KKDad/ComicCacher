import 'server-only';
import { headers } from 'next/headers';
import { newRequestId, REQUEST_ID_HEADER, resolveRequestId } from './server-log';

/**
 * The id proxy.ts gave this page render, so every API call the render makes shares it. Outside a request
 * (headers() throws there) each call gets its own id.
 */
export async function getRequestId(): Promise<string> {
  try {
    return resolveRequestId((await headers()).get(REQUEST_ID_HEADER));
  } catch {
    return newRequestId();
  }
}
