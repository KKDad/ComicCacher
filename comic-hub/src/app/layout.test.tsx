import { render, screen } from '@testing-library/react';

vi.mock('next/font/google', () => ({
  Inter: () => ({ variable: '--font-primary' }),
  DynaPuff: () => ({ variable: '--font-display' }),
  JetBrains_Mono: () => ({ variable: '--font-mono' }),
}));

vi.mock('@/lib/providers', () => ({
  Providers: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="providers">{children}</div>
  ),
}));

describe('RootLayout', () => {
  it('renders Providers wrapping children', async () => {
    const { default: RootLayout } = await import('./layout');
    render(RootLayout({ children: <div>app content</div> }));
    expect(screen.getByTestId('providers')).toBeInTheDocument();
    expect(screen.getByText('app content')).toBeInTheDocument();
  });
});
