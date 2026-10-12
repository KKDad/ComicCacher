import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { StripText, stripAlt } from './strip-text';

describe('stripAlt', () => {
  it('uses the alt text when the strip has one', () => {
    expect(stripAlt('xkcd', 'Wed, October 7, 2026', 'I need to push some updates.')).toBe(
      'xkcd, Wed, October 7, 2026: I need to push some updates.',
    );
  });

  it('falls back to the comic and date', () => {
    expect(stripAlt('Garfield', 'Sun, March 15, 2026', null)).toBe('Garfield - Sun, March 15, 2026');
  });
});

describe('StripText', () => {
  it('renders nothing without a transcript or alt text', () => {
    const { container } = render(<StripText transcript={null} altText={null} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('keeps the alt text hidden until it is asked for', async () => {
    render(<StripText transcript={null} altText="The punchline" />);

    expect(screen.queryByText('The punchline')).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Show alt text' }));
    expect(screen.getByText('The punchline')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Hide alt text' })).toHaveAttribute('aria-expanded', 'true');
  });

  it('shows separate toggles for the alt text and the transcript', async () => {
    render(<StripText transcript="Jon: Hi" altText="The punchline" />);

    await userEvent.click(screen.getByRole('button', { name: 'Show transcript' }));
    expect(screen.getByText('Jon: Hi')).toBeInTheDocument();
    expect(screen.queryByText('The punchline')).not.toBeInTheDocument();
  });

  it('keeps clicks from reaching the reader behind it', () => {
    const onParentClick = vi.fn();
    render(
      <div onClick={onParentClick}>
        <StripText transcript={null} altText="The punchline" />
      </div>,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Show alt text' }));
    expect(onParentClick).not.toHaveBeenCalled();
  });
});
