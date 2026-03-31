import { render, screen } from '@testing-library/react';
import GridReaderPage from './page';

vi.mock('next/navigation', () => ({
  useSearchParams: () => new URLSearchParams('date=2026-03-15'),
}));

vi.mock('@/components/grid-reader/grid-reader', () => ({
  GridReader: ({ initialDate }: { initialDate?: string }) => (
    <div data-testid="grid-reader" data-date={initialDate}>Grid Reader</div>
  ),
}));

describe('GridReaderPage', () => {
  it('renders GridReader with date from search params', () => {
    render(<GridReaderPage />);
    const reader = screen.getByTestId('grid-reader');
    expect(reader).toBeInTheDocument();
    expect(reader).toHaveAttribute('data-date', '2026-03-15');
  });
});
