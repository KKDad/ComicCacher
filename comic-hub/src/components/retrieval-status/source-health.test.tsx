import { render, screen, within } from '@testing-library/react';
import { SourceHealth } from './source-health';

describe('SourceHealth', () => {
  it('shows each source’s counts and links to its page', () => {
    render(
      <SourceHealth
        sources={[
          { source: 'comicskingdom', success: 4, unavailable: 0, rateLimited: 0, failed: 0 },
          { source: 'gocomics', success: 10, unavailable: 1, rateLimited: 2, failed: 3 },
        ]}
      />,
    );

    const link = screen.getByRole('link', { name: 'gocomics' });
    expect(link).toHaveAttribute('href', '/sources/gocomics');
    const card = link.parentElement!;
    expect(within(card).getByText('Rate limited').nextSibling).toHaveTextContent('2');
    expect(within(card).getByText('Failed').nextSibling).toHaveTextContent('3');
  });

  it('renders nothing without sources', () => {
    const { container } = render(<SourceHealth sources={[]} />);
    expect(container).toBeEmptyDOMElement();
  });
});
