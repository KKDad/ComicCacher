import { RetrievalStatusEnum } from '@/generated/graphql';
import { statusLabel } from './health';

const statusColors: Record<RetrievalStatusEnum, string> = {
  [RetrievalStatusEnum.Success]: 'bg-success-subtle text-success',
  [RetrievalStatusEnum.AuthenticationError]: 'bg-error-subtle text-error',
  [RetrievalStatusEnum.NetworkError]: 'bg-error-subtle text-error',
  [RetrievalStatusEnum.ParsingError]: 'bg-error-subtle text-error',
  [RetrievalStatusEnum.RateLimited]: 'bg-warning-subtle text-warning',
  [RetrievalStatusEnum.StorageError]: 'bg-error-subtle text-error',
  [RetrievalStatusEnum.ComicUnavailable]: 'bg-warning-subtle text-warning',
  [RetrievalStatusEnum.UnknownError]: 'bg-muted text-muted-foreground',
};

export function StatusBadge({ status }: { status: RetrievalStatusEnum }) {
  return (
    <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium whitespace-nowrap ${statusColors[status]}`}>
      {statusLabel(status)}
    </span>
  );
}
