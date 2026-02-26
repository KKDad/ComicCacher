You are a senior Next.js architect. Design a clean, production-quality GraphQL + auth layer
for a Next.js 16 (App Router) application. Do not write implementation code — produce a
design document: file layout, module responsibilities, interface signatures, data flow
narrative, and a testing strategy.

## Stack (do not change)
- Next.js 16, React 19, TypeScript (strict mode)
- TanStack Query v5 (`@tanstack/react-query`)
- `graphql-request` v7 for the HTTP transport
- `graphql-codegen` with plugins: `typescript`, `typescript-operations`,
  `typescript-react-query` (reactQueryVersion: 5)
- Zustand v5 with the `persist` middleware (localStorage)
- `js-cookie` for SSR-visible cookie writes
- Backend: a single GraphQL endpoint at `process.env.NEXT_PUBLIC_GRAPHQL_ENDPOINT`
  (unauthenticated mutations: login, register, refreshToken, forgotPassword, resetPassword;
   authenticated queries/mutations require `Authorization: Bearer <token>`)

## Auth contract (backend is fixed — do not change)
- `login(input)` and `register(input)` return `{ token, refreshToken, username, displayName }`
- `refreshToken(refreshToken)` returns the same shape
- JWTs are stateless; logout is client-side only
- `validateToken` query returns `Boolean` (requires auth header)

## Design requirements

1. **Single token injection point.** There must be exactly one place in the codebase where
   the JWT is added to an outgoing GraphQL request. It must be impossible for a request to
   fire with a stale or missing token due to initialization order.

2. **No module-level singleton mutation for auth.**  The current code calls
   `graphqlClient.setHeader()` from multiple locations (store rehydration, provider mount,
   fetcher body). The new design must not share mutable header state across modules.

3. **Codegen compatibility.** The `typescript-react-query` plugin requires a `fetcher`
   export. Show how to configure `codegen.yml` so generated hooks use the auth-aware
   fetcher automatically, and where that fetcher lives.

4. **Zustand store responsibilities.** The store must persist tokens to localStorage and
   sync to a cookie for server-side middleware. It must NOT import from the GQL client
   module — dependencies flow one way only (GQL client reads from store, not vice versa).

5. **Hydration safety.** The UI must not flash an unauthenticated state on page reload.
   Describe how to block rendering until Zustand has rehydrated and the token is available,
   without adding a `_hasHydrated` boolean that must be threaded through every hook.

6. **Route protection.** Dashboard routes (`/(dashboard)/**`) redirect to `/login` if
   unauthenticated. This must work without client-side useEffect races. Next.js 16
   middleware (`middleware.ts` / `proxy.ts`) or server components are both acceptable;
   justify the choice.

7. **Testability.** A developer must be able to write a Vitest + React Testing Library test
   for a dashboard component that:
   - Mocks the GraphQL response without starting a server
   - Controls the auth token (authenticated vs unauthenticated)
   - Does not require mocking localStorage or Zustand internals

8. **Token refresh.** Describe the refresh strategy: when to refresh, where the interval
   lives, what happens if a query fires while a refresh is in flight (no thundering herd).

## Deliverable format
Produce:
A. A file tree showing every new/replaced file, annotated with one-line responsibilities.
B. For each file: the exports it provides and the imports it takes (no implementation bodies).
C. A prose data-flow walkthrough covering: cold page load → Zustand rehydrate →
   first authenticated query → token expiry → refresh → retry.
D. `codegen.yml` (complete, ready to copy).
E. A testing strategy section: what to mock, how to structure test helpers, one annotated
   example test outline for a component that fetches comics.
F. Any trade-offs or alternatives you considered and why you rejected them.
