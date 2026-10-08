import { render, screen, within } from '@testing-library/react';
import { ResultsLegend } from './results-legend';

describe('ResultsLegend', () => {
  it('explains each kind of cell', () => {
    render(<ResultsLegend />);

    const key = screen.getByRole('list', { name: 'Key' });
    for (const label of ['On disk', 'Recovered after a failure', 'Missing', 'No strip due', 'Waiting for today’s run']) {
      expect(within(key).getByText(label)).toBeInTheDocument();
    }
  });
});
