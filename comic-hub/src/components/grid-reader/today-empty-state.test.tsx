import { render, screen } from '@testing-library/react';
import { TodayEmptyState } from './today-empty-state';

describe('TodayEmptyState', () => {
  it('explains the Daily Reader shows favorites and links to pick some', () => {
    render(<TodayEmptyState />);

    expect(screen.getByText('Nothing to read here yet')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Pick favorites' })).toHaveAttribute('href', '/comics');
  });
});
