import { render, screen, fireEvent } from '@testing-library/react';
import { StripCard } from './strip-card';
import type { Strip } from '@/hooks/use-reader';

describe('StripCard', () => {
  const availableStrip: Strip = {
    date: '2026-03-15',
    available: true,
    imageUrl: 'https://example.com/strip.png',
    width: 900,
    height: 300,
  };

  const unavailableStrip: Strip = {
    date: '2026-03-15',
    available: false,
    imageUrl: null,
    width: null,
    height: null,
  };

  it('renders image for available strip', () => {
    render(<StripCard strip={availableStrip} comicName="Garfield" />);

    const img = screen.getByRole('img');
    expect(img).toHaveAttribute('src', 'https://example.com/strip.png');
    expect(img).toHaveAttribute('alt', expect.stringContaining('Garfield'));
  });

  it('renders formatted date for available strip', () => {
    render(<StripCard strip={availableStrip} comicName="Garfield" />);

    const formatted = new Date('2026-03-15').toLocaleDateString('en-US', {
      weekday: 'short',
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
    expect(screen.getByText(formatted)).toBeInTheDocument();
  });

  it('renders unavailable message for missing strip', () => {
    render(<StripCard strip={unavailableStrip} comicName="Garfield" />);

    expect(screen.getByText(/no strip available/i)).toBeInTheDocument();
  });

  it('shows error state when image fails to load', () => {
    render(<StripCard strip={availableStrip} comicName="Garfield" />);

    const img = screen.getByRole('img');
    fireEvent.error(img);

    expect(screen.getByText(/failed to load strip/i)).toBeInTheDocument();
  });

  it('sets aspect ratio from width and height', () => {
    const { container } = render(
      <StripCard strip={availableStrip} comicName="Garfield" />,
    );

    const wrapper = container.querySelector('[style]');
    expect(wrapper).toHaveStyle({ aspectRatio: '900/300' });
  });

  it('uses fallback aspect ratio when no dimensions', () => {
    const noDimStrip: Strip = {
      ...availableStrip,
      width: null,
      height: null,
    };
    const { container } = render(
      <StripCard strip={noDimStrip} comicName="Garfield" />,
    );

    const wrapper = container.querySelector('.aspect-\\[3\\/1\\]');
    expect(wrapper).toBeInTheDocument();
  });

  it('transitions opacity on image load', () => {
    render(<StripCard strip={availableStrip} comicName="Garfield" />);

    const img = screen.getByRole('img');
    expect(img.className).toContain('opacity-0');

    fireEvent.load(img);
    expect(img.className).toContain('opacity-100');
  });
});
