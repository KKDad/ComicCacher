import type { GetSourceCatalogQuery, GetSourcesQuery, SourceComicFieldsFragment } from '@/generated/graphql';

// Shapes of the source fields the operations select. Codegen emits only operation types,
// so these are derived from the query results rather than the schema.
export type SourceSummary = GetSourcesQuery['sources'][number];
export type SourceSettings = SourceSummary['settings'];
export type SourceCatalog = NonNullable<GetSourceCatalogQuery['source']>;
export type CatalogEntry = SourceCatalog['catalog']['edges'][number]['node'];
export type SourceComic = SourceComicFieldsFragment;
