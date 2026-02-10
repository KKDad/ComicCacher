import { GraphQLClient } from 'graphql-request';

const GRAPHQL_ENDPOINT = process.env.NEXT_PUBLIC_GRAPHQL_ENDPOINT || 'http://10.0.0.47:8087/graphql';

export const graphqlClient = new GraphQLClient(GRAPHQL_ENDPOINT, {
  headers: {
    'Content-Type': 'application/json',
  },
});

export function getAuthHeaders(token?: string | null) {
  if (!token) return {};
  return {
    Authorization: `Bearer ${token}`,
  };
}

export function setAuthToken(token: string) {
  graphqlClient.setHeader('Authorization', `Bearer ${token}`);
}

export function clearAuthToken() {
  graphqlClient.setHeader('Authorization', '');
}
