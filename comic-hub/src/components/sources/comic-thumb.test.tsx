import { screen } from '@testing-library/react';
import { ComicThumb, initials } from './comic-thumb';
import { imageSrc, renderWithProviders } from '@/test/test-utils';

describe('initials', () => {
  it.each([
    ['Calvin and Hobbes', 'CA'],
    ['Peanuts', 'PE'],
    ['9 to 5', '9T'],
    ['Hagar – The Horrible', 'HT'],
    ['!!!', '?'],
  ])('%s → %s', (name, expected) => {
    expect(initials(name)).toBe(expected);
  });
});

describe('ComicThumb', () => {
  it('shows the picture when there is one', () => {
    renderWithProviders(<ComicThumb name="Peanuts" src="/api/v1/sources/gocomics/thumbnails/peanuts" />);

    expect(imageSrc(screen.getByRole('presentation', { hidden: true }))).toBe('/api/v1/sources/gocomics/thumbnails/peanuts');
  });

  it('shows initials, and a spinner while the picture downloads', () => {
    renderWithProviders(<ComicThumb name="Peanuts" pending />);

    expect(screen.getByText('PE')).toBeInTheDocument();
    expect(screen.getByText('Downloading picture')).toBeInTheDocument();
  });
});
