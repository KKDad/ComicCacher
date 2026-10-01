/** The name of the first query or mutation in a GraphQL document, or `undefined` for an anonymous one. */
export function documentOperationName(query: string): string | undefined {
  return /\b(?:query|mutation)\s+(\w+)/.exec(query)?.[1];
}
