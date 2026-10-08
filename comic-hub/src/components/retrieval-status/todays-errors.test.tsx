import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { RetrievalStatusEnum } from '@/generated/graphql';
import { record } from '@/test/retrieval-health';
import { TodaysErrors } from './todays-errors';

describe('TodaysErrors', () => {
  it('says when there were none', () => {
    render(<TodaysErrors errors={[]} />);

    expect(screen.getByText('No errors today.')).toBeInTheDocument();
  });

  it('lists each failure with its comic, status, HTTP code and full message', () => {
    render(
      <TodaysErrors
        errors={[
          {
            recovered: false,
            record: {
              ...record('2026-10-08', RetrievalStatusEnum.NetworkError, { httpStatusCode: 403, errorMessage: 'Forbidden by the source' }),
              comicId: 7,
              comicName: 'Garfield',
              source: 'gocomics',
            },
          },
        ]}
      />,
    );

    expect(screen.getByRole('link', { name: 'Garfield' })).toHaveAttribute('href', '/comics/7/read');
    expect(screen.getByText('NETWORK ERROR')).toBeInTheDocument();
    expect(screen.getByText('HTTP 403')).toBeInTheDocument();
    expect(screen.getByText('Forbidden by the source')).toBeInTheDocument();
    expect(screen.queryByText('since recovered')).not.toBeInTheDocument();
  });

  it('marks a failure a later attempt recovered', () => {
    render(
      <TodaysErrors
        errors={[
          {
            recovered: true,
            record: { ...record('2026-09-01', RetrievalStatusEnum.RateLimited), comicId: null, comicName: 'Old Timer', source: null },
          },
        ]}
      />,
    );

    expect(screen.getByText('since recovered')).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Old Timer' })).not.toBeInTheDocument();
  });

  it('shows the latest five until asked for the rest', async () => {
    const errors = Array.from({ length: 8 }, (_, i) => ({
      recovered: false,
      record: { ...record(`2026-10-0${i + 1}`, RetrievalStatusEnum.NetworkError), id: `e${i}`, comicId: i, comicName: `Comic ${i}`, source: null },
    }));
    render(<TodaysErrors errors={errors} />);

    expect(screen.getAllByRole('listitem')).toHaveLength(5);
    await userEvent.click(screen.getByRole('button', { name: 'Show all 8' }));
    expect(screen.getAllByRole('listitem')).toHaveLength(8);
    await userEvent.click(screen.getByRole('button', { name: 'Show fewer' }));
    expect(screen.getAllByRole('listitem')).toHaveLength(5);
  });

  it('has no toggle for five or fewer', () => {
    render(<TodaysErrors errors={[]} />);

    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });
});
