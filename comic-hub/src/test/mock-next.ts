import { vi } from 'vitest';
import { useRouter, useSearchParams } from 'next/navigation';

export function mockRouter(overrides?: Partial<ReturnType<typeof useRouter>>) {
  const router = {
    push: vi.fn(),
    replace: vi.fn(),
    prefetch: vi.fn(),
    back: vi.fn(),
    refresh: vi.fn(),
    forward: vi.fn(),
    bfcacheId: 'test-bfcache-id',
  };
  vi.mocked(useRouter).mockReturnValue({ ...router, ...overrides });
  return router;
}

export function mockSearchParams(params?: Record<string, string>) {
  const searchParams = new URLSearchParams(params);
  vi.mocked(useSearchParams).mockReturnValue(searchParams as ReturnType<typeof useSearchParams>);
  return searchParams;
}
