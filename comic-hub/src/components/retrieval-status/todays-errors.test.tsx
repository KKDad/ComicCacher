import { render, screen } from '@testing-library/react';
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
});
