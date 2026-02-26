# Comics Hub - Web Frontend

Modern web application for caching, organizing, and viewing comic strips.

## Tech Stack

- **Framework**: Next.js 14 (App Router)
- **Styling**: Tailwind CSS + shadcn/ui
- **Data Fetching**: TanStack Query + graphql-request
- **Forms**: react-hook-form + zod
- **State**: Zustand (minimal - theme & sidebar only)
- **Icons**: Lucide React
- **Type Safety**: GraphQL Code Generator

## Project Structure

```
src/
├── app/                          # Next.js App Router
│   ├── (auth)/                   # Unauthenticated routes
│   │   ├── login/
│   │   ├── register/
│   │   └── forgot-password/
│   ├── (dashboard)/              # Authenticated routes
│   │   ├── comics/
│   │   ├── metrics/
│   │   ├── retrieval-status/
│   │   └── preferences/
│   ├── layout.tsx                # Root layout (fonts, providers)
│   └── globals.css               # Design tokens + Tailwind
├── components/
│   ├── ui/                       # shadcn components
│   ├── comics/                   # Comic-specific components
│   ├── layout/                   # Sidebar, Header, Navigation
│   ├── forms/                    # Form components
│   └── feedback/                 # EmptyState, ErrorBanner
├── lib/
│   ├── graphql-client.ts         # GraphQL client setup
│   ├── providers.tsx             # React Query provider
│   └── utils.ts                  # cn() helper
├── stores/
│   ├── theme-store.ts            # Theme state (light/dark/system)
│   └── sidebar-store.ts          # Sidebar state
├── generated/                    # GraphQL codegen output
└── graphql/                      # .graphql query files
```

## Getting Started

### Prerequisites

- Node.js 18+
- npm or yarn
- Running Comics Hub backend at `http://comics.stapledon.local/graphql`

### Installation

```bash
# Install dependencies
npm install

# Start development server
npm run dev
```

Visit [http://localhost:3000](http://localhost:3000)

### GraphQL Code Generation

Generate TypeScript types from GraphQL schema:

```bash
# One-time generation
npm run codegen

# Watch mode (regenerate on schema changes)
npm run codegen:watch
```

## Design System

### Colors

- **Canvas**: `bg-canvas` - App background (#F9FAFB light, #111827 dark)
- **Surface**: `bg-surface` - Cards, panels (#FFFFFF light, #1F2937 dark)
- **Ink**: `text-ink` - Primary text (#1F2937 light, #F9FAFB dark)
- **Primary**: `text-primary` - Comic Blue (#3EAEFF light, #60A5FA dark)

### Typography

- **Primary**: Inter (body text, UI elements)
- **Display**: DynaPuff Bold (h1, branding)
- **Mono**: JetBrains Mono (code)

### Component Library

shadcn/ui components are installed and customized with Comics Hub design tokens:

```bash
# Add new components
npx shadcn@latest add [component-name]
```

Installed components: button, input, select, checkbox, radio-group, switch, dialog, sheet, sonner, tooltip, dropdown-menu, skeleton, avatar, form, label, card, badge, separator

## Development Guidelines

### Adding GraphQL Queries

1. Create `.graphql` file in `src/graphql/`:

```graphql
# src/graphql/comics.graphql
query GetComics($first: Int!, $after: String) {
  comics(first: $first, after: $after) {
    edges {
      node {
        id
        title
        imageUrl
      }
    }
    pageInfo {
      hasNextPage
      endCursor
    }
  }
}
```

2. Run codegen:

```bash
npm run codegen
```

3. Use generated hook:

```tsx
import { useGetComicsQuery } from '@/generated/graphql';

function ComicsList() {
  const { data, isLoading } = useGetComicsQuery({ first: 10 });
  // ...
}
```

### Theme Toggle

```tsx
import { useThemeStore } from '@/stores/theme-store';

function ThemeToggle() {
  const { theme, setTheme } = useThemeStore();

  return (
    <button onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')}>
      Toggle Theme
    </button>
  );
}
```

### Forms with Validation

```tsx
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

const schema = z.object({
  email: z.string().email(),
  password: z.string().min(8),
});

function LoginForm() {
  const form = useForm({
    resolver: zodResolver(schema),
    mode: 'onBlur', // Validate on blur per spec
  });

  // ...
}
```

## Environment Variables

Create `.env.local`:

```env
NEXT_PUBLIC_GRAPHQL_ENDPOINT=http://comics.stapledon.local/graphql
NEXT_PUBLIC_API_BASE_URL=http://comics.stapledon.local/api/v1
```

## Build & Deploy

```bash
# Production build
npm run build

# Start production server
npm start
```

## Documentation

Full specifications in `/docs/2026-ui-refactor/`:

- `component_specs.md` - UI component specifications
- `visual_style_guide.md` - Design system details
- `design_tokens.css` - Complete token reference
- `*_screens.md` - Screen-by-screen layouts and queries
- `TODO.md` - Implementation roadmap

## Future Mobile Support

When mobile becomes a priority:

- **Option A**: Expo with shared logic (TanStack Query, Zustand, Zod)
- **Option B**: Capacitor wrapper
- **Option C**: PWA (already Next.js native)

Domain logic is kept portable for cross-platform reuse.
