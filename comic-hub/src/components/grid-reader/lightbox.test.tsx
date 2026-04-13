import { render, screen, fireEvent } from '@testing-library/react';
import { Lightbox } from './lightbox';
import type { GridComic } from '@/hooks/use-grid-reader';

const mockComics: GridComic[] = [
  {
    id: 1, name: 'Garfield', avatarUrl: null, oldest: null, newest: null,
    strip: { date: '2026-03-29', available: true, imageUrl: '/strip/1.png', width: 900, height: 300, transcript: null },
  },
  {
    id: 2, name: 'Dilbert', avatarUrl: null, oldest: null, newest: null,
    strip: { date: '2026-03-29', available: true, imageUrl: '/strip/2.png', width: 800, height: 400, transcript: null },
  },
  {
    id: 3, name: 'Peanuts', avatarUrl: null, oldest: null, newest: null,
    strip: { date: '2026-03-29', available: true, imageUrl: '/strip/3.png', width: 700, height: 350, transcript: null },
  },
];

describe('Lightbox', () => {
  const defaultProps = {
    comics: mockComics,
    currentIndex: 1,
    onClose: vi.fn(),
    onNext: vi.fn(),
    onPrevious: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the current comic strip image', () => {
    render(<Lightbox {...defaultProps} />);
    const img = screen.getByRole('img');
    expect(img).toHaveAttribute('src', '/strip/2.png');
  });

  it('displays comic name', () => {
    render(<Lightbox {...defaultProps} />);
    expect(screen.getByText('Dilbert')).toBeInTheDocument();
  });

  it('renders close button', () => {
    render(<Lightbox {...defaultProps} />);
    const closeBtn = screen.getByRole('button', { name: /close/i });
    fireEvent.click(closeBtn);
    expect(defaultProps.onClose).toHaveBeenCalledOnce();
  });

  it('renders previous button when not at first item', () => {
    render(<Lightbox {...defaultProps} />);
    const prevBtn = screen.getByRole('button', { name: /previous/i });
    fireEvent.click(prevBtn);
    expect(defaultProps.onPrevious).toHaveBeenCalledOnce();
  });

  it('renders next button when not at last item', () => {
    render(<Lightbox {...defaultProps} />);
    const nextBtn = screen.getByRole('button', { name: /next/i });
    fireEvent.click(nextBtn);
    expect(defaultProps.onNext).toHaveBeenCalledOnce();
  });

  it('does not render previous button at first item', () => {
    render(<Lightbox {...defaultProps} currentIndex={0} />);
    expect(screen.queryByRole('button', { name: /previous/i })).not.toBeInTheDocument();
  });

  it('does not render next button at last item', () => {
    render(<Lightbox {...defaultProps} currentIndex={2} />);
    expect(screen.queryByRole('button', { name: /next/i })).not.toBeInTheDocument();
  });

  it('returns null when comic has no strip image', () => {
    const noStripComics: GridComic[] = [{
      id: 1, name: 'Empty', avatarUrl: null, oldest: null, newest: null,
      strip: { date: '2026-03-29', available: false, imageUrl: null, width: null, height: null, transcript: null },
    }];
    const { container } = render(<Lightbox {...defaultProps} comics={noStripComics} currentIndex={0} />);
    expect(container.innerHTML).toBe('');
  });

  it('has modal dialog role', () => {
    render(<Lightbox {...defaultProps} />);
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });
});
