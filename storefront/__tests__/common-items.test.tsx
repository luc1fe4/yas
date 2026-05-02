import { render, screen } from '@testing-library/react';
import { useForm } from 'react-hook-form';

import { Input } from '../common/items/Input';

type FormValues = {
  email: string;
};

function InputHarness({ error, placeholder }: { error?: string; placeholder?: string }) {
  const { register } = useForm<FormValues>();

  return (
    <Input<FormValues>
      labelText="Email"
      field="email"
      register={register}
      registerOptions={{ required: true }}
      placeholder={placeholder}
      error={error}
    />
  );
}

describe('Storefront common items', () => {
  it('renders input with placeholder and required indicator', () => {
    render(<InputHarness placeholder="Enter email" />);

    const input = screen.getByLabelText(/email/i);
    expect(input).toHaveAttribute('placeholder', 'Enter email');
    expect(screen.getByText('*', { selector: 'span' })).toBeInTheDocument();
  });

  it('shows error text when provided', () => {
    render(<InputHarness error="Required" />);

    expect(screen.getByText('Required')).toBeInTheDocument();
  });
});
