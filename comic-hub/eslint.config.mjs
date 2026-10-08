import { defineConfig, globalIgnores } from "eslint/config";
import nextVitals from "eslint-config-next/core-web-vitals";
import nextTs from "eslint-config-next/typescript";

const eslintConfig = defineConfig([
  ...nextVitals,
  ...nextTs,
  // eslint-plugin-react's "detect" calls context.getFilename(), which ESLint 10
  // removed. Pin the version until the plugin supports ESLint 10.
  { settings: { react: { version: "19.3" } } },
  // Override default ignores of eslint-config-next.
  globalIgnores([
    // Default ignores of eslint-config-next:
    ".next/**",
    "out/**",
    "build/**",
    "next-env.d.ts",
    // Generated output
    "coverage/**",
    "src/generated/**",
  ]),
]);

export default eslintConfig;
