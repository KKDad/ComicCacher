import { render, screen } from '@testing-library/react';
import DashboardPage from './page';

vi.mock('@/components/dashboard/dashboard-client', () => ({
  DashboardClient: () => <div data-testid="dashboard-client" />,
}));

describe('DashboardPage', () => {
  it('renders DashboardClient', () => {
    render(<DashboardPage />);
    expect(screen.getByTestId('dashboard-client')).toBeInTheDocument();
  });
});
