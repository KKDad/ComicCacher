import { cookies } from 'next/headers';
import { GraphQLClient } from 'graphql-request';
import { JWT_COOKIE, GRAPHQL_ENDPOINT } from './constants';

export async function getAuthenticatedClient(): Promise<GraphQLClient> {
  const cookieStore = await cookies();
  const jwt = cookieStore.get(JWT_COOKIE)?.value;

  return new GraphQLClient(GRAPHQL_ENDPOINT, {
    headers: jwt ? { Authorization: `Bearer ${jwt}` } : {},
  });
}
