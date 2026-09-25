# Comic Hub

Next.js web frontend for ComicCacher — browse, read, and manage comic strip subscriptions.

## Tech Stack

- **Framework:** Next.js 16 (App Router), React 19, TypeScript (strict)
- **Styling:** Tailwind CSS 4, Radix UI / shadcn components
- **Data Fetching:** TanStack Query v5, graphql-request v7, GraphQL Codegen
- **Forms:** react-hook-form + Zod
- **State:** Zustand v5 (display preferences — auth uses httpOnly cookies, theme via next-themes)
- **Testing:** Vitest, React Testing Library, MSW (tests run in `America/Toronto` so date bugs show up on UTC CI)

## Prerequisites

- Node 24 (see `.nvmrc`)
- Backend GraphQL endpoint (default: `http://10.0.0.47:8087/graphql`)

## Getting Started

To run against the dev API, use the script from the repo root. It picks the Node version from `.nvmrc`, installs dependencies if needed, checks the API is up and starts `next dev` on http://localhost:3000:

```bash
./utils/dev-ui.sh                                        # dev API on portainer
./utils/dev-ui.sh --api http://localhost:8888/graphql    # another endpoint, e.g. via tunnel-to-prod-api.sh
```

To run by hand:

```bash
cp .env.example .env.local   # configure NEXT_PUBLIC_GRAPHQL_ENDPOINT
npm install
npm run dev                   # http://localhost:3000
```

For a test login on the dev instance, see "Dev Tokens" in [`docs/api/overview.md`](../docs/api/overview.md).

## Scripts

| Command | Description |
|---------|-------------|
| `npm run dev` | Start dev server |
| `npm run build` | Production build (standalone output) |
| `npm run codegen` | Generate TypeScript types + hooks from GraphQL schema |
| `npm run codegen:watch` | Codegen in watch mode |
| `npm test` | Run tests (Vitest) |
| `npm run test:ui` | Vitest with browser UI |
| `npm run test:coverage` | Coverage report |

## Project Structure

```
src/
├── app/
│   ├── api/
│   │   ├── graphql/route.ts      # Server proxy: cookie → Bearer, token refresh
│   │   ├── login/route.ts        # Login → set httpOnly cookies
│   │   ├── register/route.ts     # Register → set httpOnly cookies
│   │   ├── logout/route.ts       # Revoke tokens, clear cookies
│   │   ├── forgot-password/      # Request a password reset email
│   │   ├── reset-password/       # Set a new password from the emailed token
│   │   └── health/route.ts       # Container health check
│   ├── (auth)/                   # Login, register, forgot/reset password
│   ├── (dashboard)/              # Dashboard, comics, preferences, batch jobs, metrics, retrieval status
│   └── (reader)/                 # Strip reader (comics/[id]/read) and date-grid reader (read)
├── components/
│   ├── ui/                       # shadcn components
│   ├── auth/                     # ErrorBanner
│   ├── batch-jobs/               # JobCard, LogViewer
│   ├── comics/                   # ComicTile, FavoriteCard
│   ├── dashboard/                # Dashboard sections
│   ├── grid-reader/              # Date-column grid reader, lightbox
│   ├── reader/                   # Single-comic reader (desktop scroll, mobile snap)
│   └── layout/                   # Sidebar, Header, NavRail, MobileNav
├── contexts/
│   └── user-context.tsx          # Server-fetched user data
├── hooks/
│   ├── use-auth.ts               # Login/logout/register (calls API routes)
│   ├── use-all-comics.ts         # Follows the comics cursor past the 50-per-page cap
│   ├── use-reader.ts             # Strip reader state (infinite stripWindow query)
│   ├── use-grid-reader.ts        # Grid reader state
│   ├── use-reading-list.ts       # Reader's reading list
│   └── use-responsive-nav.ts     # Breakpoint detection
├── lib/
│   ├── auth/
│   │   ├── constants.ts          # Cookie names, endpoints, public paths
│   │   ├── session.ts            # getSession() — server-side user fetch
│   │   └── graphql-server.ts     # getAuthenticatedClient() — server-side
│   ├── graphql-client.ts         # Client fetcher for codegen (no auth logic)
│   ├── date-utils.ts             # Strip dates (YYYY-MM-DD) as local calendar days
│   ├── gravatar.ts               # Gravatar URL generation
│   ├── preferences-defaults.ts   # Default preference values
│   ├── providers.tsx             # QueryClientProvider
│   ├── roles.ts                  # Role utilities (USER, OPERATOR, ADMIN)
│   ├── safe-redirect.ts          # Rejects off-site ?from= redirects
│   ├── utils.ts                  # General utilities
│   └── validations/auth.ts       # Zod schemas
├── stores/
│   └── preferences-store.ts      # User display preferences
├── graphql/operations/           # .graphql query/mutation files
├── generated/graphql.ts          # Codegen output (do not edit)
├── types/auth.ts                 # Auth type definitions
└── proxy.ts                      # UX-only route redirect (not a security boundary)
```

## Auth Architecture

Tokens are stored in **httpOnly cookies** (never accessible to JavaScript):

1. `/api/login` and `/api/register` call the backend and set httpOnly cookies
2. Client components fetch data via generated hooks → fetcher POSTs to `/api/graphql`
3. The server proxy reads the cookie, attaches the Bearer header, and forwards to the backend
4. On 401, the proxy attempts a token refresh server-side before returning an error
5. `proxy.ts` handles UX redirects (unauthenticated → `/login`) but is not a security boundary
6. Server layouts check the session with `getSession()`, which doesn't refresh yet: once the 15-minute access token expires, a full page load goes to `/login` (see `TODO.md`)

## Docker

```bash
./build-docker.sh    # builds and tags the image
```

Exposes port 8080. Uses multi-stage Node 24 Alpine build with standalone output.
