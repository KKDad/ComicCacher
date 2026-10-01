import { SourceCatalogView } from '@/components/sources/source-catalog-view';

export default async function SourceCatalogPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <SourceCatalogView sourceId={id} />;
}
