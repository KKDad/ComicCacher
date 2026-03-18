import { screen } from '@testing-library/react';
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
    // Skeletons render as divs with specific classes — just verify no log content or empty state
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
      { enabled: false }
    );
  });
});
