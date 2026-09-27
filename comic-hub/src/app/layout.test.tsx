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
  it('renders Providers wrapping children', async () => {
    const { default: RootLayout } = await import('./layout');
    render(RootLayout({ children: <div>app content</div> }));
    expect(screen.getByTestId('providers')).toBeInTheDocument();
    expect(screen.getByText('app content')).toBeInTheDocument();
  });
});
