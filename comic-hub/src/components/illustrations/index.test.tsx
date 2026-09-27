import { render } from '@testing-library/react';
import { HistoryIllustration, FavoritesIllustration, BrokenIllustration, QuietIllustration } from '.';

describe('illustrations', () => {
  it.each([
    ['history', HistoryIllustration],
    ['favorites', FavoritesIllustration],
    ['broken', BrokenIllustration],
    ['quiet', QuietIllustration],
  ])('%s renders a decorative SVG with the given class', (_, Illustration) => {
    const { container } = render(<Illustration className="w-40" />);
    const svg = container.querySelector('svg')!;

    expect(svg).toHaveAttribute('aria-hidden', 'true');
    expect(svg).toHaveClass('w-40');
    expect(svg.childElementCount).toBeGreaterThan(0);
  });
});
