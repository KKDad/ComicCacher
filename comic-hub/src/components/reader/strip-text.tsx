'use client';

import { useId, useState } from 'react';

/**
 * The strip image's alt: the source's alt text when it has one, otherwise the comic and date.
 */
export function stripAlt(comicName: string, formattedDate: string, altText: string | null | undefined): string {
  return altText ? `${comicName}, ${formattedDate}: ${altText}` : `${comicName} - ${formattedDate}`;
}

interface StripTextProps {
  transcript: string | null | undefined;
  altText: string | null | undefined;
  className?: string;
}

/**
 * "Show alt text" and "Show transcript" toggles under a strip. They stay closed until tapped: hover captions don't work on
 * phones, and the alt text is often the punchline. Renders nothing when the strip has neither.
 */
export function StripText({ transcript, altText, className = '' }: StripTextProps) {
  if (!transcript && !altText) return null;

  return (
    // Taps and drags stay here, so in the mobile reader they don't also toggle its controls or swipe to another strip,
    // and a long transcript can scroll
    <div
      className={className}
      onClick={(e) => e.stopPropagation()}
      onTouchStart={(e) => e.stopPropagation()}
      onTouchMove={(e) => e.stopPropagation()}
      onTouchEnd={(e) => e.stopPropagation()}
    >
      {altText && <TextToggle label="alt text" text={altText} />}
      {transcript && <TextToggle label="transcript" text={transcript} />}
    </div>
  );
}

function TextToggle({ label, text }: { label: string; text: string }) {
  const [expanded, setExpanded] = useState(false);
  const id = useId();

  return (
    <div>
      <button
        type="button"
        onClick={() => setExpanded(!expanded)}
        aria-expanded={expanded}
        aria-controls={id}
        className="text-xs text-ink-subtle hover:text-ink transition-colors py-1"
      >
        {expanded ? `Hide ${label}` : `Show ${label}`}
      </button>
      {expanded && (
        <p id={id} className="text-xs text-ink-subtle mt-1 whitespace-pre-wrap">{text}</p>
      )}
    </div>
  );
}
