import { render, screen } from '@testing-library/react';
import BatchJobsLayout from './layout';

vi.mock('next/font/google', () => ({
  JetBrains_Mono: () => ({ variable: 'font-mono-variable' }),
}));

describe('BatchJobsLayout', () => {
  it('provides the monospace font variable to its pages', () => {
    render(<BatchJobsLayout>logs</BatchJobsLayout>);
    expect(screen.getByText('logs')).toHaveClass('font-mono-variable');
  });
});
