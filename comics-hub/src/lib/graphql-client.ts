import { GraphQLClient } from 'graphql-request';

const GRAPHQL_ENDPOINT = process.env.NEXT_PUBLIC_GRAPHQL_ENDPOINT || 'http://10.0.0.47:8087/graphql';

export const graphqlClient = new GraphQLClient(GRAPHQL_ENDPOINT, {
  headers: {
    'Content-Type': 'application/json',
  },
});

// Read the persisted auth token directly from localStorage at request time.
// This avoids any timing dependency on setAuthToken() being called before
// queries fire, and survives page refreshes automatically.
function getPersistedToken(): string | null {
  if (typeof window === 'undefined') return null;
  try {
    const raw = localStorage.getItem('auth-storage');
    if (!raw) return null;
    const parsed = JSON.parse(raw);
    return parsed?.state?.tokens?.accessToken ?? null;
  } catch {
    return null;
  }
}

export function setAuthToken(token: string) {
  graphqlClient.setHeader('Authorization', `Bearer ${token}`);
}

export function clearAuthToken() {
  graphqlClient.setHeader('Authorization', '');
}

export function fetcher<TData, TVariables>(query: string, variables?: TVariables): () => Promise<TData> {
  return () => {
    const token = getPersistedToken();
    const authHeaders = token ? { Authorization: `Bearer ${token}` } : undefined;
    return graphqlClient.request<TData>(query, variables as Record<string, unknown>, authHeaders);
  };
}
