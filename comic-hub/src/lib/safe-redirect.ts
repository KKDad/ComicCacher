/**
 * Returns `target` only when it is a same-origin path, otherwise `fallback`.
 * Guards redirect parameters such as `?from=` against sending users off-site.
 */
export function safeRedirectPath(target: string | null | undefined, fallback = '/'): string {
  if (!target || !target.startsWith('/') || target.startsWith('//') || target.startsWith('/\\')) {
    return fallback;
  }
  return target;
}
