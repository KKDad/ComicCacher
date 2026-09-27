import { render, screen } from '@testing-library/react';

vi.mock('next/font/google', () => ({
  Figtree: () => ({ variable: '--font-primary' }),
  Bricolage_Grotesque: () => ({ variable: '--font-heading' }),
  DynaPuff: () => ({ variable: '--font-display' }),
}));

vi.mock('@/lib/providers', () => ({
  Providers: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="providers">{children}</div>
  ),
}));

describe('RootLayout', () => {
  it('matches the browser chrome to the canvas and allows safe-area insets', async () => {
    const { viewport } = await import('./layout');
    expect(viewport.viewportFit).toBe('cover');
    expect(viewport.themeColor).toEqual([
      { media: '(prefers-color-scheme: light)', color: '#F2ECDF' },
      { media: '(prefers-color-scheme: dark)', color: '#151417' },
    ]);
  });

  it('renders Providers wrapping children', async () => {
    const { default: RootLayout } = await import('./layout');
    render(RootLayout({ children: <div>app content</div> }));
    expect(screen.getByTestId('providers')).toBeInTheDocument();
    expect(screen.getByText('app content')).toBeInTheDocument();
  });
});
