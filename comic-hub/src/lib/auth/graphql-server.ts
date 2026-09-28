import 'server-only';
import { cookies } from 'next/headers';
import { GraphQLClient } from 'graphql-request';
import { JWT_COOKIE, GRAPHQL_ENDPOINT } from './constants';
import { getRequestId } from '@/lib/request-id';
import { timedGraphqlFetch } from '@/lib/server-log';

export async function getAuthenticatedClient(): Promise<GraphQLClient> {
  const cookieStore = await cookies();
  const jwt = cookieStore.get(JWT_COOKIE)?.value;
  const requestId = await getRequestId();

  return new GraphQLClient(GRAPHQL_ENDPOINT, {
    headers: jwt ? { Authorization: `Bearer ${jwt}` } : {},
    fetch: (url: RequestInfo | URL, init?: RequestInit) => timedGraphqlFetch(String(url), init ?? {}, requestId),
  });
}
