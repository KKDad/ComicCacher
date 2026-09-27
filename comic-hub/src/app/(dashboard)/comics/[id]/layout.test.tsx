import { render, screen } from '@testing-library/react';
import Layout, { generateMetadata } from './layout';
import { comicTitle } from '@/lib/comic-title';

vi.mock('@/lib/comic-title', () => ({
  comicTitle: vi.fn(),
}));

describe('comic detail layout', () => {
  it('titles the page with the comic name', async () => {
    vi.mocked(comicTitle).mockResolvedValue('Adam At Home');
    await expect(generateMetadata({ params: Promise.resolve({ id: '7' }) })).resolves.toEqual({ title: 'Adam At Home' });
    expect(comicTitle).toHaveBeenCalledWith('7');
  });

  it('leaves the default title when the name is unknown', async () => {
    vi.mocked(comicTitle).mockResolvedValue(undefined);
    await expect(generateMetadata({ params: Promise.resolve({ id: '7' }) })).resolves.toEqual({});
  });

  it('renders its children', () => {
    render(<Layout>detail</Layout>);
    expect(screen.getByText('detail')).toBeInTheDocument();
  });
});
