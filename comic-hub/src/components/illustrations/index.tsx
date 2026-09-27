// Spot illustrations for empty and error states, in the app's ink-outline,
// flat-fill comic style. Paper areas follow --illustration-paper so they dim in dark mode.

interface IllustrationProps {
  className?: string;
}

function Frame({ className, children }: IllustrationProps & { children: React.ReactNode }) {
  return (
    <svg viewBox="0 0 200 150" xmlns="http://www.w3.org/2000/svg" className={className} aria-hidden="true">
      {children}
    </svg>
  );
}

/** An open newspaper with a thinking bubble: nothing read yet. */
export function HistoryIllustration({ className }: IllustrationProps) {
  return (
    <Frame className={className}>
      <rect x="26" y="44" width="112" height="88" rx="4" style={{ fill: 'var(--illustration-paper)', stroke: '#1b1b1b', strokeWidth: 3 }}></rect>
      <path d="M40 64h44M40 78h84M40 92h84M40 106h84M40 120h56" style={{ fill: 'none', stroke: '#1b1b1b', strokeWidth: 3, strokeLinecap: 'round' }}></path>
      <rect x="94" y="56" width="30" height="16" rx="2" style={{ fill: '#7FB2F0', stroke: '#1b1b1b', strokeWidth: 2.5 }}></rect>
      <path d="M130 18h40a12 12 0 0 1 12 12v14a12 12 0 0 1-12 12h-22l-14 12v-12h-4a12 12 0 0 1-12-12V30a12 12 0 0 1 12-12z" style={{ fill: '#F2B32A', stroke: '#1b1b1b', strokeWidth: 3, strokeLinejoin: 'round' }}></path>
      <circle cx="138" cy="37" r="4" style={{ fill: '#1b1b1b' }}></circle>
      <circle cx="150" cy="37" r="4" style={{ fill: '#1b1b1b' }}></circle>
      <circle cx="162" cy="37" r="4" style={{ fill: '#1b1b1b' }}></circle>
    </Frame>
  );
}

/** A heart in a speech bubble: no favorites yet. */
export function FavoritesIllustration({ className }: IllustrationProps) {
  return (
    <Frame className={className}>
      <path d="M52 22h96a26 26 0 0 1 26 26v28a26 26 0 0 1-26 26H92l-26 24v-24H52a26 26 0 0 1-26-26V48a26 26 0 0 1 26-26z" style={{ fill: 'var(--illustration-paper)', stroke: '#1b1b1b', strokeWidth: 3, strokeLinejoin: 'round' }}></path>
      <path d="M100 88l-20-19c-8-8-8-19 1-24 6-3 13-1 19 6 6-7 13-9 19-6 9 5 9 16 1 24z" style={{ fill: '#C8322A', stroke: '#1b1b1b', strokeWidth: 3, strokeLinejoin: 'round' }}></path>
      <path d="M176 10l4 10 10 4-10 4-4 10-4-10-10-4 10-4z" style={{ fill: '#F2B32A', stroke: '#1b1b1b', strokeWidth: 2, strokeLinejoin: 'round' }}></path>
      <path d="M18 104l3 7 7 3-7 3-3 7-3-7-7-3 7-3z" style={{ fill: '#F2B32A', stroke: '#1b1b1b', strokeWidth: 2, strokeLinejoin: 'round' }}></path>
    </Frame>
  );
}

/** A strip with a torn-out middle panel: a strip that failed to load. */
export function BrokenIllustration({ className }: IllustrationProps) {
  return (
    <Frame className={className}>
      <rect x="10" y="36" width="54" height="84" rx="2" style={{ fill: 'var(--illustration-paper)', stroke: '#1b1b1b', strokeWidth: 3 }}></rect>
      <circle cx="37" cy="92" r="14" style={{ fill: '#f3c9a0', stroke: '#1b1b1b', strokeWidth: 2.5 }}></circle>
      <ellipse cx="37" cy="56" rx="20" ry="10" style={{ fill: '#ffffff', stroke: '#1b1b1b', strokeWidth: 2.5 }}></ellipse>
      <path d="M73 36h54v34l-8 6 8 8-6 8 6 8v20H73V96l7-6-7-8 6-8-6-8z" style={{ fill: 'none', stroke: '#1b1b1b', strokeWidth: 3, strokeDasharray: '6 5', strokeLinejoin: 'round' }}></path>
      <text x="100" y="90" textAnchor="middle" style={{ fontFamily: 'var(--font-display)', fontWeight: 700, fontSize: '30px', fill: '#C8322A' }}>?!</text>
      <rect x="136" y="36" width="54" height="84" rx="2" style={{ fill: 'var(--illustration-paper)', stroke: '#1b1b1b', strokeWidth: 3 }}></rect>
      <circle cx="163" cy="94" r="14" style={{ fill: '#f3c9a0', stroke: '#1b1b1b', strokeWidth: 2.5 }}></circle>
      <path d="M150 50h26M150 60h18" style={{ stroke: '#1b1b1b', strokeWidth: 3, strokeLinecap: 'round' }}></path>
    </Frame>
  );
}

/** A sleepy night panel: nothing new today. */
export function QuietIllustration({ className }: IllustrationProps) {
  return (
    <Frame className={className}>
      <rect x="24" y="22" width="152" height="112" rx="4" style={{ fill: '#1F3A63', stroke: '#1b1b1b', strokeWidth: 3 }}></rect>
      <path d="M58 40a16 16 0 1 0 16 22 13 13 0 1 1-16-22z" style={{ fill: '#F2C14E', stroke: '#1b1b1b', strokeWidth: 2.5, strokeLinejoin: 'round' }}></path>
      <circle cx="120" cy="44" r="2.5" style={{ fill: '#F3EFE7' }}></circle>
      <circle cx="150" cy="60" r="2" style={{ fill: '#F3EFE7' }}></circle>
      <circle cx="96" cy="34" r="2" style={{ fill: '#F3EFE7' }}></circle>
      <path d="M60 134v-20a40 40 0 0 1 80 0v20z" style={{ fill: 'var(--illustration-paper)', stroke: '#1b1b1b', strokeWidth: 3, strokeLinejoin: 'round' }}></path>
      <path d="M86 108q6 5 12 0M104 108q6 5 12 0" style={{ fill: 'none', stroke: '#1b1b1b', strokeWidth: 3, strokeLinecap: 'round' }}></path>
      <text x="150" y="96" textAnchor="middle" style={{ fontFamily: 'var(--font-display)', fontWeight: 700, fontSize: '20px', fill: '#F3EFE7' }}>Zzz</text>
    </Frame>
  );
}
