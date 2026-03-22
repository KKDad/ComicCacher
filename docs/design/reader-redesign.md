# Comic Reader Redesign

## Problem

The current reader (`/comics/[id]/[date]`) shows a single strip per page load with Previous/Next buttons. On mobile, the comic occupies ~15% of the screen — the rest is app chrome (header, bottom nav, card borders, padding, and empty space). There's no preloading, no gesture support, no cross-comic navigation, and no immersive reading mode.

## Goals

- **Mobile-first immersive reading** — the comic strip should dominate the screen
- **Vertical navigation** — scroll/swipe through dates within a comic
- **Cross-comic navigation** — explicit buttons on mobile, arrow keys / edge hover on desktop
- **Smart entry point** — start at last-read date, with awareness of unread strips
- **Jump controls** — first, last, random, date picker
- **Preloading** — adjacent strips load in the background for instant navigation

---

## Design Decisions

| Decision | Choice |
|----------|--------|
| Mobile layout | Full immersive — hide header/bottom nav, dark bg, tap for controls |
| Mobile date nav | Snap-per-strip swipe (up = newer, down = older) |
| Desktop date nav | Continuous scroll, newest at bottom (catch-up model, default) |
| Cross-comic nav (mobile) | Explicit prev/next comic buttons in controls overlay + reading-list drawer (button-triggered) |
| Cross-comic nav (desktop) | Arrow keys / edge hover arrows |
| Comic rotation | Preference: favorites only (default) or all comics |
| Entry point | Last-read date (fallback: latest strip) |
| Random scope | Within-series (default) + cross-comic random option |
| Jump controls | First / Last / Random / Date picker in header/overlay |
| Zoom | Pinch-to-zoom + double-tap toggle (2x / reset) |

---

## Mobile: Immersive Full-Screen Reader

### Visual Design

- **Dark background** (`bg-zinc-950`) — makes strips pop, signals "reading mode"
- **No app chrome** — header and bottom nav are hidden on entry
- **Edge-to-edge image** — strip fills horizontal space (minus safe area insets)
- **Centered vertically** — strip sits in the natural eye-rest position
- **Date label** — small, muted (`text-zinc-400`), positioned below the image
- **Gesture hint** — "swipe to navigate" shown on first visit, fades after use

### Controls Overlay (tap to toggle)

Tapping the image or empty space toggles translucent control bars:

```
┌──────────────────────────────────┐
│ ← Adam At Home              ⋯   │  translucent top bar
│                                  │
│        ▲ (newer strip)           │  tap target above image
│  ┌────────────────────────────┐  │
│  │                            │  │
│  │       comic strip          │  │
│  │       (full width)         │  │
│  │                            │  │
│  └────────────────────────────┘  │
│        ▼ (older strip)           │  tap target below image
│                                  │
│       ◀ Wed, Mar 18 ▶           │  date + prev/next strip arrows
│                                  │
│  ◄ prev comic    next comic ►   │  explicit cross-comic buttons
│  |◀  ▶|  🎲  📅  ☰             │  translucent bottom bar
│  first last rnd date reading-list│
└──────────────────────────────────┘
```

- **Tap targets for date nav:** Small chevrons above and below the strip (visible when controls are shown) provide tap-based date navigation as an alternative to swiping. Essential for motor-impaired users and discoverability.
- Controls auto-hide after a navigation action (swipe or tap), not on a timer. Tap-to-toggle is the primary show/dismiss mechanism.
- Overlays use absolute/fixed positioning — they do NOT push layout
- Overflow menu (⋯) contains: random any comic, comic info, reading list preference toggle

### Reading List Drawer

The reading-list button (☰) in the bottom bar opens a sheet/drawer showing the user's comic list (favorites or all, per preference). Tapping a comic navigates to it at its last-read date. This replaces the horizontal swipe gesture for cross-comic navigation, keeping the swipe axis reserved solely for date navigation.

> **Why not swipe-up-from-bottom-edge?** On iOS and Android, swiping up from the bottom edge triggers OS-level gestures (home indicator, app switcher). A button trigger avoids the conflict entirely.

### Gestures

| Gesture | Action |
|---------|--------|
| Swipe up | Next date (newer strip) — current strip animates out upward, new strip slides in |
| Swipe down | Previous date (older strip) — reverse animation |
| Tap | Toggle controls overlay |
| Pinch | Zoom in/out (critical for reading 4-panel strips on small screens) |
| Double-tap | Toggle 2x zoom (centered on tap point) / reset to 1x |

Swipe up/down always means date navigation — no context-dependent overloading. Reader dismissal uses the back button or the ← in the controls overlay, not a gesture.

### Loading State

When a swipe lands before the next strip's image has loaded, display a skeleton shimmer placeholder at the expected strip dimensions. The placeholder uses the strip's own aspect ratio from the `ComicStrip` metadata (width/height fields) when available; if unavailable, falls back to the comic's most common aspect ratio (typically consistent within a series). Once the image loads, it cross-fades in (150ms ease-out).

### Landscape Orientation

In landscape, the strip renders with `object-contain` constrained to the viewport. Control positioning respects `env(safe-area-inset-*)` so buttons don't overlap the notch or home indicator. The date label shifts to the side of the strip if vertical space is tight.

### Image Sizing & Aspect Ratios

Comic strips vary significantly in aspect ratio (single panels ~1:1, daily strips ~3:1, Sunday strips ~4:1+). Sizing rules:

- **Mobile:** Strip fills available width (`100vw - safe-area-insets`). Height is intrinsic — `object-contain` within the viewport. Very tall strips (portrait/editorial) are capped at `80vh` and become scrollable within their container via drag (not swipe, to avoid gesture conflict).
- **Desktop:** Strips render at intrinsic aspect ratio within the `max-w-3xl` column. No height cap — the scroll view accommodates varying heights naturally. Very narrow strips (single panels) are capped at `max-w-sm` and centered.
- **Skeleton placeholders** use the strip's own width/height metadata from `ComicStrip` when available. Within a series, aspect ratios are typically consistent (dailies vs Sundays), so the first loaded strip's ratio is a reasonable fallback for adjacent strips.

### Transitions

- **Up/down swipe:** vertical slide with slight fade (200ms ease-out). One strip snaps to center — no partial strips visible.
- **Entry:** zoom-in transition from the tapped comic card/list item
- **Exit (back button / ← tap):** reverse zoom-out back to the originating page (250ms ease-out)

---

## Desktop: Scroll-Based Multi-Strip View

### Visual Design

- **Normal app shell** — sidebar and header remain visible
- **Vertical strip column** — `max-w-3xl`, centered, strips stacked vertically
- **Minimal card chrome** — each strip has a date label (muted, above) and the image, separated by spacing (no heavy borders/shadows)
- **Follows app theme** — light/dark per user setting

### Layout

```
┌──────────────────────────────────────────────────────┐
│ ← Adam At Home              |◀  ▶|  🎲  📅         │  fixed header
├──────────────────────────────────────────────────────┤
│                                                      │
│   Tuesday, March 18, 2026                            │  ← last-read (start here)
│   ┌──────────────────────────────────────────┐       │
│   │         strip image                      │       │
│   └──────────────────────────────────────────┘       │
│                                                      │
│   Wednesday, March 19, 2026                          │  scroll ↓ to catch up
│   ┌──────────────────────────────────────────┐       │
│   │         strip image                      │       │
│   └──────────────────────────────────────────┘       │
│                                                      │
│   Thursday, March 20, 2026                           │  newest at bottom
│   ┌──────────────────────────────────────────┐       │
│   │         strip image                      │       │
│   └──────────────────────────────────────────┘       │
│                                                      │
│          ◄ prev comic    next comic ►                │
└──────────────────────────────────────────────────────┘
```

### Scroll Behavior

- **Newest at bottom** (default) — scroll down to catch up to today. The intended flow: land at last-read, scroll through missed strips chronologically, arrive at today's strip at the bottom, then flick left/right (or tap prev/next comic) to start the next comic's catch-up.
- **Start position** — scrolls to last-read date on mount
- **"X new strips" indicator** — floating Badge (bottom-center, above bottom bar), shows unread count when unread strips exist below last-read. Tap scrolls to the newest unread strip and dismisses the badge.
- **Infinite scroll** — load more strips as user approaches the top (older) or bottom (newer) edge
- **Last-read tracking** — IntersectionObserver detects which strip is centered in viewport, debounced 1s update via `updateLastRead` mutation
- **Scroll order preference** — users can switch to newest-at-top (reverse-chronological) via `displaySettings.readerScrollOrder`: `"catchup"` (default) or `"newest-first"`. Accessible from the desktop settings popover.

### Keyboard Navigation

| Key | Action |
|-----|--------|
| Arrow Left / Right | Previous / next comic in reading list |
| Arrow Up / Down | Smooth-scroll to previous / next strip |
| Home / End | Jump to first / last strip |
| R | Random strip |
| Escape | Exit reader, return to previous page |

### Cross-Comic Edge Arrows

- Subtle left/right arrows (~40px from viewport edge) appear on hover
- Translucent, with comic name tooltip
- Click navigates to adjacent comic in reading list at that comic's last-read date

---

## Header Toolbar (Desktop) / Overlay Bar (Mobile)

| Action | Icon | Behavior |
|--------|------|----------|
| Back | ← | Return to previous page |
| First strip | \|◀ | Jump to oldest available strip |
| Last strip | ▶\| | Jump to newest available strip |
| Random (series) | 🎲 | Random date from this comic's archive |
| Random (any) | 🎲 (overflow on mobile) | Random comic + random date |
| Date picker | 📅 | Calendar popup; available dates highlighted. Unavailable dates are tappable and show "No strip available" — they are NOT disabled, so users can confirm gaps rather than guessing. |
| Comic info | ℹ️ (overflow on mobile) | Navigate to comic detail page |

---

## Data & Preloading Strategy

### Strip Fetching

**Desktop (scroll view):**
1. Primary query: `stripWindow(center, before, after)` — fetches a centered window of strips in one round trip
2. Initial load: `stripWindow(lastRead, 2, 2)` — 5 strips centered on last-read
3. Prefetch: 3 strips ahead in scroll direction, triggered when 2nd-to-last loaded strip enters viewport
4. Each strip's `previous`/`next` fields provide adjacent dates without extra queries
5. TanStack Query caching: `staleTime: Infinity` for individual strip data (immutable once published); `staleTime: 5 * 60 * 1000` (5 min) for range/boundary queries so new strips are discovered

**Mobile (snap view):**
1. Initial load: 3 strips (current + 1 in each direction)
2. On each swipe, prefetch the next strip in that direction
3. Keep ≤5 strips in the DOM (current ± 2), unmount the rest

### Image Preloading

- When strip data is fetched, immediately start loading the image via `new Image().src = url`
- Desktop: preload next 2 images in scroll direction
- Mobile: preload 1 image in each direction

### Cross-Comic Data

- Fetch favorites list (or all comics) once on reader entry, cache for session
- Do NOT prefetch strips for adjacent comics — only fetch on actual navigation

### Last-Read Updates

- **Desktop:** debounced 1s via IntersectionObserver on the centered strip
- **Mobile:** immediate update on each snap landing
- Uses existing `updateLastRead` mutation

---

## Reading List & Preferences

### Cross-Comic Navigation Order

The reading list for cross-comic navigation is controlled by a preference:

- **"favorites"** (default): cycle through `favoriteComics` in alphabetical order
- **"all"**: cycle through all comics alphabetically

Stored in existing `displaySettings` JSON field:
```json
{
  "readerNavMode": "favorites",
  "readerScrollOrder": "catchup"
}
```

- `readerNavMode`: `"favorites"` (default) or `"all"`
- `readerScrollOrder`: `"catchup"` (newest at bottom, default) or `"newest-first"` (reverse-chronological)

Toggle accessible from the mobile overflow menu or a desktop settings popover.

### No Custom Ordering (v1)

Custom drag-to-reorder reading lists are deferred. Alphabetical within the selected set is sufficient for v1.

---

## Backend Changes

### 1. Random Strip Query (new)

```graphql
type Query {
  randomStrip(comicId: Int): ComicStrip!
}
```

- `comicId` provided → random date within `oldest..newest` range, snapped to nearest available strip
- `comicId` null → pick a random comic (from caller's favorites or all), then random date
- Returns full `ComicStrip` with `imageUrl`, `date`, `previous`, `next`

### 2. Strip Window Query (primary desktop query)

```graphql
type Comic {
  stripWindow(center: Date!, before: Int!, after: Int!): [ComicStrip!]!
}
```

Fetch a window of strips centered on a date — `before` older strips + the center strip + `after` newer strips, returned in chronological order. This is the primary query for the desktop scroll view — a single round trip loads the initial view (e.g., `stripWindow(center: lastRead, before: 2, after: 2)`) without needing two directional queries.

### 3. Multi-Strip Resolver (implement existing schema)

The schema already defines but no resolver exists:
```graphql
type Comic {
  strips(dates: [Date!]!): [ComicStrip!]!
}
```

Implement the resolver to batch-fetch multiple strips in one round trip — used for targeted prefetch when specific dates are known (e.g., pre-populating cache for a date-picker jump).

---

## Edge Cases & Error Handling

| Scenario | Behavior |
|----------|----------|
| **Swipe past first/last strip** | Rubber-band bounce animation (mobile), no-op with subtle flash on boundary strip (desktop). No toast — the bounce communicates the boundary. |
| **Network failure loading a strip** | Skeleton placeholder is replaced with a retry card: error icon + "Tap to retry" / "Failed to load — Retry" button. Does not block navigation to other strips. |
| **Empty comic (no strips)** | Reader shows centered empty state: comic name + "No strips available" + back button. Does not render skeleton or controls. |
| **Image decode failure** | Show broken-image icon at expected dimensions with "Image unavailable" text. Strip date label still visible. |
| **Navigating to a deleted/invalid comic ID** | Redirect to comics list with a toast: "Comic not found". |
| **Loss of connection mid-read** | Already-loaded strips remain viewable. Navigation to unloaded strips shows retry card. Reconnection triggers automatic retry of failed loads. |

---

## Component Structure

```
comic-hub/src/
├── components/reader/
│   ├── comic-reader.tsx            # Detects mobile/desktop, renders appropriate mode
│   ├── mobile-reader.tsx           # Full-screen immersive reader
│   ├── desktop-reader.tsx          # Scroll-based multi-strip view
│   ├── reader-controls.tsx         # Shared control bar (first/last/random/date-picker)
│   ├── reader-header.tsx           # Desktop fixed header / mobile overlay top bar
│   ├── strip-card.tsx              # Individual strip display (image + date)
│   ├── cross-comic-nav.tsx         # L/R navigation (hover arrows desktop, buttons mobile)
│   ├── reading-list-drawer.tsx     # Mobile swipe-up drawer for comic selection
│   ├── date-picker-popover.tsx     # Calendar with available dates highlighted
│   └── strip-skeleton.tsx          # Loading shimmer placeholder for strip images
├── hooks/
│   ├── use-reader.ts              # Core hook: strip data, navigation state, preloading
│   └── use-reading-list.ts         # Ordered list of comics for cross-comic navigation
```

### Files to Modify

| File | Change |
|------|--------|
| `comic-hub/src/app/(dashboard)/comics/[id]/[date]/page.tsx` | Replace with new reader or redirect |
| `comic-hub/src/app/(dashboard)/layout.tsx` | Support hiding shell for immersive mode |
| `comic-hub/src/graphql/operations/dashboard.graphql` | Add new queries |
| `comic-hub/src/stores/preferences-store.ts` | Add `readerNavMode` and `readerScrollOrder` fields with defaults |
| `comic-api/.../resolver/ComicResolver.java` | Add `randomStrip`, `strips`, `stripWindow` resolvers |
| `comic-api/.../resources/graphql/types/comic.graphql` | Add `randomStrip` query, `stripWindow` field |

---

## Accessibility

- **Reduced motion:** Respect `prefers-reduced-motion` — disable swipe animations, zoom transitions, and skeleton shimmer. Navigation still works, just without animation.
- **Tap alternatives to swipe:** All swipe-based navigation has equivalent tap targets (chevrons above/below strip, prev/next arrows flanking date). Users who cannot swipe reliably can navigate entirely via taps.
- **Icon button labels:** All icon-only buttons have `aria-label`: "Go to first strip", "Previous strip", "Next strip", "Go to last strip", "Random strip", "Open date picker", "Previous comic", "Next comic", "Open reading list", etc.
- **Live region for strip changes:** An `aria-live="polite"` region announces the current strip on navigation: e.g., "Adam At Home, March 18, 2026".
- **Focus management:** When the controls overlay is toggled visible, focus moves to the first interactive element. When dismissed, focus returns to the strip container. Escape closes the overlay.
- **Keyboard:** All controls are reachable via Tab. Focus-visible rings on all interactive elements. See [Keyboard Navigation](#keyboard-navigation) for shortcut keys.

---

## Implementation Order

> Accessibility (aria-labels, focus management, live regions, `prefers-reduced-motion`) is built into each step — not a separate pass at the end.

1. Backend: `randomStrip` query + `stripWindow` resolver + `strips` batch resolver
2. Core hooks: `use-reader.ts`, `use-reading-list.ts`
3. Desktop reader: scroll view, strip cards, header toolbar, keyboard nav, error/empty states
4. Mobile reader: immersive mode, swipe gestures, controls overlay (with tap-based date nav), reading-list drawer
5. Cross-comic navigation for both modes
6. Polish: transitions, pinch-to-zoom, double-tap zoom, date picker, "X new strips" indicator, loading skeletons
7. Layout integration: hide app shell on mobile reader entry
