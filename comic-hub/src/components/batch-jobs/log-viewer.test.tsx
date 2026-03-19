import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { LogViewer } from './log-viewer';
import { renderWithProviders } from '@/test/test-utils';
import { useGetBatchJobLogQuery } from '@/generated/graphql';

vi.mock('@/generated/graphql', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/generated/graphql')>();
  return {
    ...actual,
    useGetBatchJobLogQuery: vi.fn(),
  };
});

describe('LogViewer', () => {
  const defaultProps = {
    open: true,
    onOpenChange: vi.fn(),
    executionId: 42,
    jobName: 'ComicDownloadJob',
    jobLabel: 'Comic Download',
  };

  it('renders title with job label and execution ID', () => {
    vi.mocked(useGetBatchJobLogQuery).mockReturnValue({ data: null, isLoading: false } as any);
    renderWithProviders(<LogViewer {...defaultProps} />);
    expect(screen.getByText('Comic Download - Execution #42')).toBeInTheDocument();
  });

  it('shows loading skeletons while fetching', () => {
    vi.mocked(useGetBatchJobLogQuery).mockReturnValue({ data: null, isLoading: true } as any);
    renderWithProviders(<LogViewer {...defaultProps} />);
    expect(screen.queryByText('No logs available for this execution')).not.toBeInTheDocument();
  });

  it('shows empty state when no logs available', () => {
    vi.mocked(useGetBatchJobLogQuery).mockReturnValue({ data: { batchJobLog: null }, isLoading: false } as any);
    renderWithProviders(<LogViewer {...defaultProps} />);
    expect(screen.getByText('No logs available for this execution')).toBeInTheDocument();
  });

  it('renders log content in pre-formatted block', () => {
    vi.mocked(useGetBatchJobLogQuery).mockReturnValue({
      data: { batchJobLog: 'INFO Starting job...\nINFO Job completed.' },
      isLoading: false,
    } as any);
    renderWithProviders(<LogViewer {...defaultProps} />);
    expect(screen.getByText(/INFO Starting job/)).toBeInTheDocument();
    expect(screen.getByText(/INFO Job completed/)).toBeInTheDocument();
  });

  it('only fetches when open', () => {
    vi.mocked(useGetBatchJobLogQuery).mockReturnValue({ data: null, isLoading: false } as any);
    renderWithProviders(<LogViewer {...defaultProps} open={false} />);
    expect(useGetBatchJobLogQuery).toHaveBeenCalledWith(
      { executionId: 42, jobName: 'ComicDownloadJob' },
      { enabled: false },
    );
  });

  it('shows search bar when log content is present', () => {
    vi.mocked(useGetBatchJobLogQuery).mockReturnValue({
      data: { batchJobLog: 'some log content' },
      isLoading: false,
    } as any);
    renderWithProviders(<LogViewer {...defaultProps} />);
    expect(screen.getByLabelText('Search logs')).toBeInTheDocument();
  });

  it('does not show search bar when no log content', () => {
    vi.mocked(useGetBatchJobLogQuery).mockReturnValue({ data: { batchJobLog: null }, isLoading: false } as any);
    renderWithProviders(<LogViewer {...defaultProps} />);
    expect(screen.queryByLabelText('Search logs')).not.toBeInTheDocument();
  });

  it('highlights matches and shows match count', async () => {
    vi.mocked(useGetBatchJobLogQuery).mockReturnValue({
      data: { batchJobLog: 'ERROR first\nINFO second\nERROR third' },
      isLoading: false,
    } as any);
    renderWithProviders(<LogViewer {...defaultProps} />);
    await userEvent.type(screen.getByLabelText('Search logs'), 'ERROR');

    const marks = screen.getAllByText('ERROR');
    expect(marks).toHaveLength(2);
    expect(marks[0].tagName).toBe('MARK');
    expect(screen.getByTestId('match-count')).toHaveTextContent('1 of 2');
  });

  it('shows 0 results for non-matching search', async () => {
    vi.mocked(useGetBatchJobLogQuery).mockReturnValue({
      data: { batchJobLog: 'INFO some log line' },
      isLoading: false,
    } as any);
    renderWithProviders(<LogViewer {...defaultProps} />);
    await userEvent.type(screen.getByLabelText('Search logs'), 'NOTFOUND');

    expect(screen.getByTestId('match-count')).toHaveTextContent('0 results');
  });

  it('navigates to next match when Next button is clicked', async () => {
    vi.mocked(useGetBatchJobLogQuery).mockReturnValue({
      data: { batchJobLog: 'ERROR first\nERROR second\nERROR third' },
      isLoading: false,
    } as any);
    renderWithProviders(<LogViewer {...defaultProps} />);
    await userEvent.type(screen.getByLabelText('Search logs'), 'ERROR');

    expect(screen.getByTestId('match-count')).toHaveTextContent('1 of 3');

    await userEvent.click(screen.getByLabelText('Next match'));
    expect(screen.getByTestId('match-count')).toHaveTextContent('2 of 3');

    await userEvent.click(screen.getByLabelText('Next match'));
    expect(screen.getByTestId('match-count')).toHaveTextContent('3 of 3');

    // Wraps around
    await userEvent.click(screen.getByLabelText('Next match'));
    expect(screen.getByTestId('match-count')).toHaveTextContent('1 of 3');
  });

  it('navigates to previous match when Prev button is clicked', async () => {
    vi.mocked(useGetBatchJobLogQuery).mockReturnValue({
      data: { batchJobLog: 'ERROR first\nERROR second' },
      isLoading: false,
    } as any);
    renderWithProviders(<LogViewer {...defaultProps} />);
    await userEvent.type(screen.getByLabelText('Search logs'), 'ERROR');

    expect(screen.getByTestId('match-count')).toHaveTextContent('1 of 2');

    // Wraps to last
    await userEvent.click(screen.getByLabelText('Previous match'));
    expect(screen.getByTestId('match-count')).toHaveTextContent('2 of 2');
  });

  it('disables prev/next buttons when no matches', async () => {
    vi.mocked(useGetBatchJobLogQuery).mockReturnValue({
      data: { batchJobLog: 'INFO nothing here' },
      isLoading: false,
    } as any);
    renderWithProviders(<LogViewer {...defaultProps} />);
    await userEvent.type(screen.getByLabelText('Search logs'), 'MISSING');

    expect(screen.getByLabelText('Previous match')).toBeDisabled();
    expect(screen.getByLabelText('Next match')).toBeDisabled();
  });

  it('focuses search input on Ctrl+F keydown', async () => {
    vi.mocked(useGetBatchJobLogQuery).mockReturnValue({
      data: { batchJobLog: 'INFO some log content' },
      isLoading: false,
    } as any);
    renderWithProviders(<LogViewer {...defaultProps} />);
    const searchInput = screen.getByLabelText('Search logs');
    const dialog = screen.getByRole('dialog');

    await userEvent.click(dialog);
    await userEvent.keyboard('{Control>}f{/Control}');

    expect(document.activeElement).toBe(searchInput);
  });

  it('navigates to next match on Enter in search input', async () => {
    vi.mocked(useGetBatchJobLogQuery).mockReturnValue({
      data: { batchJobLog: 'ERROR first\nERROR second\nERROR third' },
      isLoading: false,
    } as any);
    renderWithProviders(<LogViewer {...defaultProps} />);
    const searchInput = screen.getByLabelText('Search logs');
    await userEvent.type(searchInput, 'ERROR');
    expect(screen.getByTestId('match-count')).toHaveTextContent('1 of 3');

    await userEvent.keyboard('{Enter}');
    expect(screen.getByTestId('match-count')).toHaveTextContent('2 of 3');
  });

  it('navigates to previous match on Shift+Enter in search input', async () => {
    vi.mocked(useGetBatchJobLogQuery).mockReturnValue({
      data: { batchJobLog: 'ERROR first\nERROR second' },
      isLoading: false,
    } as any);
    renderWithProviders(<LogViewer {...defaultProps} />);
    const searchInput = screen.getByLabelText('Search logs');
    await userEvent.type(searchInput, 'ERROR');
    expect(screen.getByTestId('match-count')).toHaveTextContent('1 of 2');

    await userEvent.keyboard('{Shift>}{Enter}{/Shift}');
    expect(screen.getByTestId('match-count')).toHaveTextContent('2 of 2');
  });

  it('performs case-insensitive search', async () => {
    vi.mocked(useGetBatchJobLogQuery).mockReturnValue({
      data: { batchJobLog: 'Error first\nerror second\nERROR third' },
      isLoading: false,
    } as any);
    renderWithProviders(<LogViewer {...defaultProps} />);
    await userEvent.type(screen.getByLabelText('Search logs'), 'error');

    expect(screen.getByTestId('match-count')).toHaveTextContent('1 of 3');
  });
});
