# Comic Hub Coding Standards

Next.js 16 / React 19 frontend. Server-rendered by default with TanStack Query for client cache. All backend access goes through `/api/*` route handlers — never directly from the browser.

## Stack

- Next.js 16.x (App Router, Turbopack dev server)
- React 19.x
- TypeScript 5
- Tailwind CSS 4 (zero-config, `@import "tailwindcss"` in `globals.css`)
- TanStack Query v5 + `graphql-request` for server-side GraphQL fetches
- Radix UI primitives + shadcn/ui generated components
- React Hook Form + Zod for forms
- Vitest + React Testing Library + MSW for tests

## App Router Rules

- **Server components by default.** Add `'use client'` only when you need state, effects, refs, browser APIs, or event handlers.
- Auth and session checks belong in **server layouts**, never in client components. The canonical example is `src/app/(dashboard)/layout.tsx`, which calls `getSession()` server-side and redirects unauthenticated users.
- Route groups: `(auth)` for login/registration, `(dashboard)` for the authenticated app, `(reader)` for the comic-reader experience.
- `proxy.ts` handles UX-only redirects. **It is not a security boundary.** Auth gates live in server layouts.

## Data Fetching & Auth

- All GraphQL traffic goes through `src/app/api/graphql/route.ts`. The route handler injects the JWT from httpOnly cookies, forwards to the backend, and rotates refresh tokens on 401 / `UNAUTHENTICATED` errors. Clone this pattern for any new authenticated route handler.
- Auth cookies: `httpOnly: true`, `secure: process.env.NODE_ENV === 'production'`, `sameSite: 'lax'`. Never expose tokens to client JavaScript.
- Session validation in server layouts uses `cache: 'no-store'` to ensure fresh JWT verification on every render.
- **Server Actions are intentionally NOT adopted.** Mutations route through `/api/*` handlers because token refresh logic lives there. Revisit if/when refresh moves to a centralized middleware layer.
- **Logout flow:** `/api/logout` calls the GraphQL `logout` mutation before clearing cookies. The backend sets the user's `tokensInvalidatedBefore` timestamp; the JWT filter and refresh path reject any token issued before the cutoff. The mutation is best-effort — if it fails, cookies are still cleared client-side.

## Error Boundaries & Loading States

The App Router requires explicit error and not-found handlers. The repo standard:
- `src/app/error.tsx` — root client error boundary (catches errors in any non-(global) route)
- `src/app/global-error.tsx` — last-resort boundary that owns its own `<html>`/`<body>`
- `src/app/not-found.tsx` — friendly 404
- `loading.tsx` per route segment that does server-side data fetching (already present for `(dashboard)` and `(reader)/comics/[id]/read`)

When you add a new route segment that fetches on the server, add a sibling `loading.tsx` for the streaming skeleton.

## Images

- Prefer `next/image` for any new image surface — automatic optimization, lazy loading, responsive sizing.
- `image-with-fallback.tsx` intentionally uses a vanilla `<img>` to support its fallback chain (cover → placeholder → broken-image SVG). **Don't "fix" this** — it's deliberate.
- Add `priority` to LCP images (typically the first comic strip on the reader page).

## Z-Index Rules

All z-index values MUST use the project's semantic tokens defined in `globals.css` (`@theme inline` block). Never use Tailwind's numeric defaults (`z-10`, `z-20`, `z-50`, etc.) for layout or overlay stacking.

| Token | Value | Use For |
|-------|-------|---------|
| `z-base` | 0 | Main content area (`<main>`) |
| `z-dropdown` | 100 | Non-portaled dropdowns within content flow |
| `z-sticky` | 200 | Sticky/fixed chrome: header, sidebar, nav rail |
| `z-fixed` | 300 | Fixed UI: mobile bottom nav |
| `z-modal-backdrop` | 400 | Modal/sheet overlays (darkened background) |
| `z-modal` | 500 | Modal/sheet content panels |
| `z-popover` | 600 | Radix-portaled floating elements: dropdown menus, selects, popovers |
| `z-tooltip` | 700 | Tooltips |
| `z-toast` | 800 | Toast notifications |

**Why portaled dropdowns use `z-popover` (600), not `z-dropdown` (100):**
Radix UI portals render at `document.body`. They must float above the header/sidebar (200) and work correctly when triggered from inside modals (500). `z-popover` (600) satisfies both constraints.

**Naming nuance:** CSS custom properties are `--z-*` (e.g., `--z-popover`). Tailwind v4's `@theme inline` block exports them as utility classes whose internal var names are `--z-index-*`. Use `z-popover` etc. as Tailwind utilities — they resolve through the theme correctly.

**When adding new shadcn/ui components:** The generated code uses Tailwind's `z-50` by default. Always replace `z-50` with the correct semantic token from the table above.

**Numeric z-index (`z-10`, `z-20`):** Acceptable only for local stacking within a component (e.g., badge over avatar). Never for global/cross-component layering.

## Testing

- Vitest + React Testing Library + MSW. Run with `npm test` or `npm run test:coverage`.
- Coverage thresholds enforced: 90% statements/lines/functions, 87% branches.
- Excluded from coverage: `src/components/ui/**` (generated shadcn), `src/generated/**` (GraphQL codegen), `src/types/**`.
- Mock backend calls with MSW handlers, not by stubbing `fetch`.

## GraphQL Codegen

- Schema lives in the backend. Run `npm run codegen` after backend schema changes; the watch mode is `npm run codegen:watch`.
- Generated TypeScript lands in `src/generated/` — never edit by hand, never commit changes that bypass codegen.
