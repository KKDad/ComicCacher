import { render, screen } from '@testing-library/react';
import { useQueryClient } from '@tanstack/react-query';
import { Providers } from './providers';

function TestChild() {
  const queryClient = useQueryClient();
  return <div data-testid="child">{queryClient ? 'has-client' : 'no-client'}</div>;
}

describe('Providers', () => {
  it('renders children', () => {
    render(
      <Providers>
        <div>Hello</div>
      </Providers>,
    );
    expect(screen.getByText('Hello')).toBeInTheDocument();
  });

  it('provides QueryClient to children', () => {
    render(
      <Providers>
        <TestChild />
      </Providers>,
    );
    expect(screen.getByTestId('child')).toHaveTextContent('has-client');
  });
});
