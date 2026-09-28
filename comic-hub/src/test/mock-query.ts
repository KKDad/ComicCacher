import type {
  MutationFunctionContext,
  UseInfiniteQueryResult,
  UseMutationOptions,
  UseMutationResult,
  UseQueryResult,
} from '@tanstack/react-query';

/**
 * A TanStack query result for `vi.mocked(useXQuery).mockReturnValue(...)`. Tests set the fields
 * the component reads (`data`, `isLoading`, ...); the result type comes from the mocked hook.
 */
export function mockQueryResult<TData, TError = unknown>(
  partial: Partial<UseQueryResult<TData, TError>>,
): UseQueryResult<TData, TError> {
  return partial as UseQueryResult<TData, TError>;
}

/** A TanStack mutation result for `vi.mocked(useXMutation).mockReturnValue(...)`, as for `mockQueryResult`. */
export function mockMutationResult<TData, TError = unknown, TVariables = void, TContext = unknown>(
  partial: Partial<UseMutationResult<TData, TError, TVariables, TContext>>,
): UseMutationResult<TData, TError, TVariables, TContext> {
  return partial as UseMutationResult<TData, TError, TVariables, TContext>;
}

/** A TanStack infinite query result for `vi.mocked(useInfiniteXQuery).mockReturnValue(...)`, as for `mockQueryResult`. */
export function mockInfiniteQueryResult<TData, TError = unknown>(
  partial: Partial<UseInfiniteQueryResult<TData, TError>>,
): UseInfiniteQueryResult<TData, TError> {
  return partial as UseInfiniteQueryResult<TData, TError>;
}

// The options a generated mutation hook takes (its TError and TContext default to unknown)
type MutationOptionsOf<THook> = THook extends (options?: infer TOptions) => unknown ? NonNullable<TOptions> : never;
type OnSuccessOf<THook> = MutationOptionsOf<THook> extends { onSuccess?: (...args: infer TArgs) => unknown } ? TArgs : never;
type MutationDataOf<THook> = OnSuccessOf<THook>[0];
type MutationVariablesOf<THook> = OnSuccessOf<THook>[1];

/**
 * Mocks a generated mutation hook and captures the options the component passes it, so a test
 * can fire the component's `onSuccess` / `onError` the way TanStack would after a request.
 */
export function captureMutation<THook extends (options?: never) => unknown>(
  hook: THook,
  result: Partial<UseMutationResult<MutationDataOf<THook>, unknown, MutationVariablesOf<THook>, unknown>> = {
    mutate: vi.fn<() => void>(),
    isPending: false,
  },
) {
  type Options = UseMutationOptions<MutationDataOf<THook>, unknown, MutationVariablesOf<THook>, unknown>;
  let options: Options | undefined;
  const capture = (opts?: Options) => {
    options = opts;
    return mockMutationResult(result);
  };
  vi.mocked(hook).mockImplementation(capture as unknown as THook);
  // Components here don't read the mutation context; TanStack passes the client and mutation key
  const context = {} as MutationFunctionContext;
  return {
    succeed: (data: MutationDataOf<THook>, variables: MutationVariablesOf<THook>) =>
      options?.onSuccess?.(data, variables, undefined, context),
    fail: (error: Error, variables: MutationVariablesOf<THook>) => options?.onError?.(error, variables, undefined, context),
  };
}
