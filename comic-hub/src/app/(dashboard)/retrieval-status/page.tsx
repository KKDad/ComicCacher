'use client';

import { useState } from 'react';
import { Card } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { Activity, CheckCircle, XCircle, Timer, ArrowUpDown, RefreshCw } from 'lucide-react';
import { useGetRetrievalSummaryQuery, useGetRetrievalRecordsQuery, RetrievalStatusEnum } from '@/generated/graphql';

function formatDuration(ms: number): string {
  if (ms < 1000) return `${ms.toFixed(0)} ms`;
  return `${(ms / 1000).toFixed(1)} s`;
}

function formatBytes(bytes: number): string {
  if (bytes === 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  const value = bytes / Math.pow(1024, i);
  return `${value.toFixed(i === 0 ? 0 : 1)} ${units[i]}`;
}

const statusColors: Record<RetrievalStatusEnum, string> = {
  [RetrievalStatusEnum.Success]: 'bg-green-100 text-green-800',
  [RetrievalStatusEnum.AuthenticationError]: 'bg-red-100 text-red-800',
  [RetrievalStatusEnum.NetworkError]: 'bg-red-100 text-red-800',
  [RetrievalStatusEnum.ParsingError]: 'bg-red-100 text-red-800',
  [RetrievalStatusEnum.StorageError]: 'bg-red-100 text-red-800',
  [RetrievalStatusEnum.ComicUnavailable]: 'bg-yellow-100 text-yellow-800',
  [RetrievalStatusEnum.UnknownError]: 'bg-gray-100 text-gray-800',
};

function StatusBadge({ status }: { status: RetrievalStatusEnum }) {
  return (
    <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${statusColors[status]}`}>
      {status.replace(/_/g, ' ')}
    </span>
  );
}

type SortKey = 'comicName' | 'comicDate' | 'status' | 'retrievalDurationMs';
type SortDir = 'asc' | 'desc';

export default function RetrievalStatusPage() {
  const { data: summaryData, isLoading: summaryLoading, error: summaryError } = useGetRetrievalSummaryQuery();
  const { data: recordsData, isLoading: recordsLoading, error: recordsError } = useGetRetrievalRecordsQuery();

  const [sortKey, setSortKey] = useState<SortKey>('comicDate');
  const [sortDir, setSortDir] = useState<SortDir>('desc');

  function toggleSort(key: SortKey) {
    if (sortKey === key) {
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortKey(key);
      setSortDir('desc');
    }
  }

  const isLoading = summaryLoading || recordsLoading;
  const error = summaryError || recordsError;

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-8 w-48" />
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {[...Array(4)].map((_, i) => (
            <Card key={i} className="p-6">
              <Skeleton className="h-4 w-24 mb-2" />
              <Skeleton className="h-8 w-32" />
            </Card>
          ))}
        </div>
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center py-20 text-center">
        <h2 className="text-xl font-semibold text-destructive">Failed to load retrieval status</h2>
        <p className="mt-2 text-muted-foreground">
          {error instanceof Error ? error.message : 'An unexpected error occurred.'}
        </p>
      </div>
    );
  }

  const summary = summaryData?.retrievalSummary;
  const records = recordsData?.retrievalRecords;

  if (!summary) {
    return (
      <div className="space-y-6">
        <h1 className="text-3xl font-bold text-ink">Retrieval Status</h1>
        <Card className="border-dashed">
          <div className="flex flex-col items-center justify-center p-12 text-center">
            <RefreshCw className="h-12 w-12 text-ink-muted mb-4" />
            <p className="text-ink-subtle mb-2">No retrieval data available</p>
            <p className="text-sm text-ink-muted">
              Retrieval data will appear once comic downloads have been attempted
            </p>
          </div>
        </Card>
      </div>
    );
  }

  const summaryCards = [
    { label: 'Total Attempts', value: summary.totalAttempts.toLocaleString(), icon: Activity },
    { label: 'Success Rate', value: `${summary.successRate.toFixed(1)}%`, icon: CheckCircle },
    { label: 'Failures', value: (summary.totalAttempts - summary.successCount).toLocaleString(), icon: XCircle },
    { label: 'Avg Duration', value: summary.averageDurationMs != null ? formatDuration(summary.averageDurationMs) : '—', icon: Timer },
  ];

  const sortedRecords = [...(records ?? [])].sort((a, b) => {
    const dir = sortDir === 'asc' ? 1 : -1;
    if (sortKey === 'comicName') return dir * a.comicName.localeCompare(b.comicName);
    if (sortKey === 'comicDate') return dir * String(a.comicDate).localeCompare(String(b.comicDate));
    if (sortKey === 'status') return dir * a.status.localeCompare(b.status);
    if (sortKey === 'retrievalDurationMs') return dir * ((a.retrievalDurationMs ?? 0) - (b.retrievalDurationMs ?? 0));
    return 0;
  });

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold text-ink">Retrieval Status</h1>

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

      {summary.byStatus && summary.byStatus.length > 0 && (
        <Card className="p-6">
          <h2 className="text-lg font-semibold text-ink mb-3">Status Breakdown</h2>
          <div className="flex flex-wrap gap-3">
            {summary.byStatus.map((s) => (
              <div key={s.status} className="flex items-center gap-2">
                <StatusBadge status={s.status} />
                <span className="text-sm text-ink-subtle">{s.count.toLocaleString()}</span>
              </div>
            ))}
          </div>
        </Card>
      )}

      {sortedRecords.length > 0 && (
        <Card>
          <div className="p-6 pb-4">
            <h2 className="text-lg font-semibold text-ink">Retrieval Records</h2>
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
                  <th className="px-6 py-3 font-medium">
                    <button className="inline-flex items-center gap-1" onClick={() => toggleSort('comicDate')}>
                      Date <ArrowUpDown className="h-3 w-3" />
                    </button>
                  </th>
                  <th className="px-6 py-3 font-medium">Source</th>
                  <th className="px-6 py-3 font-medium">
                    <button className="inline-flex items-center gap-1" onClick={() => toggleSort('status')}>
                      Status <ArrowUpDown className="h-3 w-3" />
                    </button>
                  </th>
                  <th className="px-6 py-3 font-medium text-right">
                    <button className="inline-flex items-center gap-1 ml-auto" onClick={() => toggleSort('retrievalDurationMs')}>
                      Duration <ArrowUpDown className="h-3 w-3" />
                    </button>
                  </th>
                  <th className="px-6 py-3 font-medium text-right">Image Size</th>
                  <th className="px-6 py-3 font-medium">Error</th>
                </tr>
              </thead>
              <tbody>
                {sortedRecords.map((record) => (
                  <tr key={record.id} className="border-t border-border hover:bg-surface-hover">
                    <td className="px-6 py-3 text-ink">{record.comicName}</td>
                    <td className="px-6 py-3 text-ink-subtle">{String(record.comicDate)}</td>
                    <td className="px-6 py-3 text-ink-subtle">{record.source ?? '—'}</td>
                    <td className="px-6 py-3"><StatusBadge status={record.status} /></td>
                    <td className="px-6 py-3 text-right text-ink-subtle">
                      {record.retrievalDurationMs != null ? formatDuration(record.retrievalDurationMs) : '—'}
                    </td>
                    <td className="px-6 py-3 text-right text-ink-subtle">
                      {record.imageSize != null ? formatBytes(record.imageSize) : '—'}
                    </td>
                    <td className="px-6 py-3 text-ink-subtle text-xs max-w-xs truncate">
                      {record.errorMessage ?? '—'}
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
