import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ReaderError from './error';

describe('ReaderError', () => {
  it('renders error message', () => {
    const error = new Error('Test error');
    render(<ReaderError error={error} reset={vi.fn()} />);

    expect(screen.getByText('Something went wrong')).toBeInTheDocument();
    expect(screen.getByText(/reader encountered an error/i)).toBeInTheDocument();
  });

  it('calls window.history.back when Go Back is clicked', async () => {
    const backSpy = vi.spyOn(window.history, 'back').mockImplementation(() => {});
    const error = new Error('Test error');
    render(<ReaderError error={error} reset={vi.fn()} />);

    await userEvent.click(screen.getByRole('button', { name: /go back/i }));
    expect(backSpy).toHaveBeenCalledOnce();
    backSpy.mockRestore();
  });

  it('calls reset when Try Again is clicked', async () => {
    const resetFn = vi.fn();
    const error = new Error('Test error');
    render(<ReaderError error={error} reset={resetFn} />);

    await userEvent.click(screen.getByRole('button', { name: /try again/i }));
    expect(resetFn).toHaveBeenCalledOnce();
  });
});
