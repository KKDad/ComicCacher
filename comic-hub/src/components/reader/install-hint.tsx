'use client';

import { useState } from 'react';
import { Share, X } from 'lucide-react';
import { useHydrated } from '@/hooks/use-hydrated';

export const INSTALL_HINT_DISMISSED_KEY = 'comics-hub:install-hint-dismissed';

/** iPhone, or an iPad reporting itself as a Mac. */
function isIos(): boolean {
  return (
    /iPhone|iPad|iPod/.test(navigator.userAgent) ||
    (/Macintosh/.test(navigator.userAgent) && navigator.maxTouchPoints > 1)
  );
}

/** Running from the home screen rather than in a Safari tab. */
function isInstalled(): boolean {
  return (
    (navigator as Navigator & { standalone?: boolean }).standalone === true ||
    window.matchMedia('(display-mode: standalone)').matches
  );
}

function wasDismissed(): boolean {
  try {
    return localStorage.getItem(INSTALL_HINT_DISMISSED_KEY) === '1';
  } catch {
    return false;
  }
}

function rememberDismissed() {
  try {
    localStorage.setItem(INSTALL_HINT_DISMISSED_KEY, '1');
  } catch {
    // Storage blocked: the hint just comes back next time
  }
}

interface InstallHintProps {
  /** From useFullscreen: the hint is only for browsers with no Fullscreen API. */
  fullscreenSupported: boolean | null;
}

/**
 * iPhone Safari can't go full screen, but the home-screen app opens without Safari's
 * bars. Says so once, until dismissed.
 */
export function InstallHint({ fullscreenSupported }: InstallHintProps) {
  const hydrated = useHydrated();
  const [dismissed, setDismissed] = useState(false);

  if (!hydrated || dismissed || fullscreenSupported !== false) return null;
  if (!isIos() || isInstalled() || wasDismissed()) return null;

  const dismiss = () => {
    rememberDismissed();
    setDismissed(true);
  };

  return (
    <div
      role="note"
      className="absolute left-3 right-3 z-modal flex items-center gap-3 rounded-lg border border-border bg-chrome px-3 py-2 shadow-lg"
      style={{ bottom: 'calc(4.5rem + env(safe-area-inset-bottom))' }}
      onClick={(e) => e.stopPropagation()}
    >
      <Share className="h-5 w-5 shrink-0 text-ink-subtle" aria-hidden="true" />
      <p className="flex-1 text-sm text-ink">
        For full-screen reading, tap Share, then Add to Home Screen.
      </p>
      <button
        type="button"
        onClick={dismiss}
        aria-label="Dismiss"
        className="-mr-1 rounded-md p-2 text-ink-subtle hover:text-ink"
      >
        <X className="h-4 w-4" />
      </button>
    </div>
  );
}
