'use client';

import Link from 'next/link';
import { CalendarClock, ExternalLink, History, ImageDown, Info, LibraryBig, MoreHorizontal } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { Switch } from '@/components/ui/switch';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip';
import { ComicThumb } from './comic-thumb';
import { describeStart } from './start-date-dialog';
import { formatMediumDate } from '@/lib/date-utils';
import type { SourceComic } from '@/types/sources';

/** What a row shows: a catalog entry (maybe configured), or a configured comic its catalog doesn't list. */
export interface CatalogRowItem {
  identifier: string;
  name: string;
  author?: string | null;
  description?: string | null;
  tags?: string[];
  pageUrl?: string | null;
  thumbnailUrl?: string | null;
  thumbnailPending?: boolean;
  startDate?: string | null;
  startStripNumber?: number | null;
  removedAt?: string | null;
  comic?: SourceComic | null;
}

export interface CatalogRowActions {
  /** Downloading (active) switched; adds the comic first when it isn't configured. */
  onDownloadingChange: (item: CatalogRowItem, on: boolean) => void;
  /** Visible (enabled) switched; adds the comic first when it isn't configured. */
  onVisibleChange: (item: CatalogRowItem, on: boolean) => void;
  onBackfill: (comic: SourceComic) => void;
  onFetchAvatar: (comic: SourceComic) => void;
  onEditStart: (comic: SourceComic) => void;
}

interface CatalogRowProps extends CatalogRowActions {
  item: CatalogRowItem;
  numbered: boolean;
  canChange: boolean;
  busy: boolean;
  /** The tag the list is filtered by, shown pressed. */
  activeTag?: string | null;
  /** Filters the list by a tag; chips are plain labels without it. */
  onTagClick?: (tag: string) => void;
}

function startText(item: CatalogRowItem, numbered: boolean): string | null {
  if (item.comic) {
    return describeStart(item.comic, numbered);
  }
  if (numbered) {
    return item.startStripNumber != null ? `#${item.startStripNumber}` : null;
  }
  return item.startDate ? formatMediumDate(item.startDate) : null;
}

/** One comic in a source's catalog, with its Downloading and Visible switches. */
export function CatalogRow({ item, numbered, canChange, busy, activeTag, onTagClick, ...actions }: CatalogRowProps) {
  const comic = item.comic ?? null;
  const picture = comic?.avatarAvailable ? comic.avatarUrl : item.thumbnailUrl;
  const start = startText(item, numbered);
  const disabledReason = canChange ? undefined : 'Only admins can change comics';

  return (
    <li className="flex flex-wrap items-center gap-x-4 gap-y-2 border-b px-4 py-3 last:border-b-0">
      <ComicThumb name={item.name} src={picture} pending={!!item.thumbnailPending || !!comic?.avatarPending} />

      <div className="min-w-0 flex-1 basis-48">
        <div className="flex flex-wrap items-center gap-2">
          {comic ? (
            <Link href={`/comics/${comic.id}`} className="font-medium hover:underline">
              {item.name}
            </Link>
          ) : (
            <span className="font-medium">{item.name}</span>
          )}
          {item.description && (
            <Tooltip>
              <TooltipTrigger asChild>
                <button type="button" aria-label={`About ${item.name}`} className="text-muted-foreground hover:text-foreground">
                  <Info className="size-4" aria-hidden />
                </button>
              </TooltipTrigger>
              <TooltipContent side="bottom" className="max-w-xs text-left">
                {item.description}
              </TooltipContent>
            </Tooltip>
          )}
          {item.removedAt && <Badge variant="destructive">No longer listed</Badge>}
          {comic && !comic.enabled && <Badge variant="secondary">Hidden</Badge>}
        </div>
        <div className="flex flex-wrap items-center gap-x-3 text-sm text-muted-foreground">
          {item.author && <span>{item.author}</span>}
          {item.pageUrl ? (
            <a href={item.pageUrl} target="_blank" rel="noopener noreferrer" className="inline-flex items-center gap-1 font-mono text-xs hover:underline">
              {item.identifier}
              <ExternalLink className="size-3" aria-hidden />
            </a>
          ) : (
            <span className="font-mono text-xs">{item.identifier}</span>
          )}
          {start && (
            <span title="Where the comic starts at its source" className="text-xs">
              Starts {start}
            </span>
          )}
          {item.tags?.map((tag) =>
            onTagClick ? (
              <button
                key={tag}
                type="button"
                aria-pressed={activeTag === tag}
                title={activeTag === tag ? 'Show every genre' : `Show only ${tag}`}
                onClick={() => onTagClick(tag)}
              >
                <Badge variant={activeTag === tag ? 'default' : 'outline'}>{tag}</Badge>
              </button>
            ) : (
              <Badge key={tag} variant="outline">
                {tag}
              </Badge>
            ),
          )}
        </div>
      </div>

      <div className="flex items-center gap-4" title={disabledReason}>
        <label className="flex items-center gap-2 text-sm">
          <Switch
            checked={!!comic?.active}
            disabled={!canChange || busy}
            onCheckedChange={(on) => actions.onDownloadingChange(item, on)}
            aria-label={`Download ${item.name}`}
          />
          <span>Downloading</span>
        </label>
        <label className="flex items-center gap-2 text-sm">
          <Switch
            checked={!!comic?.enabled}
            disabled={!canChange || busy}
            onCheckedChange={(on) => actions.onVisibleChange(item, on)}
            aria-label={`Show ${item.name} to readers`}
          />
          <span>Visible</span>
        </label>

        {comic && canChange ? (
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon-sm" aria-label={`More for ${item.name}`}>
                <MoreHorizontal aria-hidden />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem onSelect={() => actions.onBackfill(comic)} disabled={!comic.active}>
                <History aria-hidden /> Backfill this comic
              </DropdownMenuItem>
              <DropdownMenuItem onSelect={() => actions.onEditStart(comic)}>
                <CalendarClock aria-hidden /> Start date…
              </DropdownMenuItem>
              <DropdownMenuItem onSelect={() => actions.onFetchAvatar(comic)} disabled={comic.avatarPending}>
                <ImageDown aria-hidden /> Fetch avatar
              </DropdownMenuItem>
              <DropdownMenuItem asChild>
                <Link href={`/comics/${comic.id}`}>
                  <LibraryBig aria-hidden /> Open in Library
                </Link>
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        ) : (
          <span className="size-8" aria-hidden />
        )}
      </div>
    </li>
  );
}
