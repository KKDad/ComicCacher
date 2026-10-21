import 'server-only';
import { cookies } from 'next/headers';
import { JWT_COOKIE, GRAPHQL_ENDPOINT } from './constants';
import { getRequestId } from '@/lib/request-id';
import { documentOperationName } from '@/lib/graphql-operation';
import { timedGraphqlFetch } from '@/lib/server-log';

/**
 * Runs a GraphQL operation against the API as the signed-in user (the JWT cookie, when there is one).
 * Throws on a non-2xx status or any GraphQL error, so callers treat a partial result as a failure.
 */
export async function serverGraphql<TData>(query: string, variables?: Record<string, unknown>): Promise<TData> {
  const cookieStore = await cookies();
  const jwt = cookieStore.get(JWT_COOKIE)?.value;
  const requestId = await getRequestId();
  const operationName = documentOperationName(query);

  const res = await timedGraphqlFetch(
    GRAPHQL_ENDPOINT,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...(jwt && { Authorization: `Bearer ${jwt}` }) },
      body: JSON.stringify({ query, variables, ...(operationName && { operationName }) }),
    },
    requestId,
  );
  if (!res.ok) {
    throw new Error(`GraphQL request failed: ${res.status}`);
  }

  const json = await res.json();
  if (json.errors?.length) {
    throw new Error(json.errors[0]?.message ?? 'GraphQL error');
  }
  return json.data;
}
