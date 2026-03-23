import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ParameterControl } from './parameter-control';
import { renderWithProviders } from '@/test/test-utils';
import { BatchJobParameterType } from '@/generated/graphql';
import type { BatchJobParameter } from '@/generated/graphql';

function createParam(overrides: Partial<BatchJobParameter> & Pick<BatchJobParameter, 'type'>): BatchJobParameter {
  return {
    __typename: 'BatchJobParameter',
    name: 'testParam',
    label: 'Test Parameter',
    required: false,
    defaultValue: null,
    options: null,
    ...overrides,
  };
}

describe('ParameterControl', () => {
  describe('ENUM type', () => {
    const enumParam = createParam({
      type: BatchJobParameterType.Enum,
      name: 'source',
      label: 'Source Filter',
      defaultValue: 'ALL',
      options: [
        { __typename: 'BatchJobParameterOption', value: 'ALL', label: 'All Sources' },
        { __typename: 'BatchJobParameterOption', value: 'gocomics', label: 'GoComics' },
        { __typename: 'BatchJobParameterOption', value: 'freefall', label: 'Freefall' },
      ],
    });

    it('renders label and select trigger', () => {
      renderWithProviders(
        <ParameterControl param={enumParam} value="ALL" onChange={vi.fn()} />,
      );
      expect(screen.getByText('Source Filter')).toBeInTheDocument();
      expect(screen.getByRole('combobox')).toBeInTheDocument();
    });

    it('displays current value in select trigger', () => {
      renderWithProviders(
        <ParameterControl param={enumParam} value="ALL" onChange={vi.fn()} />,
      );
      expect(screen.getByRole('combobox')).toHaveTextContent('All Sources');
    });
  });

  describe('BOOLEAN type', () => {
    const boolParam = createParam({
      type: BatchJobParameterType.Boolean,
      name: 'dryRun',
      label: 'Dry Run',
      defaultValue: 'false',
    });

    it('renders label and switch', () => {
      renderWithProviders(
        <ParameterControl param={boolParam} value="false" onChange={vi.fn()} />,
      );
      expect(screen.getByText('Dry Run')).toBeInTheDocument();
      expect(screen.getByRole('switch')).toBeInTheDocument();
    });

    it('renders switch as checked when value is true', () => {
      renderWithProviders(
        <ParameterControl param={boolParam} value="true" onChange={vi.fn()} />,
      );
      expect(screen.getByRole('switch')).toBeChecked();
    });

    it('renders switch as unchecked when value is false', () => {
      renderWithProviders(
        <ParameterControl param={boolParam} value="false" onChange={vi.fn()} />,
      );
      expect(screen.getByRole('switch')).not.toBeChecked();
    });

    it('calls onChange with string boolean when toggled', async () => {
      const onChange = vi.fn();
      renderWithProviders(
        <ParameterControl param={boolParam} value="false" onChange={onChange} />,
      );

      await userEvent.click(screen.getByRole('switch'));
      expect(onChange).toHaveBeenCalledWith('true');
    });
  });

  describe('INTEGER type', () => {
    const intParam = createParam({
      type: BatchJobParameterType.Integer,
      name: 'batchSize',
      label: 'Batch Size',
      defaultValue: '100',
    });

    it('renders label and number input', () => {
      renderWithProviders(
        <ParameterControl param={intParam} value="100" onChange={vi.fn()} />,
      );
      expect(screen.getByText('Batch Size')).toBeInTheDocument();
      expect(screen.getByRole('spinbutton')).toHaveValue(100);
    });

    it('calls onChange when value is typed', async () => {
      const onChange = vi.fn();
      renderWithProviders(
        <ParameterControl param={intParam} value="" onChange={onChange} />,
      );

      await userEvent.type(screen.getByRole('spinbutton'), '50');
      expect(onChange).toHaveBeenCalled();
    });
  });

  describe('STRING type', () => {
    const stringParam = createParam({
      type: BatchJobParameterType.String,
      name: 'comicName',
      label: 'Comic Name',
      defaultValue: '',
    });

    it('renders label and text input', () => {
      renderWithProviders(
        <ParameterControl param={stringParam} value="test" onChange={vi.fn()} />,
      );
      expect(screen.getByText('Comic Name')).toBeInTheDocument();
      expect(screen.getByRole('textbox')).toHaveValue('test');
    });

    it('calls onChange when text is typed', async () => {
      const onChange = vi.fn();
      renderWithProviders(
        <ParameterControl param={stringParam} value="" onChange={onChange} />,
      );

      await userEvent.type(screen.getByRole('textbox'), 'hello');
      expect(onChange).toHaveBeenCalledTimes(5);
    });
  });
});
