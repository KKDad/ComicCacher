import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { GridStripCard } from './grid-strip-card';
import { useUser } from '@/contexts/user-context';
import type { GridComic } from '@/hooks/use-grid-reader';

vi.mock('@/contexts/user-context', () => ({
  useUser: vi.fn(),
}));

const mockComic: GridComic = {
  id: 1,
  name: 'Garfield',
  avatarUrl: '/avatar/1',
  oldest: '2020-01-01',
  newest: '2026-03-29',
  strip: {
    date: '2026-03-29',
    available: true,
    imageUrl: '/strip/1/2026-03-29',
    width: 900,
    height: 300,
    transcript: 'I hate Mondays',
  },
};

const unavailableComic: GridComic = {
  ...mockComic,
  strip: {
    date: '2026-03-29',
    available: false,
    imageUrl: null,
    width: null,
    height: null,
    transcript: null,
  },
};

describe('GridStripCard', () => {
  beforeEach(() => {
    vi.mocked(useUser).mockReturnValue(null);
  });

  it('renders comic name', () => {
    render(<GridStripCard comic={mockComic} date="2026-03-29" onImageClick={vi.fn()} />);
    expect(screen.getByText('Garfield')).toBeInTheDocument();
  });

  it('renders avatar image', () => {
    const { container } = render(<GridStripCard comic={mockComic} date="2026-03-29" onImageClick={vi.fn()} />);
    // Avatar img has alt="" (presentational), so use querySelector
    const avatarImg = container.querySelector('img[src="/avatar/1"]');
    expect(avatarImg).toBeInTheDocument();
  });

  it('renders strip image when available', () => {
    render(<GridStripCard comic={mockComic} date="2026-03-29" onImageClick={vi.fn()} />);
    const imgs = screen.getAllByRole('img');
    expect(imgs.some((img) => img.getAttribute('src') === '/strip/1/2026-03-29')).toBe(true);
  });

  it('shows unavailable message when strip not available', () => {
    render(<GridStripCard comic={unavailableComic} date="2026-03-29" onImageClick={vi.fn()} />);
    expect(screen.getByText(/No strip available/)).toBeInTheDocument();
  });

  it('calls onImageClick when strip image is clicked', () => {
    const onClick = vi.fn();
    render(<GridStripCard comic={mockComic} date="2026-03-29" onImageClick={onClick} />);
    const button = screen.getByRole('button', { name: /fullscreen/i });
    fireEvent.click(button);
    expect(onClick).toHaveBeenCalledOnce();
  });

  it('renders transcript toggle when transcript exists', () => {
    render(<GridStripCard comic={mockComic} date="2026-03-29" onImageClick={vi.fn()} />);
    expect(screen.getByText('Show transcript')).toBeInTheDocument();
  });

  it('expands transcript on click', async () => {
    render(<GridStripCard comic={mockComic} date="2026-03-29" onImageClick={vi.fn()} />);
    await userEvent.click(screen.getByText('Show transcript'));
    expect(screen.getByText('I hate Mondays')).toBeInTheDocument();
    expect(screen.getByText('Hide transcript')).toBeInTheDocument();
  });

  it('does not render transcript toggle when no transcript', () => {
    const noTranscript = { ...mockComic, strip: { ...mockComic.strip!, transcript: null } };
    render(<GridStripCard comic={noTranscript} date="2026-03-29" onImageClick={vi.fn()} />);
    expect(screen.queryByText('Show transcript')).not.toBeInTheDocument();
  });

  it('renders placeholder when no avatar', () => {
    const noAvatar = { ...mockComic, avatarUrl: null };
    const { container } = render(<GridStripCard comic={noAvatar} date="2026-03-29" onImageClick={vi.fn()} />);
    // Should render a div placeholder instead of img
    expect(container.querySelector('.rounded-full.bg-muted')).toBeInTheDocument();
  });

  it('does not show admin items for non-admin user', async () => {
    vi.mocked(useUser).mockReturnValue({ username: 'user', email: 'u@test.com', displayName: 'User', roles: ['USER'], created: '2026-01-01' });
    render(<GridStripCard comic={mockComic} date="2026-03-29" onImageClick={vi.fn()} />);
    // Open hamburger menu
    await userEvent.click(screen.getByRole('button', { name: /strip actions/i }));
    expect(screen.queryByText('Statistics')).not.toBeInTheDocument();
    expect(screen.queryByText('Batch refresh')).not.toBeInTheDocument();
  });

  it('shows admin items for admin user', async () => {
    vi.mocked(useUser).mockReturnValue({ username: 'admin', email: 'a@test.com', displayName: 'Admin', roles: ['USER', 'ADMIN'], created: '2026-01-01' });
    render(<GridStripCard comic={mockComic} date="2026-03-29" onImageClick={vi.fn()} />);
    await userEvent.click(screen.getByRole('button', { name: /strip actions/i }));
    expect(screen.getByText('Statistics')).toBeInTheDocument();
    expect(screen.getByText('Batch refresh')).toBeInTheDocument();
  });
});
