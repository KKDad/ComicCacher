import { render, screen } from '@testing-library/react';
import AuthLayout from './layout';

describe('AuthLayout', () => {
  it('renders children', () => {
    render(<AuthLayout><div>Login Form</div></AuthLayout>);
    expect(screen.getByText('Login Form')).toBeInTheDocument();
  });

  it('wraps children in a centered container', () => {
    const { container } = render(<AuthLayout><div>content</div></AuthLayout>);
    expect(container.firstChild).toHaveClass('min-h-screen', 'flex', 'items-center', 'justify-center');
  });
});
