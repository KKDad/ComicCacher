import type { SourceSettings } from '@/types/sources';

function seconds(ms: number): string {
  if (ms === 0) return '0 s';
  const s = ms / 1000;
  return s >= 60 ? `${Math.round(s / 6) / 10} min` : `${Math.round(s * 10) / 10} s`;
}

/** A source's request and backfill settings, read-only: they come from application.properties. */
export function SourceSettingsList({ settings }: { settings: SourceSettings }) {
  const rows: [string, string][] = [
    ['Delay between requests', settings.throttleMaxDelayMs === 0 ? 'None' : `${seconds(settings.throttleMinDelayMs)} – ${seconds(settings.throttleMaxDelayMs)}`],
    [
      'On HTTP 429',
      settings.retryMaxAttempts <= 1
        ? 'Give up at once'
        : `Up to ${settings.retryMaxAttempts} attempts, backing off ${seconds(settings.retryInitialBackoffMs)} – ${seconds(settings.retryMaxBackoffMs)}`,
    ],
    ['Backfill', settings.backfillEnabled ? 'On' : 'Off'],
    ['Backfill reaches back', `${settings.backfillMaxDaysBack} days`],
    ['Backfill per run', `${settings.backfillMaxPerRun} strips${settings.backfillMaxPerDay > 0 ? `, ${settings.backfillMaxPerDay} a day` : ''}`],
    ['Recent days checked first', `${settings.backfillRecentDays}`],
    ['Prefer colour strips', settings.backfillPreferColor ? 'Yes' : 'No'],
    ['User-Agent', settings.userAgent ?? 'Default'],
  ];

  return (
    <div className="space-y-2">
      <dl className="grid grid-cols-[max-content_1fr] gap-x-4 gap-y-1 text-sm">
        {rows.map(([label, value]) => (
          <div key={label} className="contents">
            <dt className="text-muted-foreground">{label}</dt>
            <dd className="min-w-0 break-words">{value}</dd>
          </div>
        ))}
      </dl>
      <p className="text-xs text-muted-foreground">Set in application.properties; changing them needs a redeploy.</p>
    </div>
  );
}
