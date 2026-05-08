import { fireEvent, render, screen } from '@testing-library/react';

import RootError from './error';

describe('RootError', () => {
  beforeEach(() => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
  });

  it('renders fallback message and action buttons', () => {
    render(<RootError error={new Error('boom')} reset={() => {}} />);
    expect(screen.getByText('Something went wrong')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Go home' })).toHaveAttribute('href', '/');
  });

  it('invokes reset when Try again is clicked', () => {
    const reset = vi.fn();
    render(<RootError error={new Error('boom')} reset={reset} />);
    fireEvent.click(screen.getByRole('button', { name: 'Try again' }));
    expect(reset).toHaveBeenCalledTimes(1);
  });

  it('logs the error on mount', () => {
    const error = new Error('boom');
    const spy = vi.spyOn(console, 'error');
    render(<RootError error={error} reset={() => {}} />);
    expect(spy).toHaveBeenCalledWith('Root error boundary caught:', error);
  });
});
