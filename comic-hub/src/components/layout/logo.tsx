import Link from 'next/link';
import { ComicsHubMark } from '@/components/brand/comics-hub-mark';

/**
 * The header lockup: the mark, then "Comics" over "Hub" in the display face, tilted
 * to echo the mark's folded page, with "Hub" on a highlighter stripe.
 */
export function Logo() {
  return (
    <Link href="/" aria-label="Comics Hub home" className="flex shrink-0 items-center gap-1.5 rounded-md">
      <ComicsHubMark className="size-11" />
      <span aria-hidden="true" className="flex -rotate-[4deg] origin-left flex-col gap-px font-display font-bold leading-none text-ink text-[19px]">
        <span>Comics</span>
        <span className="-ml-1 self-start px-1 bg-[linear-gradient(transparent_52%,var(--color-highlight)_52%,var(--color-highlight)_92%,transparent_92%)]">
          Hub
        </span>
      </span>
    </Link>
  );
}
