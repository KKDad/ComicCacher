import { render } from '@testing-library/react';
import { InlineScript } from './inline-script';

describe('InlineScript', () => {
  it('is inert when rendered on the client, so React never tries to run it', () => {
    const { container } = render(<InlineScript html="window.__ran = true" />);
    const script = container.querySelector('script');

    expect(script).toHaveAttribute('type', 'text/plain');
    expect(script?.innerHTML).toBe('window.__ran = true');
  });
});
