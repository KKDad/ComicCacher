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

  type SortKey = 'comicName' | 'imageCount' | 'totalBytes' | 'accessCount' | 'averageAccessTimeMs';
  const [sortKey, setSortKey] = useState<SortKey>('totalBytes');
  const [sortDir, setSortDir] = useState<SortDir>('desc');

  function toggleSort(key: SortKey) {
    if (sortKey === key) {
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortKey(key);
      setSortDir('desc');
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

  // Merge storage and access data by normalized name (strip spaces/punctuation, lowercase)
  // Storage uses directory names (e.g. "Sherman'sLagoon") while access uses display names
  // (e.g. "Sherman's Lagoon"), so we normalize to match them.
  const normalizeKey = (name: string) => name.toLowerCase().replace(/[\s''-]/g, '');

  const comicMap = new Map<string, {
    comicName: string;
    imageCount: number;
    totalBytes: number;
    accessCount: number;
    averageAccessTimeMs: number | null;
    lastAccessed: string | null;
  }>();

  for (const c of storage?.comics ?? []) {
    comicMap.set(normalizeKey(c.comicName), {
      comicName: c.comicName,
      imageCount: c.imageCount,
      totalBytes: c.totalBytes,
      accessCount: 0,
      averageAccessTimeMs: null,
      lastAccessed: null,
    });
  }

  for (const c of access?.comics ?? []) {
    const key = normalizeKey(c.comicName);
    const existing = comicMap.get(key);
    if (existing) {
      existing.accessCount = c.accessCount;
      existing.averageAccessTimeMs = c.averageAccessTimeMs ?? null;
      existing.lastAccessed = c.lastAccessed ?? null;
      // Prefer the display name with spaces if available
      if (c.comicName.includes(' ')) {
        existing.comicName = c.comicName;
      }
    } else {
      comicMap.set(key, {
        comicName: c.comicName,
        imageCount: 0,
        totalBytes: 0,
        accessCount: c.accessCount,
        averageAccessTimeMs: c.averageAccessTimeMs ?? null,
        lastAccessed: c.lastAccessed ?? null,
      });
    }
  }

  const combinedComics = [...comicMap.values()].sort((a, b) => {
    const dir = sortDir === 'asc' ? 1 : -1;
    if (sortKey === 'comicName') return dir * a.comicName.localeCompare(b.comicName);
    return dir * ((a[sortKey] ?? 0) - (b[sortKey] ?? 0));
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

      {combinedComics.length > 0 && (
        <Card>
          <div className="p-6 pb-4">
            <h2 className="text-lg font-semibold text-ink">Metrics by Comic</h2>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-t border-border text-left text-ink-subtle">
                  <th className="px-6 py-3 font-medium">
                    <button className="inline-flex items-center gap-1" onClick={() => toggleSort('comicName')}>
                      Comic <ArrowUpDown className="h-3 w-3" />
                    </button>
                  </th>
                  <th className="px-6 py-3 font-medium text-right">
                    <button className="inline-flex items-center gap-1 ml-auto" onClick={() => toggleSort('imageCount')}>
                      Images <ArrowUpDown className="h-3 w-3" />
                    </button>
                  </th>
                  <th className="px-6 py-3 font-medium text-right">
                    <button className="inline-flex items-center gap-1 ml-auto" onClick={() => toggleSort('totalBytes')}>
                      Storage <ArrowUpDown className="h-3 w-3" />
                    </button>
                  </th>
                  <th className="px-6 py-3 font-medium text-right">Avg Size</th>
                  <th className="px-6 py-3 font-medium text-right">
                    <button className="inline-flex items-center gap-1 ml-auto" onClick={() => toggleSort('accessCount')}>
                      Accesses <ArrowUpDown className="h-3 w-3" />
                    </button>
                  </th>
                  <th className="px-6 py-3 font-medium text-right">
                    <button className="inline-flex items-center gap-1 ml-auto" onClick={() => toggleSort('averageAccessTimeMs')}>
                      Avg Response <ArrowUpDown className="h-3 w-3" />
                    </button>
                  </th>
                  <th className="px-6 py-3 font-medium text-right">Last Accessed</th>
                </tr>
              </thead>
              <tbody>
                {combinedComics.map((comic) => (
                  <tr key={comic.comicName} className="border-t border-border hover:bg-surface-hover">
                    <td className="px-6 py-3 text-ink">{comic.comicName}</td>
                    <td className="px-6 py-3 text-right text-ink-subtle">{comic.imageCount.toLocaleString()}</td>
                    <td className="px-6 py-3 text-right text-ink-subtle">{formatBytes(comic.totalBytes)}</td>
                    <td className="px-6 py-3 text-right text-ink-subtle">
                      {comic.imageCount > 0 ? formatBytes(comic.totalBytes / comic.imageCount) : '—'}
                    </td>
                    <td className="px-6 py-3 text-right text-ink-subtle">{comic.accessCount > 0 ? comic.accessCount.toLocaleString() : '—'}</td>
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
