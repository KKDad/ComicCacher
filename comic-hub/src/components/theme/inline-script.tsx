'use client';

/**
 * A script that runs while the browser parses the server HTML, before the first paint.
 * React warns when it renders a <script> on the client (it would never run there), so the
 * client render marks it text/plain; suppressHydrationWarning accepts the type mismatch.
 * This is the pattern from Next's "Preventing flash before hydration" guide.
 */
export function InlineScript({ html }: { html: string }) {
  return (
    <script
      type={typeof window === 'undefined' ? 'text/javascript' : 'text/plain'}
      suppressHydrationWarning
      dangerouslySetInnerHTML={{ __html: html }}
    />
  );
}
