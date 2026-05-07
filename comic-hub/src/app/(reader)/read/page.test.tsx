import { render, screen } from '@testing-library/react';
import GridReaderPage from './page';

const searchParamsRef = { current: new URLSearchParams('date=2026-03-15') };
vi.mock('next/navigation', () => ({
  useSearchParams: () => searchParamsRef.current,
}));

vi.mock('@/components/grid-reader/grid-reader', () => ({
  GridReader: ({ initialDate }: { initialDate?: string }) => (
    <div data-testid="grid-reader" data-date={initialDate ?? 'undefined'}>Grid Reader</div>
  ),
}));

describe('GridReaderPage', () => {
  it('renders GridReader with date from search params', () => {
    searchParamsRef.current = new URLSearchParams('date=2026-03-15');
    render(<GridReaderPage />);
    const reader = screen.getByTestId('grid-reader');
    expect(reader).toBeInTheDocument();
    expect(reader).toHaveAttribute('data-date', '2026-03-15');
  });

  it('renders GridReader with undefined initialDate when no date param', () => {
    searchParamsRef.current = new URLSearchParams();
    render(<GridReaderPage />);
    expect(screen.getByTestId('grid-reader')).toHaveAttribute('data-date', 'undefined');
  });
});
