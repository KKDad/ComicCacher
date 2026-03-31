'use client';

import { useState } from 'react';
import { MoreVertical, Shuffle, ExternalLink, Info } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { StripSkeleton } from '@/components/reader/strip-skeleton';
import { formatFullDate } from '@/lib/date-utils';
import { useUser } from '@/contexts/user-context';
import { isAdmin } from '@/lib/roles';
import type { GridComic } from '@/hooks/use-grid-reader';

interface GridStripCardProps {
  comic: GridComic;
  date: string;
  onImageClick: () => void;
  onRandom?: (comicId: number) => void;
}

export function GridStripCard({ comic, date, onImageClick, onRandom }: GridStripCardProps) {
  const [imageLoaded, setImageLoaded] = useState(false);
  const [imageError, setImageError] = useState(false);
  const user = useUser();
  const admin = user ? isAdmin(user.roles) : false;

  const strip = comic.strip;
  const formattedDate = formatFullDate(date);
  const hasStrip = strip?.available && strip.imageUrl;

  return (
    <div className="bg-card rounded-lg overflow-hidden">
      {/* Header: avatar + name + hamburger */}
      <div className="flex items-center gap-3 px-4 py-3">
        {comic.avatarUrl ? (
          <img
            src={comic.avatarUrl}
            alt=""
            className="h-8 w-8 rounded-full object-cover shrink-0"
          />
        ) : (
          <div className="h-8 w-8 rounded-full bg-muted shrink-0" />
        )}
        <span className="font-medium text-sm text-ink truncate flex-1">{comic.name}</span>
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="icon" className="h-8 w-8 shrink-0" aria-label="Strip actions">
              <MoreVertical className="h-4 w-4" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="z-popover">
            {onRandom && (
              <DropdownMenuItem onClick={() => onRandom(comic.id)}>
                <Shuffle className="h-4 w-4 mr-2" />
                Random
              </DropdownMenuItem>
            )}
            <DropdownMenuItem asChild>
              <a href={`/comics/${comic.id}/read?date=${date}`}>
                <ExternalLink className="h-4 w-4 mr-2" />
                Open standalone
              </a>
            </DropdownMenuItem>
            <DropdownMenuItem asChild>
              <a href={`/comics/${comic.id}`}>
                <Info className="h-4 w-4 mr-2" />
                About
              </a>
            </DropdownMenuItem>
            {admin && (
              <>
                <DropdownMenuSeparator />
                <DropdownMenuItem asChild>
                  <a href="/metrics">Statistics</a>
                </DropdownMenuItem>
                <DropdownMenuItem asChild>
                  <a href="/batch-jobs">Batch refresh</a>
                </DropdownMenuItem>
              </>
            )}
          </DropdownMenuContent>
        </DropdownMenu>
      </div>

      {/* Strip image */}
      {hasStrip ? (
        <button
          type="button"
          className="w-full cursor-zoom-in"
          onClick={onImageClick}
          aria-label={`View ${comic.name} fullscreen`}
        >
          <div
            className="relative overflow-hidden"
            style={
              strip.width && strip.height
                ? { aspectRatio: `${strip.width}/${strip.height}` }
                : { aspectRatio: '3/1' }
            }
          >
            <div
              className={`absolute inset-0 transition-opacity duration-300 ${imageLoaded ? 'opacity-0' : 'opacity-100'}`}
            >
              <StripSkeleton />
            </div>
            {imageError ? (
              <div className="absolute inset-0 bg-card flex items-center justify-center">
                <p className="text-sm text-ink-muted">Failed to load strip</p>
              </div>
            ) : (
              <img
                src={strip.imageUrl!}
                alt={`${comic.name} - ${formattedDate}`}
                loading="lazy"
                className={`absolute inset-0 w-full h-full object-contain transition-opacity duration-300 ${imageLoaded ? 'opacity-100' : 'opacity-0'}`}
                onLoad={() => setImageLoaded(true)}
                onError={() => setImageError(true)}
              />
            )}
          </div>
        </button>
      ) : (
        <div className="flex items-center justify-center py-8 text-sm text-ink-muted">
          No strip available for {formattedDate}
        </div>
      )}

      {/* Transcript */}
      {strip?.transcript && <TranscriptToggle transcript={strip.transcript} />}
    </div>
  );
}

function TranscriptToggle({ transcript }: { transcript: string }) {
  const [expanded, setExpanded] = useState(false);

  return (
    <div className="px-4 py-2 border-t border-border">
      <button
        type="button"
        onClick={() => setExpanded(!expanded)}
        className="text-xs text-ink-subtle hover:text-ink transition-colors"
      >
        {expanded ? 'Hide transcript' : 'Show transcript'}
      </button>
      {expanded && (
        <p className="text-xs text-ink-muted mt-1 whitespace-pre-wrap">{transcript}</p>
      )}
    </div>
  );
}
