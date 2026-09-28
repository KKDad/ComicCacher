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

/**
 * The login page URL that returns the user to `from` after signing in.
 * The login page passes `from` back through `safeRedirectPath`.
 */
export function loginPath(from: string | null | undefined): string {
  const target = safeRedirectPath(from);
  return target === '/' ? '/login' : `/login?from=${encodeURIComponent(target)}`;
}
