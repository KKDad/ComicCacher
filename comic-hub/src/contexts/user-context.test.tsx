import { render, screen, renderHook } from '@testing-library/react';
import { UserProvider, useUser } from './user-context';
import { createMockUser } from '@/test/test-utils';

describe('UserContext', () => {
  it('useUser returns null when outside provider', () => {
    const { result } = renderHook(() => useUser());
    expect(result.current).toBeNull();
  });

  it('useUser returns user when inside provider', () => {
    const user = createMockUser();
    const { result } = renderHook(() => useUser(), {
      wrapper: ({ children }) => <UserProvider user={user}>{children}</UserProvider>,
    });
    expect(result.current).toEqual(user);
  });

  it('useUser returns null when provider has null user', () => {
    const { result } = renderHook(() => useUser(), {
      wrapper: ({ children }) => <UserProvider user={null}>{children}</UserProvider>,
    });
    expect(result.current).toBeNull();
  });

  it('renders children', () => {
    render(
      <UserProvider user={null}>
        <div>child content</div>
      </UserProvider>,
    );
    expect(screen.getByText('child content')).toBeInTheDocument();
  });
});
