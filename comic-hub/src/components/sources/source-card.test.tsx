import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { SourceCard } from './source-card';
import { renderWithProviders } from '@/test/test-utils';
import { mockSource } from '@/test/source-fixtures';

function renderCard(overrides: Parameters<typeof mockSource>[0] = {}, canChange = true) {
  const onRefresh = vi.fn();
  const onBackfill = vi.fn();
  renderWithProviders(<SourceCard source={mockSource(overrides)} canChange={canChange} onRefresh={onRefresh} onBackfill={onBackfill} />);
  return { onRefresh, onBackfill };
}

describe('SourceCard', () => {
  it('shows how much of the catalog is configured', () => {
    renderCard();

    expect(screen.getByText('GoComics')).toBeInTheDocument();
    expect(screen.getByText('Daily')).toBeInTheDocument();
    expect(screen.getByText(/comics configured/)).toHaveTextContent('8 of 402 comics configured · 7 downloading');
    expect(screen.getByText(/Catalog read/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Browse comics/ })).toHaveAttribute('href', '/sources/gocomics');
  });

  it('says when the catalog was never read, or is being read', () => {
    renderCard({ lastRefreshed: null });
    expect(screen.getByText('Catalog not read yet')).toBeInTheDocument();
  });

  it('shows a refresh in progress and disables the button', () => {
    renderCard({ refreshing: true });

    expect(screen.getByText('Reading the catalog now…')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Refresh catalog/ })).toBeDisabled();
  });

  it('shows the last refresh error', () => {
    renderCard({ lastRefreshError: 'HTTP 503' });
    expect(screen.getByText('Last refresh failed: HTTP 503')).toBeInTheDocument();
  });

  it('describes a numbered source without a catalog', () => {
    renderCard({ kind: 'INDEXED', hasCatalog: false, configuredCount: 1 });

    expect(screen.getByText('Numbered')).toBeInTheDocument();
    expect(screen.getByText(/comics configured/)).toHaveTextContent('1 comics configured');
    expect(screen.queryByRole('button', { name: /Refresh catalog/ })).not.toBeInTheDocument();
  });

  it('refreshes the catalog', async () => {
    const { onRefresh } = renderCard();

    await userEvent.click(screen.getByRole('button', { name: /Refresh catalog/ }));

    expect(onRefresh).toHaveBeenCalledWith('gocomics');
  });

  it('asks before backfilling the source', async () => {
    const { onBackfill } = renderCard();

    await userEvent.click(screen.getByRole('button', { name: /Backfill/ }));
    expect(screen.getByText('Backfill GoComics?')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(onBackfill).not.toHaveBeenCalled();

    await userEvent.click(screen.getByRole('button', { name: /Backfill/ }));
    await userEvent.click(screen.getByRole('button', { name: 'Start backfill' }));
    expect(onBackfill).toHaveBeenCalledWith('gocomics');
  });

  it('hides changes from operators', () => {
    renderCard({}, false);

    expect(screen.queryByRole('button', { name: /Refresh catalog/ })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Backfill/ })).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Browse comics/ })).toBeInTheDocument();
  });

  it('shows the settings on demand', async () => {
    renderCard();
    expect(screen.queryByText('Delay between requests')).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Settings' }));

    expect(screen.getByText('Delay between requests')).toBeInTheDocument();
    expect(screen.getByText('8 s – 20 s')).toBeInTheDocument();
    expect(screen.getByText('Up to 4 attempts, backing off 1 min – 10 min')).toBeInTheDocument();
    expect(screen.getByText('730 days')).toBeInTheDocument();
  });

  it('describes unthrottled sources that give up on a 429', async () => {
    renderCard({
      settings: { ...mockSource().settings, throttleMinDelayMs: 0, throttleMaxDelayMs: 0, retryMaxAttempts: 1, backfillMaxPerDay: 50, backfillEnabled: false, backfillPreferColor: false, userAgent: null },
    });

    await userEvent.click(screen.getByRole('button', { name: 'Settings' }));

    expect(screen.getByText('None')).toBeInTheDocument();
    expect(screen.getByText('Give up at once')).toBeInTheDocument();
    expect(screen.getByText('30 strips, 50 a day')).toBeInTheDocument();
    expect(screen.getByText('Default')).toBeInTheDocument();
  });
});
