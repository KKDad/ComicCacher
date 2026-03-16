'use client';

import { useState } from 'react';
import { Card } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { BarChart3, Database, Image, MousePointerClick, ArrowUpDown } from 'lucide-react';
import { useGetCombinedMetricsQuery } from '@/generated/graphql';

function formatBytes(bytes: number): string {
  if (bytes === 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  const value = bytes / Math.pow(1024, i);
  return `${value.toFixed(i === 0 ? 0 : 1)} ${units[i]}`;
}

function timeAgo(dateStr: string): string {
  const date = new Date(dateStr);
  const now = new Date();
  const seconds = Math.floor((now.getTime() - date.getTime()) / 1000);
  if (seconds < 60) return 'just now';
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  return `${days}d ago`;
}

type SortDir = 'asc' | 'desc';

export default function MetricsPage() {
  const { data, isLoading, error } = useGetCombinedMetricsQuery();

  const [storageSortKey, setStorageSortKey] = useState<'comicName' | 'imageCount' | 'totalBytes'>('totalBytes');
  const [storageSortDir, setStorageSortDir] = useState<SortDir>('desc');
  const [accessSortKey, setAccessSortKey] = useState<'comicName' | 'accessCount' | 'averageAccessTimeMs'>('accessCount');
  const [accessSortDir, setAccessSortDir] = useState<SortDir>('desc');

  function toggleStorageSort(key: typeof storageSortKey) {
    if (storageSortKey === key) {
      setStorageSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setStorageSortKey(key);
      setStorageSortDir('desc');
    }
  }

  function toggleAccessSort(key: typeof accessSortKey) {
    if (accessSortKey === key) {
      setAccessSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setAccessSortKey(key);
      setAccessSortDir('desc');
    }
  }

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-8 w-40" />
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {[...Array(4)].map((_, i) => (
            <Card key={i} className="p-6">
              <Skeleton className="h-4 w-24 mb-2" />
              <Skeleton className="h-8 w-32" />
            </Card>
          ))}
        </div>
        <Skeleton className="h-64 w-full" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center py-20 text-center">
        <h2 className="text-xl font-semibold text-destructive">Failed to load metrics</h2>
        <p className="mt-2 text-muted-foreground">
          {error instanceof Error ? error.message : 'An unexpected error occurred.'}
        </p>
      </div>
    );
  }

  const metrics = data?.combinedMetrics;

  if (!metrics || (!metrics.storage && !metrics.access)) {
    return (
      <div className="space-y-6">
        <h1 className="text-3xl font-bold text-ink">Metrics</h1>
        <Card className="border-dashed">
          <div className="flex flex-col items-center justify-center p-12 text-center">
            <BarChart3 className="h-12 w-12 text-ink-muted mb-4" />
            <p className="text-ink-subtle mb-2">No metrics available</p>
            <p className="text-sm text-ink-muted">
              Metrics will appear once comics have been cached and accessed
            </p>
          </div>
        </Card>
      </div>
    );
  }

  const storage = metrics.storage;
  const access = metrics.access;

  const totalImages = storage?.comics?.reduce((sum, c) => sum + c.imageCount, 0) ?? 0;

  const storageComics = [...(storage?.comics ?? [])].sort((a, b) => {
    const dir = storageSortDir === 'asc' ? 1 : -1;
    if (storageSortKey === 'comicName') return dir * a.comicName.localeCompare(b.comicName);
    return dir * (a[storageSortKey] - b[storageSortKey]);
  });

  const accessComics = [...(access?.comics ?? [])].sort((a, b) => {
    const dir = accessSortDir === 'asc' ? 1 : -1;
    if (accessSortKey === 'comicName') return dir * a.comicName.localeCompare(b.comicName);
    return dir * ((a[accessSortKey] ?? 0) - (b[accessSortKey] ?? 0));
  });

  const summaryCards = [
    { label: 'Total Storage', value: formatBytes(storage?.totalBytes ?? 0), icon: Database },
    { label: 'Comics Tracked', value: String(storage?.comicCount ?? 0), icon: BarChart3 },
    { label: 'Total Images', value: totalImages.toLocaleString(), icon: Image },
    { label: 'Total Accesses', value: (access?.totalAccesses ?? 0).toLocaleString(), icon: MousePointerClick },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-ink">Metrics</h1>
        {metrics.lastUpdated && (
          <p className="text-ink-subtle mt-1">
            Last updated {timeAgo(metrics.lastUpdated)}
          </p>
        )}
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {summaryCards.map((card) => {
          const Icon = card.icon;
          return (
            <Card key={card.label} className="p-6">
              <div className="flex items-center gap-2 text-ink-subtle text-sm">
                <Icon className="h-4 w-4" />
                {card.label}
              </div>
              <p className="mt-2 text-2xl font-bold text-ink">{card.value}</p>
            </Card>
          );
        })}
      </div>

      {storageComics.length > 0 && (
        <Card>
          <div className="p-6 pb-4">
            <h2 className="text-lg font-semibold text-ink">Storage by Comic</h2>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-t border-border text-left text-ink-subtle">
                  <th className="px-6 py-3 font-medium">
                    <button className="inline-flex items-center gap-1" onClick={() => toggleStorageSort('comicName')}>
                      Comic <ArrowUpDown className="h-3 w-3" />
                    </button>
                  </th>
                  <th className="px-6 py-3 font-medium text-right">
                    <button className="inline-flex items-center gap-1 ml-auto" onClick={() => toggleStorageSort('imageCount')}>
                      Images <ArrowUpDown className="h-3 w-3" />
                    </button>
                  </th>
                  <th className="px-6 py-3 font-medium text-right">
                    <button className="inline-flex items-center gap-1 ml-auto" onClick={() => toggleStorageSort('totalBytes')}>
                      Storage <ArrowUpDown className="h-3 w-3" />
                    </button>
                  </th>
                  <th className="px-6 py-3 font-medium text-right">Avg Size</th>
                </tr>
              </thead>
              <tbody>
                {storageComics.map((comic) => (
                  <tr key={comic.comicName} className="border-t border-border hover:bg-surface-hover">
                    <td className="px-6 py-3 text-ink">{comic.comicName}</td>
                    <td className="px-6 py-3 text-right text-ink-subtle">{comic.imageCount.toLocaleString()}</td>
                    <td className="px-6 py-3 text-right text-ink-subtle">{formatBytes(comic.totalBytes)}</td>
                    <td className="px-6 py-3 text-right text-ink-subtle">
                      {comic.imageCount > 0 ? formatBytes(comic.totalBytes / comic.imageCount) : '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {accessComics.length > 0 && (
        <Card>
          <div className="p-6 pb-4">
            <h2 className="text-lg font-semibold text-ink">Access by Comic</h2>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-t border-border text-left text-ink-subtle">
                  <th className="px-6 py-3 font-medium">
                    <button className="inline-flex items-center gap-1" onClick={() => toggleAccessSort('comicName')}>
                      Comic <ArrowUpDown className="h-3 w-3" />
                    </button>
                  </th>
                  <th className="px-6 py-3 font-medium text-right">
                    <button className="inline-flex items-center gap-1 ml-auto" onClick={() => toggleAccessSort('accessCount')}>
                      Accesses <ArrowUpDown className="h-3 w-3" />
                    </button>
                  </th>
                  <th className="px-6 py-3 font-medium text-right">
                    <button className="inline-flex items-center gap-1 ml-auto" onClick={() => toggleAccessSort('averageAccessTimeMs')}>
                      Avg Response <ArrowUpDown className="h-3 w-3" />
                    </button>
                  </th>
                  <th className="px-6 py-3 font-medium text-right">Last Accessed</th>
                </tr>
              </thead>
              <tbody>
                {accessComics.map((comic) => (
                  <tr key={comic.comicName} className="border-t border-border hover:bg-surface-hover">
                    <td className="px-6 py-3 text-ink">{comic.comicName}</td>
                    <td className="px-6 py-3 text-right text-ink-subtle">{comic.accessCount.toLocaleString()}</td>
                    <td className="px-6 py-3 text-right text-ink-subtle">
                      {comic.averageAccessTimeMs != null ? `${comic.averageAccessTimeMs.toFixed(1)} ms` : '—'}
                    </td>
                    <td className="px-6 py-3 text-right text-ink-subtle">
                      {comic.lastAccessed ? timeAgo(comic.lastAccessed) : '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}
    </div>
  );
}
