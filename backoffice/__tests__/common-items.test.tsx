import { render, screen } from '@testing-library/react';
import { useForm } from 'react-hook-form';

import { Input } from '../common/items/Input';
import { OptionSelect } from '../common/items/OptionSelect';

type FormValues = {
  name: string;
  category: string;
};

function InputHarness() {
  const { register } = useForm<FormValues>();

  return (
    <Input<FormValues>
      labelText="Full Name"
      field="name"
      register={register}
      registerOptions={{ required: true }}
      defaultValue="Alice"
    />
  );
}

function OptionSelectHarness() {
  const { register } = useForm<FormValues>();

  return (
    <OptionSelect<FormValues>
      labelText="Category"
      field="category"
      register={register}
      placeholder="Choose category"
      options={[{ id: '1', name: 'Electronics' }]}
      error="Required"
    />
  );
}

describe('Backoffice common items', () => {
  it('renders a required input with default value', () => {
    render(<InputHarness />);

    const input = screen.getByLabelText('Full Name');
    expect(input).toHaveValue('Alice');
    expect(screen.getByText('*', { selector: 'span' })).toBeInTheDocument();
  });

  it('renders select options and error message', () => {
    render(<OptionSelectHarness />);

    expect(screen.getByRole('option', { name: 'Choose category' })).toBeDisabled();
    expect(screen.getByRole('option', { name: 'Electronics' })).toBeInTheDocument();
    expect(screen.getByText('Required')).toBeInTheDocument();
  });
});
