import { render, screen } from '@testing-library/react';
import SourceCatalogPage from './page';

vi.mock('@/components/sources/source-catalog-view', () => ({
  SourceCatalogView: ({ sourceId }: { sourceId: string }) => <p>catalog of {sourceId}</p>,
}));

describe('SourceCatalogPage', () => {
  it('shows the catalog of the source in the URL', async () => {
    render(await SourceCatalogPage({ params: Promise.resolve({ id: 'gocomics' }) }));

    expect(screen.getByText('catalog of gocomics')).toBeInTheDocument();
  });
});
