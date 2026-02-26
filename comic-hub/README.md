# Comic Hub

Next.js web frontend for ComicCacher — browse, read, and manage comic strip subscriptions.

## Tech Stack

- **Framework:** Next.js 16 (App Router), React 19, TypeScript (strict)
- **Styling:** Tailwind CSS 4, Radix UI / shadcn components
- **Data Fetching:** TanStack Query v5, graphql-request v7, GraphQL Codegen
- **Forms:** react-hook-form + Zod
- **State:** Zustand v5 (theme & sidebar only — auth uses httpOnly cookies)
- **Testing:** Vitest, React Testing Library, MSW

## Prerequisites

- Node 22 (see `.nvmrc`)
- Backend GraphQL endpoint (default: `http://10.0.0.47:8087/graphql`)

## Getting Started

```bash
cp .env.example .env.local   # configure NEXT_PUBLIC_GRAPHQL_ENDPOINT
npm install
npm run dev                   # http://localhost:3000
```

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
│   │   └── logout/route.ts       # Clear cookies
│   ├── (auth)/                   # Login, register, forgot-password
│   └── (dashboard)/              # Dashboard, comics list, strip viewer
├── components/
│   ├── ui/                       # shadcn components
│   ├── comics/                   # ComicTile, FavoriteCard
│   ├── dashboard/                # Dashboard sections
│   └── layout/                   # Sidebar, Header, NavRail, MobileNav
├── contexts/
│   └── user-context.tsx          # Server-fetched user data
├── hooks/
│   ├── use-auth.ts               # Login/logout/register (calls API routes)
│   └── use-responsive-nav.ts     # Breakpoint detection
├── lib/
│   ├── auth/
│   │   ├── constants.ts          # Cookie names, endpoints, public paths
│   │   ├── session.ts            # getSession() — server-side user fetch
│   │   └── graphql-server.ts     # getAuthenticatedClient() — server-side
│   ├── graphql-client.ts         # Client fetcher for codegen (no auth logic)
│   ├── providers.tsx             # QueryClientProvider
│   └── validations/auth.ts       # Zod schemas
├── stores/
│   ├── theme-store.ts            # Light/dark/system theme
│   └── sidebar-store.ts          # Sidebar open/collapsed
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

## Docker

```bash
./build-docker.sh    # builds and tags the image
```

Exposes port 8080. Uses multi-stage Node 22 Alpine build with standalone output.
