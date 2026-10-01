import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { StartDateDialog, describeStart } from './start-date-dialog';
import { renderWithProviders } from '@/test/test-utils';
import { mockSourceComic } from '@/test/source-fixtures';
import type { SourceComic } from '@/types/sources';

function renderDialog(comic: SourceComic, numbered = false, canDetect = true) {
  const onSave = vi.fn();
  const onDetect = vi.fn();
  const onOpenChange = vi.fn();
  renderWithProviders(
    <StartDateDialog comic={comic} numbered={numbered} canDetect={canDetect} open onOpenChange={onOpenChange} onSave={onSave} onDetect={onDetect} />,
  );
  return { onSave, onDetect, onOpenChange };
}

describe('describeStart', () => {
  it('formats dates and strip numbers', () => {
    expect(describeStart({ sourceStartDate: '1985-11-18', firstStripNumber: null }, false)).toBe('Nov 18, 1985');
    expect(describeStart({ sourceStartDate: null, firstStripNumber: 1 }, true)).toBe('#1');
    expect(describeStart({ sourceStartDate: null, firstStripNumber: null }, false)).toBeNull();
    expect(describeStart({ sourceStartDate: null, firstStripNumber: null }, true)).toBeNull();
  });
});

describe('StartDateDialog', () => {
  it('shows the current start and where it came from', () => {
    renderDialog(mockSourceComic({ startSource: 'MANUAL' }));

    expect(screen.getByText('Nov 18, 1985')).toBeInTheDocument();
    expect(screen.getByText('Set by an admin')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Save' })).toBeDisabled();
  });

  it('offers what the source reports when it differs', async () => {
    const { onSave } = renderDialog(mockSourceComic({ sourceStartDate: '1990-01-01', startSource: 'MANUAL', oldest: '1995-01-01' }));

    expect(screen.getByText(/The source says/)).toHaveTextContent('The source says Nov 18, 1985');
    await userEvent.click(screen.getByRole('button', { name: 'Use this' }));

    expect(onSave).toHaveBeenCalledWith({ sourceStartDate: '1985-11-18' });
  });

  it('warns when the new date is after strips already stored', async () => {
    renderDialog(mockSourceComic({ oldest: '2020-01-01' }));

    const input = screen.getByLabelText('First strip date');
    await userEvent.clear(input);
    await userEvent.type(input, '2021-06-01');

    expect(screen.getByText(/are already stored/)).toHaveTextContent('Strips from Jan 1, 2020 are already stored');
  });

  it('edits a numbered comic by strip number', async () => {
    const { onSave } = renderDialog(
      mockSourceComic({ sourceStartDate: null, firstStripNumber: 5, reportedStartDate: null, reportedStartStripNumber: 1, startSource: null }),
      true,
    );

    expect(screen.getByText('#5')).toBeInTheDocument();
    expect(screen.getByText(/The source says/)).toHaveTextContent('#1');
    const input = screen.getByLabelText('First strip number');
    await userEvent.clear(input);
    await userEvent.type(input, '0');
    expect(screen.getByRole('button', { name: 'Save' })).toBeDisabled();

    await userEvent.clear(input);
    await userEvent.type(input, '2');
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    expect(onSave).toHaveBeenCalledWith({ firstStripNumber: 2 });
  });

  it('asks the source, or says it is checking', async () => {
    const { onDetect } = renderDialog(mockSourceComic());
    await userEvent.click(screen.getByRole('button', { name: /Ask the source/ }));
    expect(onDetect).toHaveBeenCalled();
  });

  it('shows a check in progress, and no button when the source cannot tell', () => {
    renderDialog(mockSourceComic({ startPending: true }));
    expect(screen.getByRole('button', { name: /Checking/ })).toBeDisabled();
  });

  it('has no ask button when the source cannot detect starts', () => {
    renderDialog(mockSourceComic({ sourceStartDate: null, startSource: null, reportedStartDate: null }), false, false);

    expect(screen.queryByRole('button', { name: /Ask the source/ })).not.toBeInTheDocument();
    expect(screen.getByText('Unknown')).toBeInTheDocument();
  });

  it('reports closing', async () => {
    const { onOpenChange } = renderDialog(mockSourceComic());
    await userEvent.keyboard('{Escape}');
    expect(onOpenChange).toHaveBeenCalledWith(false);
  });
});
