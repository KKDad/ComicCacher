import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { StripCard } from './strip-card';
import type { Strip } from '@/hooks/use-reader';
import { imageSrc } from '@/test/test-utils';

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

  it('loads lazily by default and eagerly with high priority when it is the priority strip', () => {
    const { rerender } = render(<StripCard strip={availableStrip} comicName="Garfield" />);
    expect(screen.getByRole('img')).toHaveAttribute('loading', 'lazy');

    rerender(<StripCard strip={availableStrip} comicName="Garfield" priority />);
    const img = screen.getByRole('img');
    expect(img).toHaveAttribute('loading', 'eager');
    expect(img).toHaveAttribute('fetchpriority', 'high');
  });

  it('opens fullscreen when clicked', () => {
    const onOpen = vi.fn();
    render(<StripCard strip={availableStrip} comicName="Garfield" onOpen={onOpen} />);

    fireEvent.click(screen.getByRole('button', { name: /view garfield, .* fullscreen/i }));
    expect(onOpen).toHaveBeenCalledOnce();
  });

  it('renders image for available strip', () => {
    render(<StripCard strip={availableStrip} comicName="Garfield" />);

    const img = screen.getByRole('img');
    expect(imageSrc(img)).toBe('https://example.com/strip.png');
    expect(img).toHaveAttribute('alt', expect.stringContaining('Garfield'));
  });

  it('renders formatted date for available strip', () => {
    render(<StripCard strip={availableStrip} comicName="Garfield" />);

    expect(screen.getByText('Sun, March 15, 2026')).toBeInTheDocument();
  });

  it('renders unavailable message for missing strip', () => {
    render(<StripCard strip={unavailableStrip} comicName="Garfield" />);

    expect(screen.getByText(/no strip available/i)).toBeInTheDocument();
  });

  it('shows error state when image fails to load', () => {
    render(<StripCard strip={availableStrip} comicName="Garfield" />);

    const img = screen.getByRole('img');
    fireEvent.error(img);

    expect(screen.getByText(/strip didn.t load/i)).toBeInTheDocument();
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

  it('transitions opacity on image load', async () => {
    render(<StripCard strip={availableStrip} comicName="Garfield" />);

    const img = screen.getByRole('img');
    expect(img.className).toContain('opacity-0');

    // next/image calls onLoad once the image has decoded, after the load event
    fireEvent.load(img);
    await waitFor(() => expect(img.className).toContain('opacity-100'));
  });
});
