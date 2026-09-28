import type { GetBatchSchedulersQuery, GetRecentBatchJobsQuery } from '@/generated/graphql';

// Shapes of the batch-job fields the operations select. Codegen emits only operation types,
// so these are derived from the query results rather than the schema.
export type BatchJob = GetRecentBatchJobsQuery['recentBatchJobs'][number];
export type BatchSchedulerInfo = GetBatchSchedulersQuery['batchSchedulers'][number];
export type BatchJobParameter = BatchSchedulerInfo['availableParameters'][number];
