import { readdirSync, readFileSync } from 'fs';
import { join } from 'path';
import { describe, expect, it } from 'vitest';

/**
 * Enforces the project's semantic z-index scale in all UI components.
 *
 * The project defines semantic z-index tokens in globals.css (@theme inline):
 *   z-base(0), z-dropdown(100), z-sticky(200), z-fixed(300),
 *   z-modal-backdrop(400), z-modal(500), z-popover(600), z-tooltip(700), z-toast(800)
 *
 * Tailwind's numeric defaults (z-10, z-20, z-30, z-40, z-50) must NOT be used
 * for global/cross-component stacking in components/ui/. New shadcn components
 * ship with z-50 by default and must be converted to the semantic token.
 *
 * Exception: z-10 is allowed for local stacking within a single component
 * (e.g., avatar badge layering).
 */

const SEMANTIC_TOKENS = [
  'z-base',
  'z-dropdown',
  'z-sticky',
  'z-fixed',
  'z-modal-backdrop',
  'z-modal',
  'z-popover',
  'z-tooltip',
  'z-toast',
];

// Tailwind numeric z-index classes that indicate a missing semantic token.
// z-0 is equivalent to z-base and is acceptable. z-10 is allowed for local stacking.
const FORBIDDEN_PATTERN = /\bz-(20|30|40|50)\b/g;

const UI_DIR = join(__dirname);

describe('z-index enforcement', () => {
  const uiFiles = readdirSync(UI_DIR)
    .filter((f) => f.endsWith('.tsx'))
    .map((f) => ({ name: f, path: join(UI_DIR, f) }));

  it.each(uiFiles)(
    '$name must use semantic z-index tokens, not Tailwind numeric defaults',
    ({ name, path }) => {
      const content = readFileSync(path, 'utf-8');
      const matches: string[] = [];

      let match;
      const lines = content.split('\n');
      for (let i = 0; i < lines.length; i++) {
        const line = lines[i];
        FORBIDDEN_PATTERN.lastIndex = 0;
        while ((match = FORBIDDEN_PATTERN.exec(line)) !== null) {
          matches.push(`  Line ${i + 1}: found "${match[0]}" — use a semantic token instead (${SEMANTIC_TOKENS.join(', ')})`);
        }
      }

      expect(matches, `${name} uses numeric z-index classes:\n${matches.join('\n')}`).toHaveLength(0);
    },
  );
});
