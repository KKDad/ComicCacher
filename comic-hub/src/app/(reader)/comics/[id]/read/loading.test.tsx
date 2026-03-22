import { render, screen } from '@testing-library/react';
import ReaderLoading from './loading';

describe('ReaderLoading', () => {
  it('renders skeleton placeholders', () => {
    const { container } = render(<ReaderLoading />);

    // Should render header skeleton and 3 strip skeletons
    const skeletons = container.querySelectorAll('[data-slot="skeleton"]');
    expect(skeletons.length).toBeGreaterThanOrEqual(2);
  });

  it('renders without crashing', () => {
    render(<ReaderLoading />);
    expect(document.querySelector('.min-h-screen')).toBeInTheDocument();
  });
});
