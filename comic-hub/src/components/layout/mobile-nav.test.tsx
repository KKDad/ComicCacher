import { render, screen } from '@testing-library/react';
import { MobileNav } from './mobile-nav';
import { usePathname } from 'next/navigation';

describe('MobileNav', () => {
  beforeEach(() => {
    vi.mocked(usePathname).mockReturnValue('/');
  });

  it('renders four direct tabs with short labels', () => {
    render(<MobileNav />);
    expect(screen.getAllByRole('link')).toHaveLength(4);
    expect(screen.getByRole('link', { name: 'Home' })).toHaveAttribute('href', '/');
    expect(screen.getByRole('link', { name: 'Today' })).toHaveAttribute('href', '/read');
    expect(screen.getByRole('link', { name: 'Library' })).toHaveAttribute('href', '/comics');
    expect(screen.getByRole('link', { name: 'Settings' })).toHaveAttribute('href', '/preferences');
  });

  it('has no hidden More menu', () => {
    render(<MobileNav />);
    expect(screen.queryByRole('button', { name: 'More' })).not.toBeInTheDocument();
  });

  it('marks the current page with aria-current', () => {
    vi.mocked(usePathname).mockReturnValue('/comics/3');
    render(<MobileNav />);
    expect(screen.getByRole('link', { name: 'Library' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('link', { name: 'Home' })).not.toHaveAttribute('aria-current');
  });
});
