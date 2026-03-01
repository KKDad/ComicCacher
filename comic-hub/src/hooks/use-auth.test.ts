import { renderHook, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useLogout } from './use-auth';
import { useRouter } from 'next/navigation';
import { createElement, type ReactNode } from 'react';

const mockRouter = {
  push: vi.fn(),
  replace: vi.fn(),
  prefetch: vi.fn(),
  back: vi.fn(),
  refresh: vi.fn(),
  forward: vi.fn(),
};

function createWrapper() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return {
    queryClient,
    wrapper: ({ children }: { children: ReactNode }) =>
      createElement(QueryClientProvider, { client: queryClient }, children),
  };
}

describe('useLogout', () => {
  beforeEach(() => {
    vi.mocked(useRouter).mockReturnValue(mockRouter);
    vi.spyOn(global, 'fetch').mockResolvedValue(new Response(JSON.stringify({ success: true })));
  });

  afterEach(() => {
    vi.restoreAllMocks();
    Object.values(mockRouter).forEach((fn) => fn.mockClear());
  });

  it('calls /api/logout with POST', async () => {
    const { wrapper } = createWrapper();
    const { result } = renderHook(() => useLogout(), { wrapper });

    await act(async () => {
      await result.current.logout();
    });

    expect(global.fetch).toHaveBeenCalledWith('/api/logout', { method: 'POST' });
  });

  it('clears the QueryClient', async () => {
    const { wrapper, queryClient } = createWrapper();
    const clearSpy = vi.spyOn(queryClient, 'clear');
    const { result } = renderHook(() => useLogout(), { wrapper });

    await act(async () => {
      await result.current.logout();
    });

    expect(clearSpy).toHaveBeenCalled();
  });

  it('redirects to /login', async () => {
    const { wrapper } = createWrapper();
    const { result } = renderHook(() => useLogout(), { wrapper });

    await act(async () => {
      await result.current.logout();
    });

    expect(mockRouter.push).toHaveBeenCalledWith('/login');
  });

  it('calls router.refresh', async () => {
    const { wrapper } = createWrapper();
    const { result } = renderHook(() => useLogout(), { wrapper });

    await act(async () => {
      await result.current.logout();
    });

    expect(mockRouter.refresh).toHaveBeenCalled();
  });

  it('sets isLoggingOut during logout', async () => {
    const { wrapper } = createWrapper();
    const { result } = renderHook(() => useLogout(), { wrapper });

    expect(result.current.isLoggingOut).toBe(false);

    let resolveLogout: () => void;
    vi.mocked(global.fetch).mockReturnValue(
      new Promise((resolve) => {
        resolveLogout = () => resolve(new Response(JSON.stringify({ success: true })));
      }),
    );

    let logoutPromise: Promise<void>;
    act(() => {
      logoutPromise = result.current.logout();
    });

    expect(result.current.isLoggingOut).toBe(true);

    await act(async () => {
      resolveLogout!();
      await logoutPromise;
    });

    expect(result.current.isLoggingOut).toBe(false);
  });

  it('resets isLoggingOut even on fetch failure', async () => {
    vi.mocked(global.fetch).mockRejectedValue(new Error('Network error'));
    const { wrapper } = createWrapper();
    const { result } = renderHook(() => useLogout(), { wrapper });

    await act(async () => {
      try {
        await result.current.logout();
      } catch {
        // expected — logout doesn't catch fetch errors
      }
    });

    expect(result.current.isLoggingOut).toBe(false);
  });
});
