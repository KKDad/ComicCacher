import { render, screen } from '@testing-library/react';

import NotFound from './not-found';

describe('NotFound', () => {
  it('renders the 404 message and a link back to comics', () => {
    render(<NotFound />);
    expect(screen.getByText('Page not found')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Back to comics' })).toHaveAttribute('href', '/');
  });
});
