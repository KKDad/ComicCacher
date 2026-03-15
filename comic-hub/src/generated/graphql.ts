import { useMutation, useQuery, useInfiniteQuery, UseMutationOptions, UseQueryOptions, UseInfiniteQueryOptions, InfiniteData } from '@tanstack/react-query';
import { fetcher } from '../lib/graphql-client';
export type Maybe<T> = T | null;
export type InputMaybe<T> = Maybe<T>;
export type Exact<T extends { [key: string]: unknown }> = { [K in keyof T]: T[K] };
export type MakeOptional<T, K extends keyof T> = Omit<T, K> & { [SubKey in K]?: Maybe<T[SubKey]> };
export type MakeMaybe<T, K extends keyof T> = Omit<T, K> & { [SubKey in K]: Maybe<T[SubKey]> };
export type MakeEmpty<T extends { [key: string]: unknown }, K extends keyof T> = { [_ in K]?: never };
export type Incremental<T> = T | { [P in keyof T]?: P extends ' $fragmentName' | '__typename' ? T[P] : never };
/** All built-in and custom scalars, mapped to their actual values */
export type Scalars = {
  ID: { input: string; output: string; }
  String: { input: string; output: string; }
  Boolean: { input: boolean; output: boolean; }
  Int: { input: number; output: number; }
  Float: { input: number; output: number; }
  Date: { input: any; output: any; }
  DateTime: { input: any; output: any; }
  JSON: { input: any; output: any; }
};

/** Access metrics for all comics. */
export type AccessMetrics = {
  __typename?: 'AccessMetrics';
  /** Per-comic access breakdown. */
  comics?: Maybe<Array<ComicAccessMetric>>;
  /** Last time metrics were updated. */
  lastUpdated?: Maybe<Scalars['DateTime']['output']>;
  /** Total number of access events tracked. */
  totalAccesses?: Maybe<Scalars['Int']['output']>;
};

/**
 * Authentication payload returned after successful login or registration.
 * Contains JWT tokens and user information.
 */
export type AuthPayload = {
  __typename?: 'AuthPayload';
  /** Display name of the authenticated user. */
  displayName?: Maybe<Scalars['String']['output']>;
  /** Refresh token for obtaining new access tokens. */
  refreshToken: Scalars['String']['output'];
  /**
   * JWT access token for authenticating API requests.
   * Include in Authorization header as: Bearer <token>
   */
  token: Scalars['String']['output'];
  /** Username of the authenticated user. */
  username: Scalars['String']['output'];
};

/** Batch job execution information. */
export type BatchJob = {
  __typename?: 'BatchJob';
  /** Duration of the job in milliseconds. */
  durationMs?: Maybe<Scalars['Float']['output']>;
  /** When the job ended (null if still running). */
  endTime?: Maybe<Scalars['DateTime']['output']>;
  /** Unique execution ID. */
  executionId: Scalars['Int']['output'];
  /** Exit status code. */
  exitCode?: Maybe<Scalars['String']['output']>;
  /** Exit description. */
  exitDescription?: Maybe<Scalars['String']['output']>;
  /** Job name (e.g., "comicRetrievalJob"). */
  jobName: Scalars['String']['output'];
  /** Job parameters (key-value pairs). */
  parameters?: Maybe<Scalars['JSON']['output']>;
  /** When the job started. */
  startTime: Scalars['DateTime']['output'];
  /** Job execution status. */
  status: BatchStatusEnum;
  /** Step execution details. */
  steps?: Maybe<Array<BatchStep>>;
};

/** Summary statistics for batch job executions. */
export type BatchJobSummary = {
  __typename?: 'BatchJobSummary';
  /** Average job duration in milliseconds. */
  averageDurationMs?: Maybe<Scalars['Float']['output']>;
  /** Daily breakdown of job executions. */
  dailyBreakdown?: Maybe<Array<DailyJobStats>>;
  /** Number of days included in the summary. */
  daysIncluded: Scalars['Int']['output'];
  /** Number of failed executions. */
  failureCount: Scalars['Int']['output'];
  /** Number of currently running jobs. */
  runningCount: Scalars['Int']['output'];
  /** Number of successful executions. */
  successCount: Scalars['Int']['output'];
  /** Total number of job executions. */
  totalExecutions: Scalars['Int']['output'];
  /** Total items processed (read) across all jobs. */
  totalItemsProcessed?: Maybe<Scalars['Int']['output']>;
};

/** Possible batch job status values. */
export enum BatchStatusEnum {
  Abandoned = 'ABANDONED',
  Completed = 'COMPLETED',
  Failed = 'FAILED',
  Started = 'STARTED',
  Starting = 'STARTING',
  Stopped = 'STOPPED',
  Stopping = 'STOPPING',
  Unknown = 'UNKNOWN'
}

/** Step execution within a batch job. */
export type BatchStep = {
  __typename?: 'BatchStep';
  /** Number of commits. */
  commitCount: Scalars['Int']['output'];
  /** Step end time. */
  endTime?: Maybe<Scalars['DateTime']['output']>;
  /** Number of items filtered/skipped. */
  filterCount: Scalars['Int']['output'];
  /** Number of items read. */
  readCount: Scalars['Int']['output'];
  /** Number of rollbacks. */
  rollbackCount: Scalars['Int']['output'];
  /** Number of items that failed. */
  skipCount: Scalars['Int']['output'];
  /** Step start time. */
  startTime?: Maybe<Scalars['DateTime']['output']>;
  /** Step execution status. */
  status: BatchStatusEnum;
  /** Step name. */
  stepName: Scalars['String']['output'];
  /** Number of items written. */
  writeCount: Scalars['Int']['output'];
};

/** Application build information. */
export type BuildInfo = {
  __typename?: 'BuildInfo';
  /** Build timestamp. */
  buildTime?: Maybe<Scalars['String']['output']>;
  /** Git branch. */
  gitBranch?: Maybe<Scalars['String']['output']>;
  /** Git commit hash. */
  gitCommit?: Maybe<Scalars['String']['output']>;
  /** Application version. */
  version?: Maybe<Scalars['String']['output']>;
};

/** Cache status information. */
export type CacheStatus = {
  __typename?: 'CacheStatus';
  /** Directory where the cache is stored. */
  cacheLocation?: Maybe<Scalars['String']['output']>;
  /** Newest image in the cache. */
  newestImage?: Maybe<Scalars['String']['output']>;
  /** Oldest image in the cache. */
  oldestImage?: Maybe<Scalars['String']['output']>;
  /** Total number of comics cached. */
  totalComics: Scalars['Int']['output'];
  /** Total number of cached images. */
  totalImages: Scalars['Int']['output'];
  /** Total storage used in bytes. */
  totalStorageBytes?: Maybe<Scalars['Float']['output']>;
};

/** Combined storage and access metrics. */
export type CombinedMetrics = {
  __typename?: 'CombinedMetrics';
  /** Access metrics. */
  access?: Maybe<AccessMetrics>;
  /** Last time combined metrics were calculated. */
  lastUpdated?: Maybe<Scalars['DateTime']['output']>;
  /** Storage metrics. */
  storage?: Maybe<StorageMetrics>;
};

/** A comic series/strip registered in the system. */
export type Comic = {
  __typename?: 'Comic';
  /** Whether this comic is actively publishing new strips. */
  active?: Maybe<Scalars['Boolean']['output']>;
  /** Author/creator of the comic. */
  author?: Maybe<Scalars['String']['output']>;
  /** Whether an avatar image is available for this comic. */
  avatarAvailable?: Maybe<Scalars['Boolean']['output']>;
  /**
   * URL to the comic's avatar image.
   * Returns a path to the REST endpoint (e.g., /api/v1/comics/123/avatar).
   */
  avatarUrl?: Maybe<Scalars['String']['output']>;
  /** Description of the comic. */
  description?: Maybe<Scalars['String']['output']>;
  /** Whether this comic is enabled for display. */
  enabled?: Maybe<Scalars['Boolean']['output']>;
  /** Get the first (oldest) available strip. */
  firstStrip?: Maybe<ComicStrip>;
  /** Unique identifier for the comic. */
  id: Scalars['Int']['output'];
  /** Get the last (newest) available strip. */
  lastStrip?: Maybe<ComicStrip>;
  /** Display name of the comic (e.g., "Dilbert", "Calvin and Hobbes"). */
  name: Scalars['String']['output'];
  /** Date of the newest cached strip. */
  newest?: Maybe<Scalars['Date']['output']>;
  /** Date of the oldest cached strip. */
  oldest?: Maybe<Scalars['Date']['output']>;
  /** Days of the week when this comic publishes (null/empty means daily). */
  publicationDays?: Maybe<Array<DayOfWeek>>;
  /** Source provider for this comic (e.g., "gocomics", "comicskingdom"). */
  source?: Maybe<Scalars['String']['output']>;
  /** Identifier used by the source to reference this comic. */
  sourceIdentifier?: Maybe<Scalars['String']['output']>;
  /**
   * Get a strip for a specific date.
   * If date is null, returns the latest (newest) strip.
   */
  strip?: Maybe<ComicStrip>;
  /**
   * Get strips for multiple specific dates.
   * Useful for dashboard views showing several dates at once.
   */
  strips: Array<ComicStrip>;
};


/** A comic series/strip registered in the system. */
export type ComicStripArgs = {
  date?: InputMaybe<Scalars['Date']['input']>;
};


/** A comic series/strip registered in the system. */
export type ComicStripsArgs = {
  dates: Array<Scalars['Date']['input']>;
};

/** Access metrics for a single comic. */
export type ComicAccessMetric = {
  __typename?: 'ComicAccessMetric';
  /** Total number of accesses for this comic. */
  accessCount: Scalars['Int']['output'];
  /** Average access time in milliseconds. */
  averageAccessTimeMs?: Maybe<Scalars['Float']['output']>;
  /** Comic name. */
  comicName: Scalars['String']['output'];
  /** Last access timestamp. */
  lastAccessed?: Maybe<Scalars['DateTime']['output']>;
};

/** Paginated connection of comics. */
export type ComicConnection = {
  __typename?: 'ComicConnection';
  /** List of comic edges. */
  edges: Array<ComicEdge>;
  /** Pagination information. */
  pageInfo: PageInfo;
  /** Total number of comics matching the filter (for UI display). */
  totalCount: Scalars['Int']['output'];
};

/** Edge containing a comic node and its cursor. */
export type ComicEdge = {
  __typename?: 'ComicEdge';
  /** Cursor for this edge (use with 'after' argument for pagination). */
  cursor: Scalars['String']['output'];
  /** The comic. */
  node: Comic;
};

/** Retrieval summary for a specific comic. */
export type ComicRetrievalSummary = {
  __typename?: 'ComicRetrievalSummary';
  /** Comic name. */
  comicName: Scalars['String']['output'];
  /** Failed retrievals. */
  failureCount: Scalars['Int']['output'];
  /** Successful retrievals. */
  successCount: Scalars['Int']['output'];
  /** Total attempts for this comic. */
  totalAttempts: Scalars['Int']['output'];
};

/** Storage metrics for a single comic. */
export type ComicStorageMetric = {
  __typename?: 'ComicStorageMetric';
  /** Comic ID. */
  comicId?: Maybe<Scalars['Int']['output']>;
  /** Comic name. */
  comicName: Scalars['String']['output'];
  /** Number of cached images for this comic. */
  imageCount: Scalars['Int']['output'];
  /** Total storage used by this comic in bytes. */
  totalBytes: Scalars['Float']['output'];
  /** Yearly breakdown of storage. */
  yearlyBreakdown?: Maybe<Array<YearlyStorageMetric>>;
};

/**
 * A comic strip for a specific date.
 * Used by the strips query for batch date fetching.
 */
export type ComicStrip = {
  __typename?: 'ComicStrip';
  /** Whether a strip exists for this date. */
  available: Scalars['Boolean']['output'];
  /** The date of this strip. */
  date: Scalars['Date']['output'];
  /** URL to the strip image (null if not available). */
  imageUrl?: Maybe<Scalars['String']['output']>;
  /** The next strip after this date (null if at end). */
  next?: Maybe<ComicStrip>;
  /** The previous strip before this date (null if at beginning). */
  previous?: Maybe<ComicStrip>;
};

/** Health status of an individual component. */
export type ComponentHealthEntry = {
  __typename?: 'ComponentHealthEntry';
  /** Additional details about the component. */
  details?: Maybe<Scalars['String']['output']>;
  /** Component name. */
  name: Scalars['String']['output'];
  /** Component health status. */
  status: HealthStatusEnum;
};

/** Input for creating a new comic. */
export type CreateComicInput = {
  /** Whether this comic is actively publishing. */
  active?: InputMaybe<Scalars['Boolean']['input']>;
  /** Author/creator of the comic. */
  author?: InputMaybe<Scalars['String']['input']>;
  /** Description of the comic. */
  description?: InputMaybe<Scalars['String']['input']>;
  /** Whether this comic is enabled for display. */
  enabled?: InputMaybe<Scalars['Boolean']['input']>;
  /** Display name of the comic. */
  name: Scalars['String']['input'];
  /** Days of the week when this comic publishes. */
  publicationDays?: InputMaybe<Array<DayOfWeek>>;
  /** Source provider for this comic. */
  source?: InputMaybe<Scalars['String']['input']>;
  /** Identifier used by the source. */
  sourceIdentifier?: InputMaybe<Scalars['String']['input']>;
};

/** Daily job execution statistics. */
export type DailyJobStats = {
  __typename?: 'DailyJobStats';
  /** Date. */
  date: Scalars['Date']['output'];
  /** Number of job executions on this date. */
  executionCount: Scalars['Int']['output'];
  /** Number of failed executions. */
  failureCount: Scalars['Int']['output'];
  /** Number of successful executions. */
  successCount: Scalars['Int']['output'];
};

/** Days of the week. */
export enum DayOfWeek {
  Friday = 'FRIDAY',
  Monday = 'MONDAY',
  Saturday = 'SATURDAY',
  Sunday = 'SUNDAY',
  Thursday = 'THURSDAY',
  Tuesday = 'TUESDAY',
  Wednesday = 'WEDNESDAY'
}

/**
 * Standard error codes returned by the API.
 * These codes appear in error responses to help clients handle errors programmatically.
 */
export enum ErrorCode {
  /** Comic with the specified ID does not exist. */
  ComicNotFound = 'COMIC_NOT_FOUND',
  /** User does not have permission for this operation. */
  Forbidden = 'FORBIDDEN',
  /** Internal server error. */
  InternalError = 'INTERNAL_ERROR',
  /** Invalid credentials provided. */
  InvalidCredentials = 'INVALID_CREDENTIALS',
  /** Password does not meet requirements. */
  InvalidPassword = 'INVALID_PASSWORD',
  /** Token is invalid or malformed. */
  InvalidToken = 'INVALID_TOKEN',
  /** Requested resource was not found. */
  NotFound = 'NOT_FOUND',
  /** Rate limit exceeded. */
  RateLimited = 'RATE_LIMITED',
  /** Comic strip not available for the requested date. */
  StripNotFound = 'STRIP_NOT_FOUND',
  /** Token has expired. */
  TokenExpired = 'TOKEN_EXPIRED',
  /** Authentication required but not provided. */
  Unauthenticated = 'UNAUTHENTICATED',
  /** Username or email already exists. */
  UserAlreadyExists = 'USER_ALREADY_EXISTS',
  /** User account not found. */
  UserNotFound = 'USER_NOT_FOUND',
  /** Input validation failed. */
  ValidationError = 'VALIDATION_ERROR'
}

/** Overall system health status. */
export type HealthStatus = {
  __typename?: 'HealthStatus';
  /** Application build information. */
  buildInfo?: Maybe<BuildInfo>;
  /** Cache status information (only included when detailed=true). */
  cacheStatus?: Maybe<CacheStatus>;
  /** Individual component health statuses. */
  components?: Maybe<Array<ComponentHealthEntry>>;
  /** Current health status. */
  status: HealthStatusEnum;
  /** System resource metrics (only included when detailed=true). */
  systemResources?: Maybe<SystemResources>;
  /** Timestamp when the health check was performed. */
  timestamp?: Maybe<Scalars['DateTime']['output']>;
  /** Application uptime in milliseconds. */
  uptime?: Maybe<Scalars['Float']['output']>;
};

/** Possible health status values. */
export enum HealthStatusEnum {
  Degraded = 'DEGRADED',
  Down = 'DOWN',
  Up = 'UP'
}

/** Entry representing the last read date for a comic. */
export type LastReadEntry = {
  __typename?: 'LastReadEntry';
  /** Comic ID. */
  comicId: Scalars['Int']['output'];
  /** Last read date. */
  date: Scalars['Date']['output'];
};

/** Input for user login. */
export type LoginInput = {
  /** User's password. */
  password: Scalars['String']['input'];
  /** Username or email address. */
  username: Scalars['String']['input'];
};

/**
 *  -----------------------------------------------------------------------------
 *  Root Mutation Type
 *  -----------------------------------------------------------------------------
 */
export type Mutation = {
  __typename?: 'Mutation';
  /**
   * Add a comic to the user's favorites.
   * Requires authentication.
   */
  addFavorite: UserPreference;
  /**
   * Create a new comic entry.
   * Requires admin role.
   */
  createComic: Comic;
  /**
   * Delete the current user's account.
   * This action is irreversible.
   * Requires authentication.
   */
  deleteAccount: Scalars['Boolean']['output'];
  /**
   * Delete a comic and its cached strips.
   * Requires admin role.
   */
  deleteComic: Scalars['Boolean']['output'];
  /** Delete a specific retrieval record. */
  deleteRetrievalRecord: Scalars['Boolean']['output'];
  /**
   * Request a password reset email.
   * Returns true if the email was sent (always returns true to prevent email enumeration).
   */
  forgotPassword: Scalars['Boolean']['output'];
  /**
   * Authenticate with username and password.
   * Returns authentication payload with JWT token on success.
   */
  login: AuthPayload;
  /**
   * Purge retrieval records older than specified days.
   * Returns the number of records purged.
   */
  purgeRetrievalRecords: Scalars['Int']['output'];
  /** Force a refresh of all metrics (storage, access, combined). */
  refreshAllMetrics: Scalars['Boolean']['output'];
  /** Force a refresh of storage metrics. */
  refreshStorageMetrics: StorageMetrics;
  /** Refresh an expired JWT token using a refresh token. */
  refreshToken: AuthPayload;
  /**
   * Register a new user account.
   * Returns authentication payload with JWT token on success.
   */
  register: AuthPayload;
  /**
   * Remove a comic from the user's favorites.
   * Requires authentication.
   */
  removeFavorite: UserPreference;
  /** Reset password using a token from the password reset email. */
  resetPassword: AuthPayload;
  /** Trigger a backfill job to retrieve missing comics. */
  triggerBackfillJob: BatchJob;
  /** Trigger a batch job for comic retrieval. */
  triggerBatchJob: BatchJob;
  /**
   * Update an existing comic's details.
   * Requires admin role.
   */
  updateComic: Comic;
  /**
   * Update display settings (theme, layout, etc.).
   * Requires authentication.
   */
  updateDisplaySettings: UserPreference;
  /**
   * Update the last read date for a comic.
   * Requires authentication.
   */
  updateLastRead: UserPreference;
  /**
   * Update the current user's password.
   * Requires authentication.
   */
  updatePassword: Scalars['Boolean']['output'];
  /**
   * Update the current user's profile.
   * Requires authentication.
   */
  updateProfile: User;
};


/**
 *  -----------------------------------------------------------------------------
 *  Root Mutation Type
 *  -----------------------------------------------------------------------------
 */
export type MutationAddFavoriteArgs = {
  comicId: Scalars['Int']['input'];
};


/**
 *  -----------------------------------------------------------------------------
 *  Root Mutation Type
 *  -----------------------------------------------------------------------------
 */
export type MutationCreateComicArgs = {
  input: CreateComicInput;
};


/**
 *  -----------------------------------------------------------------------------
 *  Root Mutation Type
 *  -----------------------------------------------------------------------------
 */
export type MutationDeleteComicArgs = {
  id: Scalars['Int']['input'];
};


/**
 *  -----------------------------------------------------------------------------
 *  Root Mutation Type
 *  -----------------------------------------------------------------------------
 */
export type MutationDeleteRetrievalRecordArgs = {
  id: Scalars['String']['input'];
};


/**
 *  -----------------------------------------------------------------------------
 *  Root Mutation Type
 *  -----------------------------------------------------------------------------
 */
export type MutationForgotPasswordArgs = {
  email: Scalars['String']['input'];
};


/**
 *  -----------------------------------------------------------------------------
 *  Root Mutation Type
 *  -----------------------------------------------------------------------------
 */
export type MutationLoginArgs = {
  input: LoginInput;
};


/**
 *  -----------------------------------------------------------------------------
 *  Root Mutation Type
 *  -----------------------------------------------------------------------------
 */
export type MutationPurgeRetrievalRecordsArgs = {
  daysToKeep?: InputMaybe<Scalars['Int']['input']>;
};


/**
 *  -----------------------------------------------------------------------------
 *  Root Mutation Type
 *  -----------------------------------------------------------------------------
 */
export type MutationRefreshTokenArgs = {
  refreshToken: Scalars['String']['input'];
};


/**
 *  -----------------------------------------------------------------------------
 *  Root Mutation Type
 *  -----------------------------------------------------------------------------
 */
export type MutationRegisterArgs = {
  input: RegisterInput;
};


/**
 *  -----------------------------------------------------------------------------
 *  Root Mutation Type
 *  -----------------------------------------------------------------------------
 */
export type MutationRemoveFavoriteArgs = {
  comicId: Scalars['Int']['input'];
};


/**
 *  -----------------------------------------------------------------------------
 *  Root Mutation Type
 *  -----------------------------------------------------------------------------
 */
export type MutationResetPasswordArgs = {
  newPassword: Scalars['String']['input'];
  token: Scalars['String']['input'];
};


/**
 *  -----------------------------------------------------------------------------
 *  Root Mutation Type
 *  -----------------------------------------------------------------------------
 */
export type MutationTriggerBatchJobArgs = {
  targetDate?: InputMaybe<Scalars['Date']['input']>;
};


/**
 *  -----------------------------------------------------------------------------
 *  Root Mutation Type
 *  -----------------------------------------------------------------------------
 */
export type MutationUpdateComicArgs = {
  id: Scalars['Int']['input'];
  input: UpdateComicInput;
};


/**
 *  -----------------------------------------------------------------------------
 *  Root Mutation Type
 *  -----------------------------------------------------------------------------
 */
export type MutationUpdateDisplaySettingsArgs = {
  settings: Scalars['JSON']['input'];
};


/**
 *  -----------------------------------------------------------------------------
 *  Root Mutation Type
 *  -----------------------------------------------------------------------------
 */
export type MutationUpdateLastReadArgs = {
  comicId: Scalars['Int']['input'];
  date: Scalars['Date']['input'];
};


/**
 *  -----------------------------------------------------------------------------
 *  Root Mutation Type
 *  -----------------------------------------------------------------------------
 */
export type MutationUpdatePasswordArgs = {
  newPassword: Scalars['String']['input'];
};


/**
 *  -----------------------------------------------------------------------------
 *  Root Mutation Type
 *  -----------------------------------------------------------------------------
 */
export type MutationUpdateProfileArgs = {
  input: UpdateProfileInput;
};

/** Pagination metadata. */
export type PageInfo = {
  __typename?: 'PageInfo';
  /** Cursor of the last edge. */
  endCursor?: Maybe<Scalars['String']['output']>;
  /** Whether there are more results after the last edge. */
  hasNextPage: Scalars['Boolean']['output'];
  /** Whether there are results before the first edge. */
  hasPreviousPage: Scalars['Boolean']['output'];
  /** Cursor of the first edge. */
  startCursor?: Maybe<Scalars['String']['output']>;
};

/**
 *  -----------------------------------------------------------------------------
 *  Root Query Type
 *  -----------------------------------------------------------------------------
 */
export type Query = {
  __typename?: 'Query';
  /** Get access metrics for all comics. */
  accessMetrics?: Maybe<AccessMetrics>;
  /** Get a specific batch job execution by ID. */
  batchJob?: Maybe<BatchJob>;
  /** Get summary statistics for batch jobs. */
  batchJobSummary: BatchJobSummary;
  /** Get batch jobs within a date range. */
  batchJobsByDateRange: Array<BatchJob>;
  /** Get combined storage and access metrics. */
  combinedMetrics?: Maybe<CombinedMetrics>;
  /** Get a specific comic by its ID. */
  comic?: Maybe<Comic>;
  /**
   * Get comics with optional search, filtering, and pagination.
   * Supports cursor-based pagination for efficient large archive handling.
   */
  comics: ComicConnection;
  /** Get a list of all known error codes. */
  errorCodes: Array<ErrorCode>;
  /** Get the health status of the application. */
  health: HealthStatus;
  /**
   * Get the current authenticated user's profile.
   * Requires authentication.
   */
  me?: Maybe<User>;
  /**
   * Get the current authenticated user's preferences.
   * Requires authentication.
   */
  preferences?: Maybe<UserPreference>;
  /** Get recent batch job executions. */
  recentBatchJobs: Array<BatchJob>;
  /** Get a specific retrieval record by ID. */
  retrievalRecord?: Maybe<RetrievalRecord>;
  /**
   * Get retrieval records with optional filtering.
   * Returns records from the last 7 days.
   */
  retrievalRecords: Array<RetrievalRecord>;
  /** Get retrieval records for a specific comic. */
  retrievalRecordsForComic: Array<RetrievalRecord>;
  /** Get summary statistics of retrieval operations. */
  retrievalSummary: RetrievalSummary;
  /**
   * Full-text search across comics.
   * Searches comic names, authors, and descriptions.
   */
  search: SearchResults;
  /** Get storage metrics for the comic cache. */
  storageMetrics?: Maybe<StorageMetrics>;
  /**
   * Get a comic strip directly by comic ID and date.
   * More efficient than querying comic.strip when you only need the strip.
   */
  strip?: Maybe<ComicStrip>;
  /**
   * Validate the current JWT token from Authorization header.
   * Returns true if the token is valid, false otherwise.
   */
  validateToken: Scalars['Boolean']['output'];
};


/**
 *  -----------------------------------------------------------------------------
 *  Root Query Type
 *  -----------------------------------------------------------------------------
 */
export type QueryBatchJobArgs = {
  executionId: Scalars['Int']['input'];
};


/**
 *  -----------------------------------------------------------------------------
 *  Root Query Type
 *  -----------------------------------------------------------------------------
 */
export type QueryBatchJobSummaryArgs = {
  days?: InputMaybe<Scalars['Int']['input']>;
};


/**
 *  -----------------------------------------------------------------------------
 *  Root Query Type
 *  -----------------------------------------------------------------------------
 */
export type QueryBatchJobsByDateRangeArgs = {
  endDate: Scalars['Date']['input'];
  startDate: Scalars['Date']['input'];
};


/**
 *  -----------------------------------------------------------------------------
 *  Root Query Type
 *  -----------------------------------------------------------------------------
 */
export type QueryComicArgs = {
  id: Scalars['Int']['input'];
};


/**
 *  -----------------------------------------------------------------------------
 *  Root Query Type
 *  -----------------------------------------------------------------------------
 */
export type QueryComicsArgs = {
  active?: InputMaybe<Scalars['Boolean']['input']>;
  after?: InputMaybe<Scalars['String']['input']>;
  enabled?: InputMaybe<Scalars['Boolean']['input']>;
  first?: InputMaybe<Scalars['Int']['input']>;
  search?: InputMaybe<Scalars['String']['input']>;
};


/**
 *  -----------------------------------------------------------------------------
 *  Root Query Type
 *  -----------------------------------------------------------------------------
 */
export type QueryHealthArgs = {
  detailed?: InputMaybe<Scalars['Boolean']['input']>;
};


/**
 *  -----------------------------------------------------------------------------
 *  Root Query Type
 *  -----------------------------------------------------------------------------
 */
export type QueryRecentBatchJobsArgs = {
  count?: InputMaybe<Scalars['Int']['input']>;
};


/**
 *  -----------------------------------------------------------------------------
 *  Root Query Type
 *  -----------------------------------------------------------------------------
 */
export type QueryRetrievalRecordArgs = {
  id: Scalars['String']['input'];
};


/**
 *  -----------------------------------------------------------------------------
 *  Root Query Type
 *  -----------------------------------------------------------------------------
 */
export type QueryRetrievalRecordsArgs = {
  comicName?: InputMaybe<Scalars['String']['input']>;
  fromDate?: InputMaybe<Scalars['Date']['input']>;
  limit?: InputMaybe<Scalars['Int']['input']>;
  status?: InputMaybe<RetrievalStatusEnum>;
  toDate?: InputMaybe<Scalars['Date']['input']>;
};


/**
 *  -----------------------------------------------------------------------------
 *  Root Query Type
 *  -----------------------------------------------------------------------------
 */
export type QueryRetrievalRecordsForComicArgs = {
  comicName: Scalars['String']['input'];
  limit?: InputMaybe<Scalars['Int']['input']>;
};


/**
 *  -----------------------------------------------------------------------------
 *  Root Query Type
 *  -----------------------------------------------------------------------------
 */
export type QueryRetrievalSummaryArgs = {
  fromDate?: InputMaybe<Scalars['Date']['input']>;
  toDate?: InputMaybe<Scalars['Date']['input']>;
};


/**
 *  -----------------------------------------------------------------------------
 *  Root Query Type
 *  -----------------------------------------------------------------------------
 */
export type QuerySearchArgs = {
  limit?: InputMaybe<Scalars['Int']['input']>;
  query: Scalars['String']['input'];
};


/**
 *  -----------------------------------------------------------------------------
 *  Root Query Type
 *  -----------------------------------------------------------------------------
 */
export type QueryStripArgs = {
  comicId: Scalars['Int']['input'];
  date: Scalars['Date']['input'];
};

/** Input for new user registration. */
export type RegisterInput = {
  /** User's display name. */
  displayName?: InputMaybe<Scalars['String']['input']>;
  /** User's email address. */
  email: Scalars['String']['input'];
  /** User's password. */
  password: Scalars['String']['input'];
  /** Desired username (must be unique). */
  username: Scalars['String']['input'];
};

/** Record of a comic retrieval attempt. */
export type RetrievalRecord = {
  __typename?: 'RetrievalRecord';
  /** Date for which the comic was retrieved. */
  comicDate: Scalars['Date']['output'];
  /** Name of the comic. */
  comicName: Scalars['String']['output'];
  /** Error message if retrieval failed. */
  errorMessage?: Maybe<Scalars['String']['output']>;
  /** HTTP status code from the comic source. */
  httpStatusCode?: Maybe<Scalars['Int']['output']>;
  /** Unique identifier (format: "ComicName_YYYY-MM-DD"). */
  id: Scalars['String']['output'];
  /** Size of the retrieved image in bytes (if successful). */
  imageSize?: Maybe<Scalars['Float']['output']>;
  /** Duration of the retrieval operation in milliseconds. */
  retrievalDurationMs?: Maybe<Scalars['Float']['output']>;
  /** Source provider (e.g., "gocomics", "comicskingdom"). */
  source?: Maybe<Scalars['String']['output']>;
  /** Retrieval status. */
  status: RetrievalStatusEnum;
};

/** Possible retrieval status values. */
export enum RetrievalStatusEnum {
  Error = 'ERROR',
  Failure = 'FAILURE',
  NotFound = 'NOT_FOUND',
  RateLimited = 'RATE_LIMITED',
  Skipped = 'SKIPPED',
  Success = 'SUCCESS'
}

/** Summary statistics for retrieval operations. */
export type RetrievalSummary = {
  __typename?: 'RetrievalSummary';
  /** Average retrieval duration in milliseconds. */
  averageDurationMs?: Maybe<Scalars['Float']['output']>;
  /** Breakdown by comic. */
  byComic?: Maybe<Array<ComicRetrievalSummary>>;
  /** Breakdown by status. */
  byStatus?: Maybe<Array<StatusCount>>;
  /** Number of failed retrievals. */
  failureCount: Scalars['Int']['output'];
  /** Number of skipped retrievals. */
  skippedCount: Scalars['Int']['output'];
  /** Number of successful retrievals. */
  successCount: Scalars['Int']['output'];
  /** Success rate as a percentage (0-100). */
  successRate: Scalars['Float']['output'];
  /** Total number of retrieval attempts. */
  totalAttempts: Scalars['Int']['output'];
};

/** Search results containing matched comics. */
export type SearchResults = {
  __typename?: 'SearchResults';
  /** List of comics matching the search query. */
  comics: Array<Comic>;
  /** The search query that was executed. */
  query: Scalars['String']['output'];
  /** Total number of results found. */
  totalCount: Scalars['Int']['output'];
};

/** Count of retrievals by status. */
export type StatusCount = {
  __typename?: 'StatusCount';
  /** Number of records with this status. */
  count: Scalars['Int']['output'];
  /** Retrieval status. */
  status: RetrievalStatusEnum;
};

/** Storage metrics for the comic cache. */
export type StorageMetrics = {
  __typename?: 'StorageMetrics';
  /** Total number of comics being tracked. */
  comicCount?: Maybe<Scalars['Int']['output']>;
  /** Per-comic storage breakdown. */
  comics?: Maybe<Array<ComicStorageMetric>>;
  /** Last time metrics were calculated. */
  lastUpdated?: Maybe<Scalars['DateTime']['output']>;
  /** Total storage used in bytes across all comics. */
  totalBytes?: Maybe<Scalars['Float']['output']>;
};

/** System resource metrics. */
export type SystemResources = {
  __typename?: 'SystemResources';
  /** Number of available CPU processors. */
  availableProcessors?: Maybe<Scalars['Int']['output']>;
  /** Free memory in bytes. */
  freeMemory?: Maybe<Scalars['Float']['output']>;
  /** Maximum memory the JVM can use in bytes. */
  maxMemory?: Maybe<Scalars['Float']['output']>;
  /** Memory usage percentage (0-100). */
  memoryUsagePercent?: Maybe<Scalars['Float']['output']>;
  /** Total memory in bytes. */
  totalMemory?: Maybe<Scalars['Float']['output']>;
};

/**
 * Input for updating an existing comic.
 * All fields are optional - only provided fields will be updated.
 */
export type UpdateComicInput = {
  /** Whether this comic is actively publishing. */
  active?: InputMaybe<Scalars['Boolean']['input']>;
  /** Author/creator of the comic. */
  author?: InputMaybe<Scalars['String']['input']>;
  /** Description of the comic. */
  description?: InputMaybe<Scalars['String']['input']>;
  /** Whether this comic is enabled for display. */
  enabled?: InputMaybe<Scalars['Boolean']['input']>;
  /** Display name of the comic. */
  name?: InputMaybe<Scalars['String']['input']>;
  /** Days of the week when this comic publishes. */
  publicationDays?: InputMaybe<Array<DayOfWeek>>;
  /** Source provider for this comic. */
  source?: InputMaybe<Scalars['String']['input']>;
  /** Identifier used by the source. */
  sourceIdentifier?: InputMaybe<Scalars['String']['input']>;
};

/**
 * Input for updating user profile.
 * All fields are optional - only provided fields will be updated.
 */
export type UpdateProfileInput = {
  /** New display name. */
  displayName?: InputMaybe<Scalars['String']['input']>;
  /** New email address. */
  email?: InputMaybe<Scalars['String']['input']>;
};

/** User account information. */
export type User = {
  __typename?: 'User';
  /** Account creation timestamp. */
  created?: Maybe<Scalars['DateTime']['output']>;
  /** User's display name. */
  displayName?: Maybe<Scalars['String']['output']>;
  /** User's email address. */
  email?: Maybe<Scalars['String']['output']>;
  /** Last login timestamp. */
  lastLogin?: Maybe<Scalars['DateTime']['output']>;
  /** User's assigned roles (e.g., "USER", "ADMIN"). */
  roles: Array<Scalars['String']['output']>;
  /** Unique username. */
  username: Scalars['String']['output'];
};

/** User preferences including favorites and display settings. */
export type UserPreference = {
  __typename?: 'UserPreference';
  /** Display settings as key-value pairs. */
  displaySettings?: Maybe<Scalars['JSON']['output']>;
  /** List of favorite comic IDs. */
  favoriteComics: Array<Scalars['Int']['output']>;
  /** Last read dates for each comic (comicId -> date). */
  lastReadDates: Array<LastReadEntry>;
  /** Username this preference belongs to. */
  username: Scalars['String']['output'];
};

/** Storage metrics for a specific year. */
export type YearlyStorageMetric = {
  __typename?: 'YearlyStorageMetric';
  /** Storage used in bytes for this year. */
  bytes: Scalars['Float']['output'];
  /** Number of images for this year. */
  imageCount: Scalars['Int']['output'];
  /** Year. */
  year: Scalars['Int']['output'];
};

export type LoginMutationVariables = Exact<{
  input: LoginInput;
}>;


export type LoginMutation = { __typename?: 'Mutation', login: { __typename?: 'AuthPayload', token: string, refreshToken: string, username: string, displayName?: string | null } };

export type RegisterMutationVariables = Exact<{
  input: RegisterInput;
}>;


export type RegisterMutation = { __typename?: 'Mutation', register: { __typename?: 'AuthPayload', token: string, refreshToken: string, username: string, displayName?: string | null } };

export type RefreshTokenMutationVariables = Exact<{
  refreshToken: Scalars['String']['input'];
}>;


export type RefreshTokenMutation = { __typename?: 'Mutation', refreshToken: { __typename?: 'AuthPayload', token: string, refreshToken: string, username: string, displayName?: string | null } };

export type ForgotPasswordMutationVariables = Exact<{
  email: Scalars['String']['input'];
}>;


export type ForgotPasswordMutation = { __typename?: 'Mutation', forgotPassword: boolean };

export type ResetPasswordMutationVariables = Exact<{
  token: Scalars['String']['input'];
  newPassword: Scalars['String']['input'];
}>;


export type ResetPasswordMutation = { __typename?: 'Mutation', resetPassword: { __typename?: 'AuthPayload', token: string, refreshToken: string, username: string, displayName?: string | null } };

export type ValidateTokenQueryVariables = Exact<{ [key: string]: never; }>;


export type ValidateTokenQuery = { __typename?: 'Query', validateToken: boolean };

export type GetMeQueryVariables = Exact<{ [key: string]: never; }>;


export type GetMeQuery = { __typename?: 'Query', me?: { __typename?: 'User', username: string, email?: string | null, displayName?: string | null, created?: any | null, lastLogin?: any | null, roles: Array<string> } | null };

export type GetUserPreferencesQueryVariables = Exact<{ [key: string]: never; }>;


export type GetUserPreferencesQuery = { __typename?: 'Query', preferences?: { __typename?: 'UserPreference', username: string, favoriteComics: Array<number>, displaySettings?: any | null, lastReadDates: Array<{ __typename?: 'LastReadEntry', comicId: number, date: any }> } | null };

export type GetComicsQueryVariables = Exact<{
  first?: InputMaybe<Scalars['Int']['input']>;
  after?: InputMaybe<Scalars['String']['input']>;
}>;


export type GetComicsQuery = { __typename?: 'Query', comics: { __typename?: 'ComicConnection', totalCount: number, edges: Array<{ __typename?: 'ComicEdge', cursor: string, node: { __typename?: 'Comic', id: number, name: string, description?: string | null, oldest?: any | null, newest?: any | null, avatarUrl?: string | null, lastStrip?: { __typename?: 'ComicStrip', imageUrl?: string | null, date: any } | null } }>, pageInfo: { __typename?: 'PageInfo', hasNextPage: boolean, hasPreviousPage: boolean, startCursor?: string | null, endCursor?: string | null } } };

export type GetComicQueryVariables = Exact<{
  id: Scalars['Int']['input'];
}>;


export type GetComicQuery = { __typename?: 'Query', comic?: { __typename?: 'Comic', id: number, name: string, description?: string | null, author?: string | null, source?: string | null, sourceIdentifier?: string | null, oldest?: any | null, newest?: any | null, avatarUrl?: string | null, lastStrip?: { __typename?: 'ComicStrip', imageUrl?: string | null, date: any } | null, firstStrip?: { __typename?: 'ComicStrip', imageUrl?: string | null, date: any } | null } | null };

export type GetComicStripQueryVariables = Exact<{
  comicId: Scalars['Int']['input'];
  date: Scalars['Date']['input'];
}>;


export type GetComicStripQuery = { __typename?: 'Query', strip?: { __typename?: 'ComicStrip', available: boolean, imageUrl?: string | null, date: any, previous?: { __typename?: 'ComicStrip', date: any } | null, next?: { __typename?: 'ComicStrip', date: any } | null } | null };

export type SearchComicsQueryVariables = Exact<{
  query: Scalars['String']['input'];
}>;


export type SearchComicsQuery = { __typename?: 'Query', search: { __typename?: 'SearchResults', comics: Array<{ __typename?: 'Comic', id: number, name: string, description?: string | null }> } };

export type AddFavoriteMutationVariables = Exact<{
  comicId: Scalars['Int']['input'];
}>;


export type AddFavoriteMutation = { __typename?: 'Mutation', addFavorite: { __typename?: 'UserPreference', favoriteComics: Array<number> } };

export type RemoveFavoriteMutationVariables = Exact<{
  comicId: Scalars['Int']['input'];
}>;


export type RemoveFavoriteMutation = { __typename?: 'Mutation', removeFavorite: { __typename?: 'UserPreference', favoriteComics: Array<number> } };

export type UpdateLastReadMutationVariables = Exact<{
  comicId: Scalars['Int']['input'];
  date: Scalars['Date']['input'];
}>;


export type UpdateLastReadMutation = { __typename?: 'Mutation', updateLastRead: { __typename?: 'UserPreference', lastReadDates: Array<{ __typename?: 'LastReadEntry', comicId: number, date: any }> } };

export type GetCombinedMetricsQueryVariables = Exact<{ [key: string]: never; }>;


export type GetCombinedMetricsQuery = { __typename?: 'Query', combinedMetrics?: { __typename?: 'CombinedMetrics', lastUpdated?: any | null, storage?: { __typename?: 'StorageMetrics', totalBytes?: number | null, comicCount?: number | null, lastUpdated?: any | null, comics?: Array<{ __typename?: 'ComicStorageMetric', comicId?: number | null, comicName: string, totalBytes: number, imageCount: number, yearlyBreakdown?: Array<{ __typename?: 'YearlyStorageMetric', year: number, bytes: number, imageCount: number }> | null }> | null } | null, access?: { __typename?: 'AccessMetrics', totalAccesses?: number | null, lastUpdated?: any | null, comics?: Array<{ __typename?: 'ComicAccessMetric', comicName: string, accessCount: number, averageAccessTimeMs?: number | null, lastAccessed?: any | null }> | null } | null } | null };

export type GetRetrievalSummaryQueryVariables = Exact<{
  fromDate?: InputMaybe<Scalars['Date']['input']>;
  toDate?: InputMaybe<Scalars['Date']['input']>;
}>;


export type GetRetrievalSummaryQuery = { __typename?: 'Query', retrievalSummary: { __typename?: 'RetrievalSummary', totalAttempts: number, successCount: number, failureCount: number, skippedCount: number, successRate: number, averageDurationMs?: number | null, byStatus?: Array<{ __typename?: 'StatusCount', status: RetrievalStatusEnum, count: number }> | null, byComic?: Array<{ __typename?: 'ComicRetrievalSummary', comicName: string, totalAttempts: number, successCount: number, failureCount: number }> | null } };

export type GetRetrievalRecordsQueryVariables = Exact<{
  comicName?: InputMaybe<Scalars['String']['input']>;
  status?: InputMaybe<RetrievalStatusEnum>;
  fromDate?: InputMaybe<Scalars['Date']['input']>;
  toDate?: InputMaybe<Scalars['Date']['input']>;
  limit?: InputMaybe<Scalars['Int']['input']>;
}>;


export type GetRetrievalRecordsQuery = { __typename?: 'Query', retrievalRecords: Array<{ __typename?: 'RetrievalRecord', id: string, comicName: string, comicDate: any, source?: string | null, status: RetrievalStatusEnum, retrievalDurationMs?: number | null, imageSize?: number | null, httpStatusCode?: number | null, errorMessage?: string | null }> };



export const LoginDocument = `
    mutation Login($input: LoginInput!) {
  login(input: $input) {
    token
    refreshToken
    username
    displayName
  }
}
    `;

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

export const RegisterDocument = `
    mutation Register($input: RegisterInput!) {
  register(input: $input) {
    token
    refreshToken
    username
    displayName
  }
}
    `;

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

export const RefreshTokenDocument = `
    mutation RefreshToken($refreshToken: String!) {
  refreshToken(refreshToken: $refreshToken) {
    token
    refreshToken
    username
    displayName
  }
}
    `;

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

export const ForgotPasswordDocument = `
    mutation ForgotPassword($email: String!) {
  forgotPassword(email: $email)
}
    `;

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

export const ResetPasswordDocument = `
    mutation ResetPassword($token: String!, $newPassword: String!) {
  resetPassword(token: $token, newPassword: $newPassword) {
    token
    refreshToken
    username
    displayName
  }
}
    `;

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

export const ValidateTokenDocument = `
    query ValidateToken {
  validateToken
}
    `;

export const useValidateTokenQuery = <
      TData = ValidateTokenQuery,
      TError = unknown
    >(
      variables?: ValidateTokenQueryVariables,
      options?: Omit<UseQueryOptions<ValidateTokenQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ValidateTokenQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ValidateTokenQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['ValidateToken'] : ['ValidateToken', variables],
    queryFn: fetcher<ValidateTokenQuery, ValidateTokenQueryVariables>(ValidateTokenDocument, variables),
    ...options
  }
    )};

useValidateTokenQuery.getKey = (variables?: ValidateTokenQueryVariables) => variables === undefined ? ['ValidateToken'] : ['ValidateToken', variables];

export const useInfiniteValidateTokenQuery = <
      TData = InfiniteData<ValidateTokenQuery>,
      TError = unknown
    >(
      variables: ValidateTokenQueryVariables,
      options: Omit<UseInfiniteQueryOptions<ValidateTokenQuery, TError, TData>, 'queryKey'> & { queryKey?: UseInfiniteQueryOptions<ValidateTokenQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useInfiniteQuery<ValidateTokenQuery, TError, TData>(
      (() => {
    const { queryKey: optionsQueryKey, ...restOptions } = options;
    return {
      queryKey: optionsQueryKey ?? variables === undefined ? ['ValidateToken.infinite'] : ['ValidateToken.infinite', variables],
      queryFn: (metaData) => fetcher<ValidateTokenQuery, ValidateTokenQueryVariables>(ValidateTokenDocument, {...variables, ...(metaData.pageParam ?? {})})(),
      ...restOptions
    }
  })()
    )};

useInfiniteValidateTokenQuery.getKey = (variables?: ValidateTokenQueryVariables) => variables === undefined ? ['ValidateToken.infinite'] : ['ValidateToken.infinite', variables];


useValidateTokenQuery.fetcher = (variables?: ValidateTokenQueryVariables, options?: RequestInit['headers']) => fetcher<ValidateTokenQuery, ValidateTokenQueryVariables>(ValidateTokenDocument, variables, options);

export const GetMeDocument = `
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
    `;

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

export const GetUserPreferencesDocument = `
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
    `;

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

export const GetComicsDocument = `
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
    `;

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

export const GetComicDocument = `
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
    }
    firstStrip {
      imageUrl
      date
    }
  }
}
    `;

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

export const GetComicStripDocument = `
    query GetComicStrip($comicId: Int!, $date: Date!) {
  strip(comicId: $comicId, date: $date) {
    available
    imageUrl
    date
    previous {
      date
    }
    next {
      date
    }
  }
}
    `;

export const useGetComicStripQuery = <
      TData = GetComicStripQuery,
      TError = unknown
    >(
      variables: GetComicStripQueryVariables,
      options?: Omit<UseQueryOptions<GetComicStripQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<GetComicStripQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<GetComicStripQuery, TError, TData>(
      {
    queryKey: ['GetComicStrip', variables],
    queryFn: fetcher<GetComicStripQuery, GetComicStripQueryVariables>(GetComicStripDocument, variables),
    ...options
  }
    )};

useGetComicStripQuery.getKey = (variables: GetComicStripQueryVariables) => ['GetComicStrip', variables];

export const useInfiniteGetComicStripQuery = <
      TData = InfiniteData<GetComicStripQuery>,
      TError = unknown
    >(
      variables: GetComicStripQueryVariables,
      options: Omit<UseInfiniteQueryOptions<GetComicStripQuery, TError, TData>, 'queryKey'> & { queryKey?: UseInfiniteQueryOptions<GetComicStripQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useInfiniteQuery<GetComicStripQuery, TError, TData>(
      (() => {
    const { queryKey: optionsQueryKey, ...restOptions } = options;
    return {
      queryKey: optionsQueryKey ?? ['GetComicStrip.infinite', variables],
      queryFn: (metaData) => fetcher<GetComicStripQuery, GetComicStripQueryVariables>(GetComicStripDocument, {...variables, ...(metaData.pageParam ?? {})})(),
      ...restOptions
    }
  })()
    )};

useInfiniteGetComicStripQuery.getKey = (variables: GetComicStripQueryVariables) => ['GetComicStrip.infinite', variables];


useGetComicStripQuery.fetcher = (variables: GetComicStripQueryVariables, options?: RequestInit['headers']) => fetcher<GetComicStripQuery, GetComicStripQueryVariables>(GetComicStripDocument, variables, options);

export const SearchComicsDocument = `
    query SearchComics($query: String!) {
  search(query: $query) {
    comics {
      id
      name
      description
    }
  }
}
    `;

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

export const AddFavoriteDocument = `
    mutation AddFavorite($comicId: Int!) {
  addFavorite(comicId: $comicId) {
    favoriteComics
  }
}
    `;

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

export const RemoveFavoriteDocument = `
    mutation RemoveFavorite($comicId: Int!) {
  removeFavorite(comicId: $comicId) {
    favoriteComics
  }
}
    `;

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

export const UpdateLastReadDocument = `
    mutation UpdateLastRead($comicId: Int!, $date: Date!) {
  updateLastRead(comicId: $comicId, date: $date) {
    lastReadDates {
      comicId
      date
    }
  }
}
    `;

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

export const GetCombinedMetricsDocument = `
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
    `;

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

export const GetRetrievalSummaryDocument = `
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
    `;

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

export const GetRetrievalRecordsDocument = `
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
    `;

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
