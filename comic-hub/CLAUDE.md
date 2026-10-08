# Comic Hub Coding Standards

Next.js 16 / React 19 frontend. Server-rendered by default with TanStack Query for client cache. All backend access goes through `/api/*` route handlers — never directly from the browser.

## Stack

- Next.js 16.x (App Router, Turbopack dev server)
- React 19.x
- TypeScript 6 (7 waits for typescript-eslint support)
- Tailwind CSS 4 (zero-config, `@import "tailwindcss"` in `globals.css`)
- TanStack Query v5 + `graphql-request` for server-side GraphQL fetches
- Radix UI primitives + shadcn/ui generated components
- React Hook Form + Zod for forms
- Vitest + React Testing Library for tests

## App Router Rules

- **Server components by default.** Add `'use client'` only when you need state, effects, refs, browser APIs, or event handlers.
- Auth and session checks belong in **server layouts**, never in client components. The canonical example is `src/app/(dashboard)/layout.tsx`, which calls `getSession()` server-side and redirects unauthenticated users.
- Route groups: `(auth)` for login/registration, `(dashboard)` for the authenticated app, `(reader)` for the comic-reader experience. Inside `(dashboard)`, `(operations)` holds the operator pages (metrics, retrieval status, batch jobs): its server layout 404s anyone without OPERATOR or ADMIN, as `sources/layout.tsx` does for `/sources`. Put a new operator page there.
- `src/proxy.ts` refreshes an expired access token before a page renders (server components can't set cookies), forwarding the new cookies to the render and the browser. It never redirects: auth gates live in server layouts, and route handlers own the auth boundary and their own refresh for `/api/*`, which the proxy skips.

## Data Fetching & Auth

- Every operation lives in `src/graphql/operations/*.graphql`; route handlers send the generated document (`LoginDocument.toString()`), never an inline query string.
- All GraphQL traffic goes through `src/app/api/graphql/route.ts`. The route handler injects the JWT from httpOnly cookies, forwards to the backend, and rotates refresh tokens on 401 / `UNAUTHENTICATED` errors, using the shared helpers in `src/lib/auth/tokens.ts`. Clone this pattern for any new authenticated route handler.
- Auth cookies: `httpOnly: true`, `secure: process.env.NODE_ENV === 'production'`, `sameSite: 'lax'`. Never expose tokens to client JavaScript.
- Session validation in server layouts uses `cache: 'no-store'` to ensure fresh JWT verification on every render.
- **Server Actions are intentionally NOT adopted.** Mutations route through `/api/*` handlers because token refresh logic lives there. `proxy.ts` refreshes only for page renders, so this still holds.
- **Logout flow:** `/api/logout` calls the GraphQL `logout` mutation before clearing cookies. The backend sets the user's `tokensInvalidatedBefore` timestamp; the JWT filter and refresh path reject any token issued before the cutoff. The mutation is best-effort — if it fails, cookies are still cleared client-side.

## Layout, Theme & Titles

- **Responsive layout is CSS.** `DashboardShell` renders the sidebar, nav rail and mobile nav together and Tailwind breakpoints (`md`, `lg`) pick one, so the server HTML is already right. Use `useResponsiveNav` only when a component must render a different tree per device (the readers); it returns `null` on the server and during hydration, so render a skeleton for `null` rather than guess.
- **Viewport height:** use `dvh` (`h-dvh`, `min-h-dvh`), not `h-screen`, so mobile browser bars don't cut pages off. `viewportFit: "cover"` in the root layout makes `env(safe-area-inset-*)` work.
- **Theme:** the inline script in the root layout sets the `<html>` class before paint and `ThemeSync` owns it after hydration (`src/lib/theme.ts`). Read the theme from the preferences store; don't set it on `document.documentElement` anywhere else.
- **Colour tokens:** text tokens must hold 4.5:1 on `canvas` and `surface` in both themes (WCAG AA); check new pairs with a contrast calculation. A `--color-*` token defined only in `:root` generates no Tailwind class: add it to the `@theme inline` block too.
- **Page titles** use the metadata API with the root template `%s · Comics Hub`. Client pages get theirs from a sibling `layout.tsx` exporting `metadata`; comic pages use `generateMetadata` with `comicTitle()`.
- **Strip dates** are `YYYY-MM-DD` calendar days: parse and format them with `src/lib/date-utils.ts`, never `new Date(date)`, which reads them as UTC midnight and shows the previous day west of Greenwich. Tests run in `America/Toronto` to catch this.

## Error Boundaries & Loading States

The App Router requires explicit error and not-found handlers. The repo standard:
- `src/app/error.tsx` — root client error boundary (catches errors in any non-(global) route)
- `src/app/global-error.tsx` — last-resort boundary that owns its own `<html>`/`<body>`
- `src/app/not-found.tsx` — friendly 404
- `loading.tsx` per route segment that does server-side data fetching (already present for `(dashboard)` and `(reader)/comics/[id]/read`)

When you add a new route segment that fetches on the server, add a sibling `loading.tsx` for the streaming skeleton.

## Images

- **`next/image` for every image.** No `<img>` and no `no-img-element` disables; lint fails on either.
- Comic images are `/api/v1/comics/**` paths, allowed by `images.localPatterns` in `next.config.ts`. The optimizer fetches them through the `/api/v1` rewrite without the user's cookies, which works because the backend serves them to anyone.
- Give every image `sizes`. In a box that sets the aspect ratio, use `fill` (the strips: `STRIP_SIZES` in `components/reader/strip-sizes.ts`); otherwise pass the strip's `width` / `height`, falling back to 900×300.
- For the LCP strip, set `loading="eager"` and `fetchPriority="high"`, not `preload`: the reader has several candidate LCP images, where the Next 16 docs recommend against `preload`.
- `ImageWithFallback` shows its fallback text when there is no image or it fails to load.

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

**Naming nuance:** the scale is defined once, as `--z-index-*` in the `@theme inline` block, which is what generates the `z-popover` etc. utilities. There are no separate `--z-*` custom properties.

**When adding new shadcn/ui components:** The generated code uses Tailwind's `z-50` by default. Always replace `z-50` with the correct semantic token from the table above.

**Numeric z-index (`z-10`, `z-20`):** Acceptable only for local stacking within a component (e.g., badge over avatar). Never for global/cross-component layering.

## Testing

- Vitest + React Testing Library. Run with `npm test` or `npm run test:coverage`.
- Coverage thresholds enforced: 90% statements/lines/functions, 87% branches.
- Excluded from coverage: `src/components/ui/**` (generated shadcn), `src/generated/**` (GraphQL codegen), `src/types/**`.
- Mock backend calls by stubbing `fetch` (route handlers) or `vi.mock('@/generated/graphql')` (components).
- No `as any` in tests (lint fails on it). Mock generated hooks with `mockQueryResult` / `mockInfiniteQueryResult` / `mockMutationResult`, and fire a component's mutation callbacks with `captureMutation`, all from `src/test/mock-query.ts`. For cookies, the router, search params and fixtures use `mockCookieStore` (`src/test/mock-cookies.ts`), `mockRouter` / `mockSearchParams` (`src/test/mock-next.ts`) and `mockComic` (`src/test/test-utils.tsx`).
- `next/image` points `src` at the optimizer: read the original URL with `imageSrc(img)` from `src/test/test-utils.tsx`.

## GraphQL Codegen

- Schema lives in the backend. Run `npm run codegen` after backend schema changes; the watch mode is `npm run codegen:watch`.
- Generated TypeScript lands in `src/generated/` — never edit by hand, never commit changes that bypass codegen. CI fails if `src/generated` differs from a fresh `npm run codegen`.
- Only `typescript-operations` runs, not the `typescript` plugin, so there are no schema object types: derive them from operation results (see `src/types/batch-jobs.ts`). Enums are `const` objects with a matching union type, and the `Date` / `DateTime` scalars are `string`.

<!-- BEGIN:nextjs-agent-rules -->

# This is NOT the Next.js you know

This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `node_modules/next/dist/docs/` (resolved from this file's directory; in monorepos the `next` package may not be visible from the repo root) before writing any code. Heed deprecation notices.

This block is written and re-added by `next dev` — verify at `node_modules/next/dist/server/lib/generate-agent-files.js`. Removing it from a diff only re-creates the uncommitted change; committing it with your work keeps the tree clean.

<!-- END:nextjs-agent-rules -->
