/** Internal type. DO NOT USE DIRECTLY. */
type Exact<T extends { [key: string]: unknown }> = { [K in keyof T]: T[K] };
/** Internal type. DO NOT USE DIRECTLY. */
export type Incremental<T> = T | { [P in keyof T]?: P extends ' $fragmentName' | '__typename' ? T[P] : never };
import { DocumentTypeDecoration } from '@graphql-typed-document-node/core';
import { useMutation, useQuery, useInfiniteQuery, UseMutationOptions, UseQueryOptions, UseInfiniteQueryOptions, InfiniteData } from '@tanstack/react-query';
import { fetcher } from '../lib/graphql-client';
/** Data types for batch job parameters. */
export const BatchJobParameterType = {
  Boolean: 'BOOLEAN',
  Enum: 'ENUM',
  Integer: 'INTEGER',
  String: 'STRING'
} as const;

export type BatchJobParameterType = typeof BatchJobParameterType[keyof typeof BatchJobParameterType];
/** Possible batch job status values. */
export const BatchStatusEnum = {
  Abandoned: 'ABANDONED',
  Completed: 'COMPLETED',
  Failed: 'FAILED',
  Started: 'STARTED',
  Starting: 'STARTING',
  Stopped: 'STOPPED',
  Stopping: 'STOPPING',
  Unknown: 'UNKNOWN'
} as const;

export type BatchStatusEnum = typeof BatchStatusEnum[keyof typeof BatchStatusEnum];
/**
 * Standard error codes returned by the API.
 * These codes appear in error responses to help clients handle errors programmatically.
 */
export const ErrorCode = {
  /** Comic with the specified ID does not exist. */
  ComicNotFound: 'COMIC_NOT_FOUND',
  /** User does not have permission for this operation. */
  Forbidden: 'FORBIDDEN',
  /** Internal server error. */
  InternalError: 'INTERNAL_ERROR',
  /** Invalid credentials provided. */
  InvalidCredentials: 'INVALID_CREDENTIALS',
  /** Password does not meet requirements. */
  InvalidPassword: 'INVALID_PASSWORD',
  /** Token is invalid or malformed. */
  InvalidToken: 'INVALID_TOKEN',
  /** Requested resource was not found. */
  NotFound: 'NOT_FOUND',
  /** Rate limit exceeded. */
  RateLimited: 'RATE_LIMITED',
  /** Comic strip not available for the requested date. */
  StripNotFound: 'STRIP_NOT_FOUND',
  /** Token has expired. */
  TokenExpired: 'TOKEN_EXPIRED',
  /** Authentication required but not provided. */
  Unauthenticated: 'UNAUTHENTICATED',
  /** Username or email already exists. */
  UserAlreadyExists: 'USER_ALREADY_EXISTS',
  /** User account not found. */
  UserNotFound: 'USER_NOT_FOUND',
  /** Input validation failed. */
  ValidationError: 'VALIDATION_ERROR'
} as const;

export type ErrorCode = typeof ErrorCode[keyof typeof ErrorCode];
/** Input for user login. */
export type LoginInput = {
  /** User's password. */
  password: string;
  /** Username or email address. */
  username: string;
};

/** Input for new user registration. */
export type RegisterInput = {
  /** User's display name. */
  displayName?: string | null | undefined;
  /** User's email address. */
  email: string;
  /** User's password. */
  password: string;
  /** Desired username (must be unique). */
  username: string;
};

/**
 * Possible retrieval status values.
 * Mirrors the Java ComicRetrievalStatus enum 1:1.
 */
export const RetrievalStatusEnum = {
  AuthenticationError: 'AUTHENTICATION_ERROR',
  ComicUnavailable: 'COMIC_UNAVAILABLE',
  NetworkError: 'NETWORK_ERROR',
  ParsingError: 'PARSING_ERROR',
  RateLimited: 'RATE_LIMITED',
  StorageError: 'STORAGE_ERROR',
  Success: 'SUCCESS',
  UnknownError: 'UNKNOWN_ERROR'
} as const;

export type RetrievalStatusEnum = typeof RetrievalStatusEnum[keyof typeof RetrievalStatusEnum];
export type LoginMutationVariables = Exact<{
  input: LoginInput;
}>;


export type LoginMutation = { login: { token: string, refreshToken: string, username: string, displayName: string | null } };

export type RegisterMutationVariables = Exact<{
  input: RegisterInput;
}>;


export type RegisterMutation = { register: { token: string, refreshToken: string, username: string, displayName: string | null } };

export type RefreshTokenMutationVariables = Exact<{
  refreshToken: string;
}>;


export type RefreshTokenMutation = { refreshToken: { token: string, refreshToken: string, username: string, displayName: string | null } };

export type ForgotPasswordMutationVariables = Exact<{
  email: string;
}>;


export type ForgotPasswordMutation = { forgotPassword: boolean };

export type ResetPasswordMutationVariables = Exact<{
  token: string;
  newPassword: string;
}>;


export type ResetPasswordMutation = { resetPassword: { token: string, refreshToken: string, username: string, displayName: string | null } };

export type LogoutMutationVariables = Exact<{ [key: string]: never; }>;


export type LogoutMutation = { logout: boolean };

export type GetMeQueryVariables = Exact<{ [key: string]: never; }>;


export type GetMeQuery = { me: { username: string, email: string | null, displayName: string | null, created: string | null, lastLogin: string | null, roles: Array<string> } | null };

export type GetBatchSchedulersQueryVariables = Exact<{ [key: string]: never; }>;


export type GetBatchSchedulersQuery = { batchSchedulers: Array<{ jobName: string, cronExpression: string, description: string | null, timezone: string, nextRunTime: string | null, enabled: boolean, paused: boolean, lastToggled: string | null, toggledBy: string | null, availableParameters: Array<{ name: string, label: string, type: BatchJobParameterType, required: boolean, defaultValue: string | null, options: Array<{ value: string, label: string }> | null }> }> };

export type GetRecentBatchJobsQueryVariables = Exact<{
  count?: number | null | undefined;
}>;


export type GetRecentBatchJobsQuery = { recentBatchJobs: Array<{ executionId: number, jobName: string, status: BatchStatusEnum, startTime: string, endTime: string | null, durationMs: number | null, exitCode: string | null, exitDescription: string | null, steps: Array<{ stepName: string, status: BatchStatusEnum, readCount: number, writeCount: number, filterCount: number, skipCount: number, commitCount: number, rollbackCount: number, startTime: string | null, endTime: string | null }> | null }> };

export type GetBatchJobLogQueryVariables = Exact<{
  executionId: number;
  jobName: string;
}>;


export type GetBatchJobLogQuery = { batchJobLog: string | null };

export type TriggerJobMutationVariables = Exact<{
  jobName: string;
  parameters?: any;
}>;


export type TriggerJobMutation = { triggerJob: { batchJob: { executionId: number, jobName: string, status: BatchStatusEnum, startTime: string } | null, errors: Array<{ message: string, field: string | null, code: ErrorCode | null }> } };

export type ToggleJobSchedulerMutationVariables = Exact<{
  jobName: string;
  paused: boolean;
}>;


export type ToggleJobSchedulerMutation = { toggleJobScheduler: { scheduler: { jobName: string, paused: boolean, lastToggled: string | null, toggledBy: string | null } | null, errors: Array<{ message: string, field: string | null, code: ErrorCode | null }> } };

export type GetUserPreferencesQueryVariables = Exact<{ [key: string]: never; }>;


export type GetUserPreferencesQuery = { preferences: { username: string, favoriteComics: Array<number>, displaySettings: any, lastReadDates: Array<{ comicId: number, date: string }> } | null };

export type GetComicsQueryVariables = Exact<{
  first?: number | null | undefined;
  after?: string | null | undefined;
}>;


export type GetComicsQuery = { comics: { totalCount: number, edges: Array<{ cursor: string, node: { id: number, name: string, description: string | null, oldest: string | null, newest: string | null, avatarUrl: string | null, lastStrip: { imageUrl: string | null, date: string } | null } }>, pageInfo: { hasNextPage: boolean, hasPreviousPage: boolean, startCursor: string | null, endCursor: string | null } } };

export type GetComicQueryVariables = Exact<{
  id: number;
}>;


export type GetComicQuery = { comic: { id: number, name: string, description: string | null, author: string | null, source: string | null, sourceIdentifier: string | null, oldest: string | null, newest: string | null, avatarUrl: string | null, lastStrip: { imageUrl: string | null, date: string, width: number | null, height: number | null } | null, firstStrip: { imageUrl: string | null, date: string } | null } | null };

export type SearchComicsQueryVariables = Exact<{
  query: string;
}>;


export type SearchComicsQuery = { search: { comics: Array<{ id: number, name: string, description: string | null, oldest: string | null, newest: string | null, avatarUrl: string | null, lastStrip: { imageUrl: string | null, date: string } | null }> } };

export type AddFavoriteMutationVariables = Exact<{
  comicId: number;
}>;


export type AddFavoriteMutation = { addFavorite: { preference: { favoriteComics: Array<number> } | null, errors: Array<{ message: string, field: string | null, code: ErrorCode | null }> } };

export type RemoveFavoriteMutationVariables = Exact<{
  comicId: number;
}>;


export type RemoveFavoriteMutation = { removeFavorite: { preference: { favoriteComics: Array<number> } | null, errors: Array<{ message: string, field: string | null, code: ErrorCode | null }> } };

export type UpdateLastReadMutationVariables = Exact<{
  comicId: number;
  date: string;
}>;


export type UpdateLastReadMutation = { updateLastRead: { preference: { lastReadDates: Array<{ comicId: number, date: string }> } | null, errors: Array<{ message: string, field: string | null, code: ErrorCode | null }> } };

export type UpdateDisplaySettingsMutationVariables = Exact<{
  settings: any;
}>;


export type UpdateDisplaySettingsMutation = { updateDisplaySettings: { preference: { displaySettings: any } | null, errors: Array<{ message: string, field: string | null, code: ErrorCode | null }> } };

export type GetStripWindowQueryVariables = Exact<{
  comicId: number;
  center: string;
  before: number;
  after: number;
}>;


export type GetStripWindowQuery = { comic: { id: number, name: string, oldest: string | null, newest: string | null, avatarUrl: string | null, stripWindow: Array<{ date: string, available: boolean, imageUrl: string | null, width: number | null, height: number | null }> } | null };

export type GetRandomStripQueryVariables = Exact<{
  comicId?: number | null | undefined;
}>;


export type GetRandomStripQuery = { randomStrip: { date: string, available: boolean, imageUrl: string | null, width: number | null, height: number | null } | null };

export type GetComicsForDateQueryVariables = Exact<{
  first?: number | null | undefined;
  date: string;
}>;


export type GetComicsForDateQuery = { comics: { totalCount: number, edges: Array<{ node: { id: number, name: string, avatarUrl: string | null, oldest: string | null, newest: string | null, strip: { date: string, available: boolean, imageUrl: string | null, width: number | null, height: number | null, transcript: string | null } | null } }> } };

export type GetCombinedMetricsQueryVariables = Exact<{ [key: string]: never; }>;


export type GetCombinedMetricsQuery = { combinedMetrics: { lastUpdated: string | null, storage: { totalBytes: number | null, comicCount: number | null, lastUpdated: string | null, comics: Array<{ comicId: number | null, comicName: string, totalBytes: number, imageCount: number, yearlyBreakdown: Array<{ year: number, bytes: number, imageCount: number }> | null }> | null } | null, access: { totalAccesses: number | null, lastUpdated: string | null, comics: Array<{ comicName: string, accessCount: number, averageAccessTimeMs: number | null, lastAccessed: string | null }> | null } | null } | null };

export type GetRetrievalSummaryQueryVariables = Exact<{
  fromDate?: string | null | undefined;
  toDate?: string | null | undefined;
}>;


export type GetRetrievalSummaryQuery = { retrievalSummary: { totalAttempts: number, successCount: number, failureCount: number, skippedCount: number, successRate: number, averageDurationMs: number | null, byStatus: Array<{ status: RetrievalStatusEnum, count: number }> | null, byComic: Array<{ comicName: string, totalAttempts: number, successCount: number, failureCount: number }> | null } };

export type GetRetrievalRecordsQueryVariables = Exact<{
  comicName?: string | null | undefined;
  status?: RetrievalStatusEnum | null | undefined;
  fromDate?: string | null | undefined;
  toDate?: string | null | undefined;
  limit?: number | null | undefined;
}>;


export type GetRetrievalRecordsQuery = { retrievalRecords: Array<{ id: string, comicName: string, comicDate: string, source: string | null, status: RetrievalStatusEnum, retrievalDurationMs: number | null, imageSize: number | null, httpStatusCode: number | null, errorMessage: string | null }> };


export class TypedDocumentString<TResult, TVariables>
  extends String
  implements DocumentTypeDecoration<TResult, TVariables>
{
  __apiType?: NonNullable<DocumentTypeDecoration<TResult, TVariables>['__apiType']>;
  private value: string;
  public __meta__?: Record<string, any> | undefined;

  constructor(value: string, __meta__?: Record<string, any> | undefined) {
    super(value);
    this.value = value;
    this.__meta__ = __meta__;
  }

  override toString(): string & DocumentTypeDecoration<TResult, TVariables> {
    return this.value;
  }
}

export const LoginDocument = new TypedDocumentString(`
    mutation Login($input: LoginInput!) {
  login(input: $input) {
    token
    refreshToken
    username
    displayName
  }
}
    `);

export const useLoginMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<LoginMutation, TError, LoginMutationVariables, TContext>) => {
    
    return useMutation<LoginMutation, TError, LoginMutationVariables, TContext>(
      {
    mutationKey: ['Login'],
    mutationFn: (variables?: LoginMutationVariables) => fetcher<LoginMutation, LoginMutationVariables>(LoginDocument, variables)(),
    ...options
  }
    )};


useLoginMutation.fetcher = (variables: LoginMutationVariables, options?: RequestInit['headers']) => fetcher<LoginMutation, LoginMutationVariables>(LoginDocument, variables, options);

export const RegisterDocument = new TypedDocumentString(`
    mutation Register($input: RegisterInput!) {
  register(input: $input) {
    token
    refreshToken
    username
    displayName
  }
}
    `);

export const useRegisterMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<RegisterMutation, TError, RegisterMutationVariables, TContext>) => {
    
    return useMutation<RegisterMutation, TError, RegisterMutationVariables, TContext>(
      {
    mutationKey: ['Register'],
    mutationFn: (variables?: RegisterMutationVariables) => fetcher<RegisterMutation, RegisterMutationVariables>(RegisterDocument, variables)(),
    ...options
  }
    )};


useRegisterMutation.fetcher = (variables: RegisterMutationVariables, options?: RequestInit['headers']) => fetcher<RegisterMutation, RegisterMutationVariables>(RegisterDocument, variables, options);

export const RefreshTokenDocument = new TypedDocumentString(`
    mutation RefreshToken($refreshToken: String!) {
  refreshToken(refreshToken: $refreshToken) {
    token
    refreshToken
    username
    displayName
  }
}
    `);

export const useRefreshTokenMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<RefreshTokenMutation, TError, RefreshTokenMutationVariables, TContext>) => {
    
    return useMutation<RefreshTokenMutation, TError, RefreshTokenMutationVariables, TContext>(
      {
    mutationKey: ['RefreshToken'],
    mutationFn: (variables?: RefreshTokenMutationVariables) => fetcher<RefreshTokenMutation, RefreshTokenMutationVariables>(RefreshTokenDocument, variables)(),
    ...options
  }
    )};


useRefreshTokenMutation.fetcher = (variables: RefreshTokenMutationVariables, options?: RequestInit['headers']) => fetcher<RefreshTokenMutation, RefreshTokenMutationVariables>(RefreshTokenDocument, variables, options);

export const ForgotPasswordDocument = new TypedDocumentString(`
    mutation ForgotPassword($email: String!) {
  forgotPassword(email: $email)
}
    `);

export const useForgotPasswordMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<ForgotPasswordMutation, TError, ForgotPasswordMutationVariables, TContext>) => {
    
    return useMutation<ForgotPasswordMutation, TError, ForgotPasswordMutationVariables, TContext>(
      {
    mutationKey: ['ForgotPassword'],
    mutationFn: (variables?: ForgotPasswordMutationVariables) => fetcher<ForgotPasswordMutation, ForgotPasswordMutationVariables>(ForgotPasswordDocument, variables)(),
    ...options
  }
    )};


useForgotPasswordMutation.fetcher = (variables: ForgotPasswordMutationVariables, options?: RequestInit['headers']) => fetcher<ForgotPasswordMutation, ForgotPasswordMutationVariables>(ForgotPasswordDocument, variables, options);

export const ResetPasswordDocument = new TypedDocumentString(`
    mutation ResetPassword($token: String!, $newPassword: String!) {
  resetPassword(token: $token, newPassword: $newPassword) {
    token
    refreshToken
    username
    displayName
  }
}
    `);

export const useResetPasswordMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<ResetPasswordMutation, TError, ResetPasswordMutationVariables, TContext>) => {
    
    return useMutation<ResetPasswordMutation, TError, ResetPasswordMutationVariables, TContext>(
      {
    mutationKey: ['ResetPassword'],
    mutationFn: (variables?: ResetPasswordMutationVariables) => fetcher<ResetPasswordMutation, ResetPasswordMutationVariables>(ResetPasswordDocument, variables)(),
    ...options
  }
    )};


useResetPasswordMutation.fetcher = (variables: ResetPasswordMutationVariables, options?: RequestInit['headers']) => fetcher<ResetPasswordMutation, ResetPasswordMutationVariables>(ResetPasswordDocument, variables, options);

export const LogoutDocument = new TypedDocumentString(`
    mutation Logout {
  logout
}
    `);

export const useLogoutMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<LogoutMutation, TError, LogoutMutationVariables, TContext>) => {
    
    return useMutation<LogoutMutation, TError, LogoutMutationVariables, TContext>(
      {
    mutationKey: ['Logout'],
    mutationFn: (variables?: LogoutMutationVariables) => fetcher<LogoutMutation, LogoutMutationVariables>(LogoutDocument, variables)(),
    ...options
  }
    )};


useLogoutMutation.fetcher = (variables?: LogoutMutationVariables, options?: RequestInit['headers']) => fetcher<LogoutMutation, LogoutMutationVariables>(LogoutDocument, variables, options);

export const GetMeDocument = new TypedDocumentString(`
    query GetMe {
  me {
    username
    email
    displayName
    created
    lastLogin
    roles
  }
}
    `);

export const useGetMeQuery = <
      TData = GetMeQuery,
      TError = unknown
    >(
      variables?: GetMeQueryVariables,
      options?: Omit<UseQueryOptions<GetMeQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<GetMeQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<GetMeQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['GetMe'] : ['GetMe', variables],
    queryFn: fetcher<GetMeQuery, GetMeQueryVariables>(GetMeDocument, variables),
    ...options
  }
    )};

useGetMeQuery.getKey = (variables?: GetMeQueryVariables) => variables === undefined ? ['GetMe'] : ['GetMe', variables];

export const useInfiniteGetMeQuery = <
      TData = InfiniteData<GetMeQuery>,
      TError = unknown
    >(
      variables: GetMeQueryVariables,
      options: Omit<UseInfiniteQueryOptions<GetMeQuery, TError, TData>, 'queryKey'> & { queryKey?: UseInfiniteQueryOptions<GetMeQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useInfiniteQuery<GetMeQuery, TError, TData>(
      (() => {
    const { queryKey: optionsQueryKey, ...restOptions } = options;
    return {
      queryKey: optionsQueryKey ?? variables === undefined ? ['GetMe.infinite'] : ['GetMe.infinite', variables],
      queryFn: (metaData) => fetcher<GetMeQuery, GetMeQueryVariables>(GetMeDocument, {...variables, ...(metaData.pageParam ?? {})})(),
      ...restOptions
    }
  })()
    )};

useInfiniteGetMeQuery.getKey = (variables?: GetMeQueryVariables) => variables === undefined ? ['GetMe.infinite'] : ['GetMe.infinite', variables];


useGetMeQuery.fetcher = (variables?: GetMeQueryVariables, options?: RequestInit['headers']) => fetcher<GetMeQuery, GetMeQueryVariables>(GetMeDocument, variables, options);

export const GetBatchSchedulersDocument = new TypedDocumentString(`
    query GetBatchSchedulers {
  batchSchedulers {
    jobName
    cronExpression
    description
    timezone
    nextRunTime
    enabled
    paused
    lastToggled
    toggledBy
    availableParameters {
      name
      label
      type
      required
      defaultValue
      options {
        value
        label
      }
    }
  }
}
    `);

export const useGetBatchSchedulersQuery = <
      TData = GetBatchSchedulersQuery,
      TError = unknown
    >(
      variables?: GetBatchSchedulersQueryVariables,
      options?: Omit<UseQueryOptions<GetBatchSchedulersQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<GetBatchSchedulersQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<GetBatchSchedulersQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['GetBatchSchedulers'] : ['GetBatchSchedulers', variables],
    queryFn: fetcher<GetBatchSchedulersQuery, GetBatchSchedulersQueryVariables>(GetBatchSchedulersDocument, variables),
    ...options
  }
    )};

useGetBatchSchedulersQuery.getKey = (variables?: GetBatchSchedulersQueryVariables) => variables === undefined ? ['GetBatchSchedulers'] : ['GetBatchSchedulers', variables];

export const useInfiniteGetBatchSchedulersQuery = <
      TData = InfiniteData<GetBatchSchedulersQuery>,
      TError = unknown
    >(
      variables: GetBatchSchedulersQueryVariables,
      options: Omit<UseInfiniteQueryOptions<GetBatchSchedulersQuery, TError, TData>, 'queryKey'> & { queryKey?: UseInfiniteQueryOptions<GetBatchSchedulersQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useInfiniteQuery<GetBatchSchedulersQuery, TError, TData>(
      (() => {
    const { queryKey: optionsQueryKey, ...restOptions } = options;
    return {
      queryKey: optionsQueryKey ?? variables === undefined ? ['GetBatchSchedulers.infinite'] : ['GetBatchSchedulers.infinite', variables],
      queryFn: (metaData) => fetcher<GetBatchSchedulersQuery, GetBatchSchedulersQueryVariables>(GetBatchSchedulersDocument, {...variables, ...(metaData.pageParam ?? {})})(),
      ...restOptions
    }
  })()
    )};

useInfiniteGetBatchSchedulersQuery.getKey = (variables?: GetBatchSchedulersQueryVariables) => variables === undefined ? ['GetBatchSchedulers.infinite'] : ['GetBatchSchedulers.infinite', variables];


useGetBatchSchedulersQuery.fetcher = (variables?: GetBatchSchedulersQueryVariables, options?: RequestInit['headers']) => fetcher<GetBatchSchedulersQuery, GetBatchSchedulersQueryVariables>(GetBatchSchedulersDocument, variables, options);

export const GetRecentBatchJobsDocument = new TypedDocumentString(`
    query GetRecentBatchJobs($count: Int = 10) {
  recentBatchJobs(count: $count) {
    executionId
    jobName
    status
    startTime
    endTime
    durationMs
    exitCode
    exitDescription
    steps {
      stepName
      status
      readCount
      writeCount
      filterCount
      skipCount
      commitCount
      rollbackCount
      startTime
      endTime
    }
  }
}
    `);

export const useGetRecentBatchJobsQuery = <
      TData = GetRecentBatchJobsQuery,
      TError = unknown
    >(
      variables?: GetRecentBatchJobsQueryVariables,
      options?: Omit<UseQueryOptions<GetRecentBatchJobsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<GetRecentBatchJobsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<GetRecentBatchJobsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['GetRecentBatchJobs'] : ['GetRecentBatchJobs', variables],
    queryFn: fetcher<GetRecentBatchJobsQuery, GetRecentBatchJobsQueryVariables>(GetRecentBatchJobsDocument, variables),
    ...options
  }
    )};

useGetRecentBatchJobsQuery.getKey = (variables?: GetRecentBatchJobsQueryVariables) => variables === undefined ? ['GetRecentBatchJobs'] : ['GetRecentBatchJobs', variables];

export const useInfiniteGetRecentBatchJobsQuery = <
      TData = InfiniteData<GetRecentBatchJobsQuery>,
      TError = unknown
    >(
      variables: GetRecentBatchJobsQueryVariables,
      options: Omit<UseInfiniteQueryOptions<GetRecentBatchJobsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseInfiniteQueryOptions<GetRecentBatchJobsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useInfiniteQuery<GetRecentBatchJobsQuery, TError, TData>(
      (() => {
    const { queryKey: optionsQueryKey, ...restOptions } = options;
    return {
      queryKey: optionsQueryKey ?? variables === undefined ? ['GetRecentBatchJobs.infinite'] : ['GetRecentBatchJobs.infinite', variables],
      queryFn: (metaData) => fetcher<GetRecentBatchJobsQuery, GetRecentBatchJobsQueryVariables>(GetRecentBatchJobsDocument, {...variables, ...(metaData.pageParam ?? {})})(),
      ...restOptions
    }
  })()
    )};

useInfiniteGetRecentBatchJobsQuery.getKey = (variables?: GetRecentBatchJobsQueryVariables) => variables === undefined ? ['GetRecentBatchJobs.infinite'] : ['GetRecentBatchJobs.infinite', variables];


useGetRecentBatchJobsQuery.fetcher = (variables?: GetRecentBatchJobsQueryVariables, options?: RequestInit['headers']) => fetcher<GetRecentBatchJobsQuery, GetRecentBatchJobsQueryVariables>(GetRecentBatchJobsDocument, variables, options);

export const GetBatchJobLogDocument = new TypedDocumentString(`
    query GetBatchJobLog($executionId: Int!, $jobName: String!) {
  batchJobLog(executionId: $executionId, jobName: $jobName)
}
    `);

export const useGetBatchJobLogQuery = <
      TData = GetBatchJobLogQuery,
      TError = unknown
    >(
      variables: GetBatchJobLogQueryVariables,
      options?: Omit<UseQueryOptions<GetBatchJobLogQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<GetBatchJobLogQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<GetBatchJobLogQuery, TError, TData>(
      {
    queryKey: ['GetBatchJobLog', variables],
    queryFn: fetcher<GetBatchJobLogQuery, GetBatchJobLogQueryVariables>(GetBatchJobLogDocument, variables),
    ...options
  }
    )};

useGetBatchJobLogQuery.getKey = (variables: GetBatchJobLogQueryVariables) => ['GetBatchJobLog', variables];

export const useInfiniteGetBatchJobLogQuery = <
      TData = InfiniteData<GetBatchJobLogQuery>,
      TError = unknown
    >(
      variables: GetBatchJobLogQueryVariables,
      options: Omit<UseInfiniteQueryOptions<GetBatchJobLogQuery, TError, TData>, 'queryKey'> & { queryKey?: UseInfiniteQueryOptions<GetBatchJobLogQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useInfiniteQuery<GetBatchJobLogQuery, TError, TData>(
      (() => {
    const { queryKey: optionsQueryKey, ...restOptions } = options;
    return {
      queryKey: optionsQueryKey ?? ['GetBatchJobLog.infinite', variables],
      queryFn: (metaData) => fetcher<GetBatchJobLogQuery, GetBatchJobLogQueryVariables>(GetBatchJobLogDocument, {...variables, ...(metaData.pageParam ?? {})})(),
      ...restOptions
    }
  })()
    )};

useInfiniteGetBatchJobLogQuery.getKey = (variables: GetBatchJobLogQueryVariables) => ['GetBatchJobLog.infinite', variables];


useGetBatchJobLogQuery.fetcher = (variables: GetBatchJobLogQueryVariables, options?: RequestInit['headers']) => fetcher<GetBatchJobLogQuery, GetBatchJobLogQueryVariables>(GetBatchJobLogDocument, variables, options);

export const TriggerJobDocument = new TypedDocumentString(`
    mutation TriggerJob($jobName: String!, $parameters: JSON) {
  triggerJob(jobName: $jobName, parameters: $parameters) {
    batchJob {
      executionId
      jobName
      status
      startTime
    }
    errors {
      message
      field
      code
    }
  }
}
    `);

export const useTriggerJobMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<TriggerJobMutation, TError, TriggerJobMutationVariables, TContext>) => {
    
    return useMutation<TriggerJobMutation, TError, TriggerJobMutationVariables, TContext>(
      {
    mutationKey: ['TriggerJob'],
    mutationFn: (variables?: TriggerJobMutationVariables) => fetcher<TriggerJobMutation, TriggerJobMutationVariables>(TriggerJobDocument, variables)(),
    ...options
  }
    )};


useTriggerJobMutation.fetcher = (variables: TriggerJobMutationVariables, options?: RequestInit['headers']) => fetcher<TriggerJobMutation, TriggerJobMutationVariables>(TriggerJobDocument, variables, options);

export const ToggleJobSchedulerDocument = new TypedDocumentString(`
    mutation ToggleJobScheduler($jobName: String!, $paused: Boolean!) {
  toggleJobScheduler(jobName: $jobName, paused: $paused) {
    scheduler {
      jobName
      paused
      lastToggled
      toggledBy
    }
    errors {
      message
      field
      code
    }
  }
}
    `);

export const useToggleJobSchedulerMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<ToggleJobSchedulerMutation, TError, ToggleJobSchedulerMutationVariables, TContext>) => {
    
    return useMutation<ToggleJobSchedulerMutation, TError, ToggleJobSchedulerMutationVariables, TContext>(
      {
    mutationKey: ['ToggleJobScheduler'],
    mutationFn: (variables?: ToggleJobSchedulerMutationVariables) => fetcher<ToggleJobSchedulerMutation, ToggleJobSchedulerMutationVariables>(ToggleJobSchedulerDocument, variables)(),
    ...options
  }
    )};


useToggleJobSchedulerMutation.fetcher = (variables: ToggleJobSchedulerMutationVariables, options?: RequestInit['headers']) => fetcher<ToggleJobSchedulerMutation, ToggleJobSchedulerMutationVariables>(ToggleJobSchedulerDocument, variables, options);

export const GetUserPreferencesDocument = new TypedDocumentString(`
    query GetUserPreferences {
  preferences {
    username
    favoriteComics
    lastReadDates {
      comicId
      date
    }
    displaySettings
  }
}
    `);

export const useGetUserPreferencesQuery = <
      TData = GetUserPreferencesQuery,
      TError = unknown
    >(
      variables?: GetUserPreferencesQueryVariables,
      options?: Omit<UseQueryOptions<GetUserPreferencesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<GetUserPreferencesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<GetUserPreferencesQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['GetUserPreferences'] : ['GetUserPreferences', variables],
    queryFn: fetcher<GetUserPreferencesQuery, GetUserPreferencesQueryVariables>(GetUserPreferencesDocument, variables),
    ...options
  }
    )};

useGetUserPreferencesQuery.getKey = (variables?: GetUserPreferencesQueryVariables) => variables === undefined ? ['GetUserPreferences'] : ['GetUserPreferences', variables];

export const useInfiniteGetUserPreferencesQuery = <
      TData = InfiniteData<GetUserPreferencesQuery>,
      TError = unknown
    >(
      variables: GetUserPreferencesQueryVariables,
      options: Omit<UseInfiniteQueryOptions<GetUserPreferencesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseInfiniteQueryOptions<GetUserPreferencesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useInfiniteQuery<GetUserPreferencesQuery, TError, TData>(
      (() => {
    const { queryKey: optionsQueryKey, ...restOptions } = options;
    return {
      queryKey: optionsQueryKey ?? variables === undefined ? ['GetUserPreferences.infinite'] : ['GetUserPreferences.infinite', variables],
      queryFn: (metaData) => fetcher<GetUserPreferencesQuery, GetUserPreferencesQueryVariables>(GetUserPreferencesDocument, {...variables, ...(metaData.pageParam ?? {})})(),
      ...restOptions
    }
  })()
    )};

useInfiniteGetUserPreferencesQuery.getKey = (variables?: GetUserPreferencesQueryVariables) => variables === undefined ? ['GetUserPreferences.infinite'] : ['GetUserPreferences.infinite', variables];


useGetUserPreferencesQuery.fetcher = (variables?: GetUserPreferencesQueryVariables, options?: RequestInit['headers']) => fetcher<GetUserPreferencesQuery, GetUserPreferencesQueryVariables>(GetUserPreferencesDocument, variables, options);

export const GetComicsDocument = new TypedDocumentString(`
    query GetComics($first: Int, $after: String) {
  comics(first: $first, after: $after) {
    edges {
      cursor
      node {
        id
        name
        description
        oldest
        newest
        avatarUrl
        lastStrip {
          imageUrl
          date
        }
      }
    }
    pageInfo {
      hasNextPage
      hasPreviousPage
      startCursor
      endCursor
    }
    totalCount
  }
}
    `);

export const useGetComicsQuery = <
      TData = GetComicsQuery,
      TError = unknown
    >(
      variables?: GetComicsQueryVariables,
      options?: Omit<UseQueryOptions<GetComicsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<GetComicsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<GetComicsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['GetComics'] : ['GetComics', variables],
    queryFn: fetcher<GetComicsQuery, GetComicsQueryVariables>(GetComicsDocument, variables),
    ...options
  }
    )};

useGetComicsQuery.getKey = (variables?: GetComicsQueryVariables) => variables === undefined ? ['GetComics'] : ['GetComics', variables];

export const useInfiniteGetComicsQuery = <
      TData = InfiniteData<GetComicsQuery>,
      TError = unknown
    >(
      variables: GetComicsQueryVariables,
      options: Omit<UseInfiniteQueryOptions<GetComicsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseInfiniteQueryOptions<GetComicsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useInfiniteQuery<GetComicsQuery, TError, TData>(
      (() => {
    const { queryKey: optionsQueryKey, ...restOptions } = options;
    return {
      queryKey: optionsQueryKey ?? variables === undefined ? ['GetComics.infinite'] : ['GetComics.infinite', variables],
      queryFn: (metaData) => fetcher<GetComicsQuery, GetComicsQueryVariables>(GetComicsDocument, {...variables, ...(metaData.pageParam ?? {})})(),
      ...restOptions
    }
  })()
    )};

useInfiniteGetComicsQuery.getKey = (variables?: GetComicsQueryVariables) => variables === undefined ? ['GetComics.infinite'] : ['GetComics.infinite', variables];


useGetComicsQuery.fetcher = (variables?: GetComicsQueryVariables, options?: RequestInit['headers']) => fetcher<GetComicsQuery, GetComicsQueryVariables>(GetComicsDocument, variables, options);

export const GetComicDocument = new TypedDocumentString(`
    query GetComic($id: Int!) {
  comic(id: $id) {
    id
    name
    description
    author
    source
    sourceIdentifier
    oldest
    newest
    avatarUrl
    lastStrip {
      imageUrl
      date
      width
      height
    }
    firstStrip {
      imageUrl
      date
    }
  }
}
    `);

export const useGetComicQuery = <
      TData = GetComicQuery,
      TError = unknown
    >(
      variables: GetComicQueryVariables,
      options?: Omit<UseQueryOptions<GetComicQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<GetComicQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<GetComicQuery, TError, TData>(
      {
    queryKey: ['GetComic', variables],
    queryFn: fetcher<GetComicQuery, GetComicQueryVariables>(GetComicDocument, variables),
    ...options
  }
    )};

useGetComicQuery.getKey = (variables: GetComicQueryVariables) => ['GetComic', variables];

export const useInfiniteGetComicQuery = <
      TData = InfiniteData<GetComicQuery>,
      TError = unknown
    >(
      variables: GetComicQueryVariables,
      options: Omit<UseInfiniteQueryOptions<GetComicQuery, TError, TData>, 'queryKey'> & { queryKey?: UseInfiniteQueryOptions<GetComicQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useInfiniteQuery<GetComicQuery, TError, TData>(
      (() => {
    const { queryKey: optionsQueryKey, ...restOptions } = options;
    return {
      queryKey: optionsQueryKey ?? ['GetComic.infinite', variables],
      queryFn: (metaData) => fetcher<GetComicQuery, GetComicQueryVariables>(GetComicDocument, {...variables, ...(metaData.pageParam ?? {})})(),
      ...restOptions
    }
  })()
    )};

useInfiniteGetComicQuery.getKey = (variables: GetComicQueryVariables) => ['GetComic.infinite', variables];


useGetComicQuery.fetcher = (variables: GetComicQueryVariables, options?: RequestInit['headers']) => fetcher<GetComicQuery, GetComicQueryVariables>(GetComicDocument, variables, options);

export const SearchComicsDocument = new TypedDocumentString(`
    query SearchComics($query: String!) {
  search(query: $query) {
    comics {
      id
      name
      description
      oldest
      newest
      avatarUrl
      lastStrip {
        imageUrl
        date
      }
    }
  }
}
    `);

export const useSearchComicsQuery = <
      TData = SearchComicsQuery,
      TError = unknown
    >(
      variables: SearchComicsQueryVariables,
      options?: Omit<UseQueryOptions<SearchComicsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<SearchComicsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<SearchComicsQuery, TError, TData>(
      {
    queryKey: ['SearchComics', variables],
    queryFn: fetcher<SearchComicsQuery, SearchComicsQueryVariables>(SearchComicsDocument, variables),
    ...options
  }
    )};

useSearchComicsQuery.getKey = (variables: SearchComicsQueryVariables) => ['SearchComics', variables];

export const useInfiniteSearchComicsQuery = <
      TData = InfiniteData<SearchComicsQuery>,
      TError = unknown
    >(
      variables: SearchComicsQueryVariables,
      options: Omit<UseInfiniteQueryOptions<SearchComicsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseInfiniteQueryOptions<SearchComicsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useInfiniteQuery<SearchComicsQuery, TError, TData>(
      (() => {
    const { queryKey: optionsQueryKey, ...restOptions } = options;
    return {
      queryKey: optionsQueryKey ?? ['SearchComics.infinite', variables],
      queryFn: (metaData) => fetcher<SearchComicsQuery, SearchComicsQueryVariables>(SearchComicsDocument, {...variables, ...(metaData.pageParam ?? {})})(),
      ...restOptions
    }
  })()
    )};

useInfiniteSearchComicsQuery.getKey = (variables: SearchComicsQueryVariables) => ['SearchComics.infinite', variables];


useSearchComicsQuery.fetcher = (variables: SearchComicsQueryVariables, options?: RequestInit['headers']) => fetcher<SearchComicsQuery, SearchComicsQueryVariables>(SearchComicsDocument, variables, options);

export const AddFavoriteDocument = new TypedDocumentString(`
    mutation AddFavorite($comicId: Int!) {
  addFavorite(comicId: $comicId) {
    preference {
      favoriteComics
    }
    errors {
      message
      field
      code
    }
  }
}
    `);

export const useAddFavoriteMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<AddFavoriteMutation, TError, AddFavoriteMutationVariables, TContext>) => {
    
    return useMutation<AddFavoriteMutation, TError, AddFavoriteMutationVariables, TContext>(
      {
    mutationKey: ['AddFavorite'],
    mutationFn: (variables?: AddFavoriteMutationVariables) => fetcher<AddFavoriteMutation, AddFavoriteMutationVariables>(AddFavoriteDocument, variables)(),
    ...options
  }
    )};


useAddFavoriteMutation.fetcher = (variables: AddFavoriteMutationVariables, options?: RequestInit['headers']) => fetcher<AddFavoriteMutation, AddFavoriteMutationVariables>(AddFavoriteDocument, variables, options);

export const RemoveFavoriteDocument = new TypedDocumentString(`
    mutation RemoveFavorite($comicId: Int!) {
  removeFavorite(comicId: $comicId) {
    preference {
      favoriteComics
    }
    errors {
      message
      field
      code
    }
  }
}
    `);

export const useRemoveFavoriteMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<RemoveFavoriteMutation, TError, RemoveFavoriteMutationVariables, TContext>) => {
    
    return useMutation<RemoveFavoriteMutation, TError, RemoveFavoriteMutationVariables, TContext>(
      {
    mutationKey: ['RemoveFavorite'],
    mutationFn: (variables?: RemoveFavoriteMutationVariables) => fetcher<RemoveFavoriteMutation, RemoveFavoriteMutationVariables>(RemoveFavoriteDocument, variables)(),
    ...options
  }
    )};


useRemoveFavoriteMutation.fetcher = (variables: RemoveFavoriteMutationVariables, options?: RequestInit['headers']) => fetcher<RemoveFavoriteMutation, RemoveFavoriteMutationVariables>(RemoveFavoriteDocument, variables, options);

export const UpdateLastReadDocument = new TypedDocumentString(`
    mutation UpdateLastRead($comicId: Int!, $date: Date!) {
  updateLastRead(comicId: $comicId, date: $date) {
    preference {
      lastReadDates {
        comicId
        date
      }
    }
    errors {
      message
      field
      code
    }
  }
}
    `);

export const useUpdateLastReadMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateLastReadMutation, TError, UpdateLastReadMutationVariables, TContext>) => {
    
    return useMutation<UpdateLastReadMutation, TError, UpdateLastReadMutationVariables, TContext>(
      {
    mutationKey: ['UpdateLastRead'],
    mutationFn: (variables?: UpdateLastReadMutationVariables) => fetcher<UpdateLastReadMutation, UpdateLastReadMutationVariables>(UpdateLastReadDocument, variables)(),
    ...options
  }
    )};


useUpdateLastReadMutation.fetcher = (variables: UpdateLastReadMutationVariables, options?: RequestInit['headers']) => fetcher<UpdateLastReadMutation, UpdateLastReadMutationVariables>(UpdateLastReadDocument, variables, options);

export const UpdateDisplaySettingsDocument = new TypedDocumentString(`
    mutation UpdateDisplaySettings($settings: JSON!) {
  updateDisplaySettings(settings: $settings) {
    preference {
      displaySettings
    }
    errors {
      message
      field
      code
    }
  }
}
    `);

export const useUpdateDisplaySettingsMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateDisplaySettingsMutation, TError, UpdateDisplaySettingsMutationVariables, TContext>) => {
    
    return useMutation<UpdateDisplaySettingsMutation, TError, UpdateDisplaySettingsMutationVariables, TContext>(
      {
    mutationKey: ['UpdateDisplaySettings'],
    mutationFn: (variables?: UpdateDisplaySettingsMutationVariables) => fetcher<UpdateDisplaySettingsMutation, UpdateDisplaySettingsMutationVariables>(UpdateDisplaySettingsDocument, variables)(),
    ...options
  }
    )};


useUpdateDisplaySettingsMutation.fetcher = (variables: UpdateDisplaySettingsMutationVariables, options?: RequestInit['headers']) => fetcher<UpdateDisplaySettingsMutation, UpdateDisplaySettingsMutationVariables>(UpdateDisplaySettingsDocument, variables, options);

export const GetStripWindowDocument = new TypedDocumentString(`
    query GetStripWindow($comicId: Int!, $center: Date!, $before: Int!, $after: Int!) {
  comic(id: $comicId) {
    id
    name
    oldest
    newest
    avatarUrl
    stripWindow(center: $center, before: $before, after: $after) {
      date
      available
      imageUrl
      width
      height
    }
  }
}
    `);

export const useGetStripWindowQuery = <
      TData = GetStripWindowQuery,
      TError = unknown
    >(
      variables: GetStripWindowQueryVariables,
      options?: Omit<UseQueryOptions<GetStripWindowQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<GetStripWindowQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<GetStripWindowQuery, TError, TData>(
      {
    queryKey: ['GetStripWindow', variables],
    queryFn: fetcher<GetStripWindowQuery, GetStripWindowQueryVariables>(GetStripWindowDocument, variables),
    ...options
  }
    )};

useGetStripWindowQuery.getKey = (variables: GetStripWindowQueryVariables) => ['GetStripWindow', variables];

export const useInfiniteGetStripWindowQuery = <
      TData = InfiniteData<GetStripWindowQuery>,
      TError = unknown
    >(
      variables: GetStripWindowQueryVariables,
      options: Omit<UseInfiniteQueryOptions<GetStripWindowQuery, TError, TData>, 'queryKey'> & { queryKey?: UseInfiniteQueryOptions<GetStripWindowQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useInfiniteQuery<GetStripWindowQuery, TError, TData>(
      (() => {
    const { queryKey: optionsQueryKey, ...restOptions } = options;
    return {
      queryKey: optionsQueryKey ?? ['GetStripWindow.infinite', variables],
      queryFn: (metaData) => fetcher<GetStripWindowQuery, GetStripWindowQueryVariables>(GetStripWindowDocument, {...variables, ...(metaData.pageParam ?? {})})(),
      ...restOptions
    }
  })()
    )};

useInfiniteGetStripWindowQuery.getKey = (variables: GetStripWindowQueryVariables) => ['GetStripWindow.infinite', variables];


useGetStripWindowQuery.fetcher = (variables: GetStripWindowQueryVariables, options?: RequestInit['headers']) => fetcher<GetStripWindowQuery, GetStripWindowQueryVariables>(GetStripWindowDocument, variables, options);

export const GetRandomStripDocument = new TypedDocumentString(`
    query GetRandomStrip($comicId: Int) {
  randomStrip(comicId: $comicId) {
    date
    available
    imageUrl
    width
    height
  }
}
    `);

export const useGetRandomStripQuery = <
      TData = GetRandomStripQuery,
      TError = unknown
    >(
      variables?: GetRandomStripQueryVariables,
      options?: Omit<UseQueryOptions<GetRandomStripQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<GetRandomStripQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<GetRandomStripQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['GetRandomStrip'] : ['GetRandomStrip', variables],
    queryFn: fetcher<GetRandomStripQuery, GetRandomStripQueryVariables>(GetRandomStripDocument, variables),
    ...options
  }
    )};

useGetRandomStripQuery.getKey = (variables?: GetRandomStripQueryVariables) => variables === undefined ? ['GetRandomStrip'] : ['GetRandomStrip', variables];

export const useInfiniteGetRandomStripQuery = <
      TData = InfiniteData<GetRandomStripQuery>,
      TError = unknown
    >(
      variables: GetRandomStripQueryVariables,
      options: Omit<UseInfiniteQueryOptions<GetRandomStripQuery, TError, TData>, 'queryKey'> & { queryKey?: UseInfiniteQueryOptions<GetRandomStripQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useInfiniteQuery<GetRandomStripQuery, TError, TData>(
      (() => {
    const { queryKey: optionsQueryKey, ...restOptions } = options;
    return {
      queryKey: optionsQueryKey ?? variables === undefined ? ['GetRandomStrip.infinite'] : ['GetRandomStrip.infinite', variables],
      queryFn: (metaData) => fetcher<GetRandomStripQuery, GetRandomStripQueryVariables>(GetRandomStripDocument, {...variables, ...(metaData.pageParam ?? {})})(),
      ...restOptions
    }
  })()
    )};

useInfiniteGetRandomStripQuery.getKey = (variables?: GetRandomStripQueryVariables) => variables === undefined ? ['GetRandomStrip.infinite'] : ['GetRandomStrip.infinite', variables];


useGetRandomStripQuery.fetcher = (variables?: GetRandomStripQueryVariables, options?: RequestInit['headers']) => fetcher<GetRandomStripQuery, GetRandomStripQueryVariables>(GetRandomStripDocument, variables, options);

export const GetComicsForDateDocument = new TypedDocumentString(`
    query GetComicsForDate($first: Int, $date: Date!) {
  comics(first: $first) {
    edges {
      node {
        id
        name
        avatarUrl
        oldest
        newest
        strip(date: $date) {
          date
          available
          imageUrl
          width
          height
          transcript
        }
      }
    }
    totalCount
  }
}
    `);

export const useGetComicsForDateQuery = <
      TData = GetComicsForDateQuery,
      TError = unknown
    >(
      variables: GetComicsForDateQueryVariables,
      options?: Omit<UseQueryOptions<GetComicsForDateQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<GetComicsForDateQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<GetComicsForDateQuery, TError, TData>(
      {
    queryKey: ['GetComicsForDate', variables],
    queryFn: fetcher<GetComicsForDateQuery, GetComicsForDateQueryVariables>(GetComicsForDateDocument, variables),
    ...options
  }
    )};

useGetComicsForDateQuery.getKey = (variables: GetComicsForDateQueryVariables) => ['GetComicsForDate', variables];

export const useInfiniteGetComicsForDateQuery = <
      TData = InfiniteData<GetComicsForDateQuery>,
      TError = unknown
    >(
      variables: GetComicsForDateQueryVariables,
      options: Omit<UseInfiniteQueryOptions<GetComicsForDateQuery, TError, TData>, 'queryKey'> & { queryKey?: UseInfiniteQueryOptions<GetComicsForDateQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useInfiniteQuery<GetComicsForDateQuery, TError, TData>(
      (() => {
    const { queryKey: optionsQueryKey, ...restOptions } = options;
    return {
      queryKey: optionsQueryKey ?? ['GetComicsForDate.infinite', variables],
      queryFn: (metaData) => fetcher<GetComicsForDateQuery, GetComicsForDateQueryVariables>(GetComicsForDateDocument, {...variables, ...(metaData.pageParam ?? {})})(),
      ...restOptions
    }
  })()
    )};

useInfiniteGetComicsForDateQuery.getKey = (variables: GetComicsForDateQueryVariables) => ['GetComicsForDate.infinite', variables];


useGetComicsForDateQuery.fetcher = (variables: GetComicsForDateQueryVariables, options?: RequestInit['headers']) => fetcher<GetComicsForDateQuery, GetComicsForDateQueryVariables>(GetComicsForDateDocument, variables, options);

export const GetCombinedMetricsDocument = new TypedDocumentString(`
    query GetCombinedMetrics {
  combinedMetrics {
    storage {
      totalBytes
      comicCount
      comics {
        comicId
        comicName
        totalBytes
        imageCount
        yearlyBreakdown {
          year
          bytes
          imageCount
        }
      }
      lastUpdated
    }
    access {
      totalAccesses
      comics {
        comicName
        accessCount
        averageAccessTimeMs
        lastAccessed
      }
      lastUpdated
    }
    lastUpdated
  }
}
    `);

export const useGetCombinedMetricsQuery = <
      TData = GetCombinedMetricsQuery,
      TError = unknown
    >(
      variables?: GetCombinedMetricsQueryVariables,
      options?: Omit<UseQueryOptions<GetCombinedMetricsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<GetCombinedMetricsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<GetCombinedMetricsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['GetCombinedMetrics'] : ['GetCombinedMetrics', variables],
    queryFn: fetcher<GetCombinedMetricsQuery, GetCombinedMetricsQueryVariables>(GetCombinedMetricsDocument, variables),
    ...options
  }
    )};

useGetCombinedMetricsQuery.getKey = (variables?: GetCombinedMetricsQueryVariables) => variables === undefined ? ['GetCombinedMetrics'] : ['GetCombinedMetrics', variables];

export const useInfiniteGetCombinedMetricsQuery = <
      TData = InfiniteData<GetCombinedMetricsQuery>,
      TError = unknown
    >(
      variables: GetCombinedMetricsQueryVariables,
      options: Omit<UseInfiniteQueryOptions<GetCombinedMetricsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseInfiniteQueryOptions<GetCombinedMetricsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useInfiniteQuery<GetCombinedMetricsQuery, TError, TData>(
      (() => {
    const { queryKey: optionsQueryKey, ...restOptions } = options;
    return {
      queryKey: optionsQueryKey ?? variables === undefined ? ['GetCombinedMetrics.infinite'] : ['GetCombinedMetrics.infinite', variables],
      queryFn: (metaData) => fetcher<GetCombinedMetricsQuery, GetCombinedMetricsQueryVariables>(GetCombinedMetricsDocument, {...variables, ...(metaData.pageParam ?? {})})(),
      ...restOptions
    }
  })()
    )};

useInfiniteGetCombinedMetricsQuery.getKey = (variables?: GetCombinedMetricsQueryVariables) => variables === undefined ? ['GetCombinedMetrics.infinite'] : ['GetCombinedMetrics.infinite', variables];


useGetCombinedMetricsQuery.fetcher = (variables?: GetCombinedMetricsQueryVariables, options?: RequestInit['headers']) => fetcher<GetCombinedMetricsQuery, GetCombinedMetricsQueryVariables>(GetCombinedMetricsDocument, variables, options);

export const GetRetrievalSummaryDocument = new TypedDocumentString(`
    query GetRetrievalSummary($fromDate: Date, $toDate: Date) {
  retrievalSummary(fromDate: $fromDate, toDate: $toDate) {
    totalAttempts
    successCount
    failureCount
    skippedCount
    successRate
    averageDurationMs
    byStatus {
      status
      count
    }
    byComic {
      comicName
      totalAttempts
      successCount
      failureCount
    }
  }
}
    `);

export const useGetRetrievalSummaryQuery = <
      TData = GetRetrievalSummaryQuery,
      TError = unknown
    >(
      variables?: GetRetrievalSummaryQueryVariables,
      options?: Omit<UseQueryOptions<GetRetrievalSummaryQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<GetRetrievalSummaryQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<GetRetrievalSummaryQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['GetRetrievalSummary'] : ['GetRetrievalSummary', variables],
    queryFn: fetcher<GetRetrievalSummaryQuery, GetRetrievalSummaryQueryVariables>(GetRetrievalSummaryDocument, variables),
    ...options
  }
    )};

useGetRetrievalSummaryQuery.getKey = (variables?: GetRetrievalSummaryQueryVariables) => variables === undefined ? ['GetRetrievalSummary'] : ['GetRetrievalSummary', variables];

export const useInfiniteGetRetrievalSummaryQuery = <
      TData = InfiniteData<GetRetrievalSummaryQuery>,
      TError = unknown
    >(
      variables: GetRetrievalSummaryQueryVariables,
      options: Omit<UseInfiniteQueryOptions<GetRetrievalSummaryQuery, TError, TData>, 'queryKey'> & { queryKey?: UseInfiniteQueryOptions<GetRetrievalSummaryQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useInfiniteQuery<GetRetrievalSummaryQuery, TError, TData>(
      (() => {
    const { queryKey: optionsQueryKey, ...restOptions } = options;
    return {
      queryKey: optionsQueryKey ?? variables === undefined ? ['GetRetrievalSummary.infinite'] : ['GetRetrievalSummary.infinite', variables],
      queryFn: (metaData) => fetcher<GetRetrievalSummaryQuery, GetRetrievalSummaryQueryVariables>(GetRetrievalSummaryDocument, {...variables, ...(metaData.pageParam ?? {})})(),
      ...restOptions
    }
  })()
    )};

useInfiniteGetRetrievalSummaryQuery.getKey = (variables?: GetRetrievalSummaryQueryVariables) => variables === undefined ? ['GetRetrievalSummary.infinite'] : ['GetRetrievalSummary.infinite', variables];


useGetRetrievalSummaryQuery.fetcher = (variables?: GetRetrievalSummaryQueryVariables, options?: RequestInit['headers']) => fetcher<GetRetrievalSummaryQuery, GetRetrievalSummaryQueryVariables>(GetRetrievalSummaryDocument, variables, options);

export const GetRetrievalRecordsDocument = new TypedDocumentString(`
    query GetRetrievalRecords($comicName: String, $status: RetrievalStatusEnum, $fromDate: Date, $toDate: Date, $limit: Int) {
  retrievalRecords(
    comicName: $comicName
    status: $status
    fromDate: $fromDate
    toDate: $toDate
    limit: $limit
  ) {
    id
    comicName
    comicDate
    source
    status
    retrievalDurationMs
    imageSize
    httpStatusCode
    errorMessage
  }
}
    `);

export const useGetRetrievalRecordsQuery = <
      TData = GetRetrievalRecordsQuery,
      TError = unknown
    >(
      variables?: GetRetrievalRecordsQueryVariables,
      options?: Omit<UseQueryOptions<GetRetrievalRecordsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<GetRetrievalRecordsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<GetRetrievalRecordsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['GetRetrievalRecords'] : ['GetRetrievalRecords', variables],
    queryFn: fetcher<GetRetrievalRecordsQuery, GetRetrievalRecordsQueryVariables>(GetRetrievalRecordsDocument, variables),
    ...options
  }
    )};

useGetRetrievalRecordsQuery.getKey = (variables?: GetRetrievalRecordsQueryVariables) => variables === undefined ? ['GetRetrievalRecords'] : ['GetRetrievalRecords', variables];

export const useInfiniteGetRetrievalRecordsQuery = <
      TData = InfiniteData<GetRetrievalRecordsQuery>,
      TError = unknown
    >(
      variables: GetRetrievalRecordsQueryVariables,
      options: Omit<UseInfiniteQueryOptions<GetRetrievalRecordsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseInfiniteQueryOptions<GetRetrievalRecordsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useInfiniteQuery<GetRetrievalRecordsQuery, TError, TData>(
      (() => {
    const { queryKey: optionsQueryKey, ...restOptions } = options;
    return {
      queryKey: optionsQueryKey ?? variables === undefined ? ['GetRetrievalRecords.infinite'] : ['GetRetrievalRecords.infinite', variables],
      queryFn: (metaData) => fetcher<GetRetrievalRecordsQuery, GetRetrievalRecordsQueryVariables>(GetRetrievalRecordsDocument, {...variables, ...(metaData.pageParam ?? {})})(),
      ...restOptions
    }
  })()
    )};

useInfiniteGetRetrievalRecordsQuery.getKey = (variables?: GetRetrievalRecordsQueryVariables) => variables === undefined ? ['GetRetrievalRecords.infinite'] : ['GetRetrievalRecords.infinite', variables];


useGetRetrievalRecordsQuery.fetcher = (variables?: GetRetrievalRecordsQueryVariables, options?: RequestInit['headers']) => fetcher<GetRetrievalRecordsQuery, GetRetrievalRecordsQueryVariables>(GetRetrievalRecordsDocument, variables, options);
