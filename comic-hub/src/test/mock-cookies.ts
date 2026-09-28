import { cookies } from 'next/headers';

type CookieStore = Awaited<ReturnType<typeof cookies>>;

/**
 * Makes the mocked `cookies()` from `next/headers` return a store holding `values`.
 * The test file must `vi.mock('next/headers')`. Only `get` is implemented.
 */
export function mockCookieStore(values: Record<string, string> = {}) {
  const store = {
    get: vi.fn((name: string) => {
      const value = values[name];
      return value ? { name, value } : undefined;
    }),
  };
  vi.mocked(cookies).mockResolvedValue(store as unknown as CookieStore);
  return store;
}
