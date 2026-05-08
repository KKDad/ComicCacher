import { fireEvent, render, screen } from '@testing-library/react';

import GlobalError from './global-error';

describe('GlobalError', () => {
  beforeEach(() => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
  });

  it('renders the catastrophic-failure message and Reload button', () => {
    render(<GlobalError error={new Error('catastrophic')} reset={() => {}} />);
    expect(screen.getByText('Something broke badly')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Reload' })).toBeInTheDocument();
  });

  it('invokes reset when Reload is clicked', () => {
    const reset = vi.fn();
    render(<GlobalError error={new Error('catastrophic')} reset={reset} />);
    fireEvent.click(screen.getByRole('button', { name: 'Reload' }));
    expect(reset).toHaveBeenCalledTimes(1);
  });

  it('logs the error on mount', () => {
    const error = new Error('catastrophic');
    const spy = vi.spyOn(console, 'error');
    render(<GlobalError error={error} reset={() => {}} />);
    expect(spy).toHaveBeenCalledWith('Global error boundary caught:', error);
  });
});
