# Comic Hub Coding Standards

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

**When adding new shadcn/ui components:** The generated code uses Tailwind's `z-50` by default. Always replace `z-50` with the correct semantic token from the table above.

**Numeric z-index (`z-10`, `z-20`):** Acceptable only for local stacking within a component (e.g., badge over avatar). Never for global/cross-component layering.
