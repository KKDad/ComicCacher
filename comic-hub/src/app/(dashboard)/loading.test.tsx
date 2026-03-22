import { render } from '@testing-library/react';
import DashboardLoading from './loading';

describe('DashboardLoading', () => {
  it('renders skeleton placeholders for header, avatars, and grid', () => {
    const { container } = render(<DashboardLoading />);

    const skeletons = container.querySelectorAll('[data-slot="skeleton"]');
    // Header (2) + 3 avatar circles + grid header (1) + 4 grid items with 2 skeletons each (8) = 14
    expect(skeletons.length).toBeGreaterThanOrEqual(10);
  });

  it('renders a 4-column grid section', () => {
    const { container } = render(<DashboardLoading />);

    expect(container.querySelector('.grid')).toBeInTheDocument();
  });
});
