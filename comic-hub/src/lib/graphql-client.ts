import type { TypedDocumentString } from '@/generated/graphql';
import { documentOperationName } from '@/lib/graphql-operation';
import { loginPath } from '@/lib/safe-redirect';

export function fetcher<TData, TVariables>(
  query: string | TypedDocumentString<unknown, unknown>,
  variables?: TVariables,
  headers?: RequestInit['headers'],
) {
  return async (): Promise<TData> => {
    const text = query.toString();
    // The API logs requests by operation name, and the generated documents don't carry it separately
    const operationName = documentOperationName(text);
    const res = await fetch('/api/graphql', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...headers },
      body: JSON.stringify({ query: text, variables, ...(operationName && { operationName }) }),
    });

    if (!res.ok) {
      if (res.status === 401) {
        // A full page load, not router.push, so no client state from the expired session survives
        const { pathname, search, origin } = window.location;
        window.location.assign(new URL(loginPath(pathname + search), origin));
      }
      throw new Error(`Request failed: ${res.status}`);
    }

    const json = await res.json();
    if (json.errors && !json.data) {
      throw new Error(json.errors[0]?.message ?? 'GraphQL error');
    }
    return json.data;
  };
}
