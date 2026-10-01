import type { CatalogEntry, SourceCatalog, SourceComic, SourceSummary } from '@/types/sources';

/** A source as the Sources page lists it. */
export function mockSource(overrides: Partial<SourceSummary> = {}): SourceSummary {
  return {
    id: 'gocomics',
    displayName: 'GoComics',
    kind: 'DAILY',
    hasCatalog: true,
    catalogUrl: 'https://www.gocomics.com/comics/a-to-z',
    canDetectStart: true,
    catalogCount: 402,
    configuredCount: 8,
    activeCount: 7,
    lastRefreshed: '2026-09-28T11:00:00Z',
    lastRefreshAttempt: '2026-09-28T11:00:00Z',
    lastRefreshError: null,
    refreshing: false,
    settings: {
      userAgent: 'Mozilla/5.0 Chrome/154',
      throttleMinDelayMs: 8000,
      throttleMaxDelayMs: 20000,
      retryMaxAttempts: 4,
      retryInitialBackoffMs: 60000,
      retryMaxBackoffMs: 600000,
      backfillEnabled: true,
      backfillMaxDaysBack: 730,
      backfillMaxPerRun: 30,
      backfillMaxPerDay: 0,
      backfillRecentDays: 7,
      backfillPreferColor: true,
    },
    ...overrides,
  };
}

/** A configured comic as the catalog's rows carry it. */
export function mockSourceComic(overrides: Partial<SourceComic> = {}): SourceComic {
  return {
    id: 1,
    name: 'Calvin and Hobbes',
    source: 'gocomics',
    sourceIdentifier: 'calvinandhobbes',
    enabled: true,
    active: true,
    avatarUrl: '/api/v1/comics/1/avatar',
    avatarAvailable: true,
    avatarPending: false,
    oldest: '2020-01-01',
    newest: '2026-09-28',
    firstStripNumber: null,
    lastStripNumber: null,
    sourceStartDate: '1985-11-18',
    startSource: 'DETECTED',
    startPending: false,
    reportedStartDate: '1985-11-18',
    reportedStartStripNumber: null,
    ...overrides,
  };
}

/** A catalog entry, not configured unless `comic` is given. */
export function mockEntry(overrides: Partial<CatalogEntry> = {}): CatalogEntry {
  return {
    identifier: 'peanuts',
    name: 'Peanuts',
    author: 'Charles Schulz',
    pageUrl: 'https://www.gocomics.com/peanuts',
    thumbnailUrl: null,
    thumbnailPending: false,
    startDate: null,
    startStripNumber: null,
    removedAt: null,
    comic: null,
    ...overrides,
  };
}

/** A source with its catalog, as the catalog page loads it. */
export function mockCatalogSource(entries: CatalogEntry[], overrides: Partial<SourceCatalog> = {}): SourceCatalog {
  const source = mockSource();
  return {
    id: source.id,
    displayName: source.displayName,
    kind: source.kind,
    hasCatalog: source.hasCatalog,
    canDetectStart: source.canDetectStart,
    catalogCount: entries.length,
    configuredCount: entries.filter((e) => e.comic).length,
    activeCount: entries.filter((e) => e.comic?.active).length,
    lastRefreshed: source.lastRefreshed,
    lastRefreshError: null,
    refreshing: false,
    catalog: {
      totalCount: entries.length,
      pageInfo: { hasNextPage: false, endCursor: null },
      edges: entries.map((node) => ({ node })),
    },
    orphans: [],
    ...overrides,
  };
}
