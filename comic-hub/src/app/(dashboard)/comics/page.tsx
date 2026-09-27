'use client';

import { useMemo, useState } from 'react';
import { useSearchParams } from 'next/navigation';
import { ComicTile } from '@/components/comics/comic-tile';
import { Card } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { Input } from '@/components/ui/input';
import { BookOpen, Search, SearchX } from 'lucide-react';
import { useSearchComicsQuery } from '@/generated/graphql';
import { useAllComics } from '@/hooks/use-all-comics';
import { useFavorites } from '@/hooks/use-favorite';
import { compareByName } from '@/lib/sort';

function LoadingSkeleton() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <Skeleton className="h-8 w-48 mb-2" />
          <Skeleton className="h-5 w-64" />
        </div>
      </div>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {[...Array(12)].map((_, i) => (
          <Card key={i} className="overflow-hidden">
            <Skeleton className="aspect-[4/3] w-full" />
            <div className="p-3 space-y-2">
              <Skeleton className="h-5 w-3/4" />
              <Skeleton className="h-4 w-1/2" />
            </div>
          </Card>
        ))}
      </div>
    </div>
  );
}

function SearchResults({ query }: { query: string }) {
  const { favoriteIds, toggle } = useFavorites();
  const { data, isLoading } = useSearchComicsQuery(
    { query },
    { enabled: query.length > 0 },
  );

  if (isLoading) return <LoadingSkeleton />;

  const comics = data?.search.comics ?? [];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-ink">Search Results</h1>
        <p className="text-ink-subtle mt-1">
          {comics.length === 0
            ? `No comics matching "${query}"`
            : `${comics.length} comic${comics.length === 1 ? '' : 's'} matching "${query}"`}
        </p>
      </div>

      {comics.length === 0 ? (
        <Card className="border-dashed">
          <div className="flex flex-col items-center justify-center p-12 text-center">
            <SearchX className="h-12 w-12 text-ink-muted mb-4" />
            <p className="text-ink-subtle mb-2">No comics found</p>
            <p className="text-sm text-ink-muted">
              Try a different search term
            </p>
          </div>
        </Card>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {comics.map((comic) => (
            <ComicTile
              key={comic.id}
              comic={{
                id: comic.id,
                name: comic.name,
                date: comic.lastStrip?.date ?? comic.newest,
                thumbnail: comic.lastStrip?.imageUrl ?? comic.avatarUrl ?? undefined,
              }}
              isFavorite={favoriteIds.has(comic.id)}
              onToggleFavorite={() => toggle(comic.id)}
            />
          ))}
        </div>
      )}
    </div>
  );
}

function BrowseComics() {
  // The whole catalogue is small, so load it all: then it can be sorted and filtered
  // here, and favorited from any tile.
  const { comics, isLoading } = useAllComics();
  const { favoriteIds, toggle } = useFavorites();
  const [filter, setFilter] = useState('');

  const visible = useMemo(() => {
    const needle = filter.trim().toLocaleLowerCase();
    return comics
      .filter((c) => !needle || c.name.toLocaleLowerCase().includes(needle))
      .sort(compareByName);
  }, [comics, filter]);

  if (isLoading) return <LoadingSkeleton />;

  if (comics.length === 0) {
    return (
      <div className="space-y-6">
        <h1 className="text-3xl font-bold text-ink">Browse Comics</h1>
        <Card className="border-dashed">
          <div className="flex flex-col items-center justify-center p-12 text-center">
            <BookOpen className="h-12 w-12 text-ink-muted mb-4" />
            <p className="text-ink-subtle mb-2">No comics available</p>
            <p className="text-sm text-ink-muted">
              Check back later when comics are added
            </p>
          </div>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-3xl font-bold text-ink">Browse Comics</h1>
          <p className="text-ink-subtle mt-1">
            Explore all {comics.length} available comics
          </p>
        </div>
        <div className="relative w-full sm:w-72">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-muted" aria-hidden="true" />
          <Input
            type="search"
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            placeholder="Filter comics"
            aria-label="Filter comics by name"
            className="pl-9"
          />
        </div>
      </div>

      {visible.length === 0 ? (
        <p className="text-ink-subtle py-12 text-center">No comics match &ldquo;{filter.trim()}&rdquo;</p>
      ) : (
        <ul className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {visible.map((comic) => (
            <li key={comic.id}>
              <ComicTile
                comic={{
                  id: comic.id,
                  name: comic.name,
                  date: comic.lastStrip?.date ?? comic.newest,
                  thumbnail: comic.lastStrip?.imageUrl ?? comic.avatarUrl ?? undefined,
                }}
                isFavorite={favoriteIds.has(comic.id)}
                onToggleFavorite={() => toggle(comic.id)}
              />
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default function ComicsPage() {
  const searchParams = useSearchParams();
  const query = searchParams.get('q')?.trim() ?? '';

  if (query) {
    return <SearchResults query={query} />;
  }

  return <BrowseComics />;
}
