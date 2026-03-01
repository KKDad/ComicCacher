export function fetcher<TData, TVariables>(query: string, variables?: TVariables, headers?: RequestInit['headers']) {
  return async (): Promise<TData> => {
    const res = await fetch('/api/graphql', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...headers },
      body: JSON.stringify({ query, variables }),
    });

    if (!res.ok) {
      if (res.status === 401) {
        window.location.href = '/login';
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
