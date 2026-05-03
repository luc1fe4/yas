import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import { useForm } from 'react-hook-form';
import { Input, Select, CheckBox } from './Input';

// Helper to render form components with react-hook-form context
const renderWithForm = <T extends Record<string, any>>(
  ui: (props: { register: ReturnType<typeof useForm>['register']; error?: string }) => React.ReactElement,
  defaultValues?: T
) => {
  const FormWrapper = () => {
    const { register, handleSubmit, watch, setValue, formState: { errors } } = useForm<T>({ defaultValues: defaultValues as T });
    return (
      <form onSubmit={handleSubmit(jest.fn())}>
        {ui({ register, error: errors.name?.message as string })}
        <button type="submit">Submit</button>
      </form>
    );
  };
  return render(<FormWrapper />);
};

describe('Input Component', () => {
  it('renders input element with label', () => {
    renderWithForm(
      ({ register }) => (
        <Input<{ name: string }>
          labelText="Name"
          field="name"
          register={register}
        />
      )
    );

    expect(screen.getByLabelText(/Name/)).toBeInTheDocument();
    expect(screen.getByRole('textbox')).toBeInTheDocument();
  });

  it('shows required asterisk when field is required', () => {
    renderWithForm(
      ({ register }) => (
        <Input<{ name: string }>
          labelText="Name"
          field="name"
          register={register}
          registerOptions={{ required: true }}
        />
      )
    );

    expect(screen.getByText('*')).toBeInTheDocument();
  });

  it('displays error message when error prop is provided', () => {
    renderWithForm(
      ({ register }) => (
        <Input<{ name: string }>
          labelText="Name"
          field="name"
          register={register}
          error="Name is required"
        />
      )
    );

    expect(screen.getByText('Name is required')).toBeInTheDocument();
  });

  it('applies error class when error exists', () => {
    renderWithForm(
      ({ register }) => (
        <Input<{ name: string }>
          labelText="Name"
          field="name"
          register={register}
          error="Error"
        />
      )
    );

    const input = screen.getByRole('textbox');
    expect(input).toHaveClass('border-danger');
  });

  it('handles disabled state', () => {
    renderWithForm(
      ({ register }) => (
        <Input<{ name: string }>
          labelText="Name"
          field="name"
          register={register}
          disabled
        />
      )
    );

    expect(screen.getByRole('textbox')).toBeDisabled();
  });

  it('accepts different input types', () => {
    renderWithForm(
      ({ register }) => (
        <Input<{ email: string }>
          labelText="Email"
          field="email"
          register={register}
          type="email"
        />
      )
    );

    expect(screen.getByRole('textbox')).toHaveAttribute('type', 'email');
  });
});

describe('Select Component', () => {
  it('renders select element with options', () => {
    const options = [
      { value: 'vietnam', label: 'Vietnam' },
      { value: 'usa', label: 'USA' },
    ];

    renderWithForm(
      ({ register }) => (
        <Select<{ country: string }>
          labelText="Country"
          field="country"
          register={register}
          options={options}
          placeholder="Select country"
        />
      )
    );

    expect(screen.getByLabelText(/Country/)).toBeInTheDocument();
    expect(screen.getByRole('combobox')).toBeInTheDocument();
    expect(screen.getByText('Vietnam')).toBeInTheDocument();
    expect(screen.getByText('USA')).toBeInTheDocument();
  });

  it('shows placeholder option', () => {
    const options = [{ value: 'vietnam', label: 'Vietnam' }];

    renderWithForm(
      ({ register }) => (
        <Select<{ country: string }>
          labelText="Country"
          field="country"
          register={register}
          options={options}
          placeholder="Choose a country"
        />
      )
    );

    expect(screen.getByText('Choose a country')).toBeInTheDocument();
  });

  it('can change selection', () => {
    const options = [
      { value: 'vietnam', label: 'Vietnam' },
      { value: 'usa', label: 'USA' },
    ];

    renderWithForm(
      ({ register }) => (
        <Select<{ country: string }>
          labelText="Country"
          field="country"
          register={register}
          options={options}
        />
      )
    );

    const select = screen.getByRole('combobox');
    fireEvent.change(select, { target: { value: 'usa' } });

    expect(select).toHaveValue('usa');
  });

  it('can handle multiple selection', () => {
    const options = [
      { value: 'vietnam', label: 'Vietnam' },
      { value: 'usa', label: 'USA' },
    ];

    renderWithForm(
      ({ register }) => (
        <Select<{ countries: string[] }>
          labelText="Countries"
          field="countries"
          register={register}
          options={options}
          isMultiple
        />
      )
    );

    expect(screen.getByRole('combobox')).toHaveAttribute('multiple');
  });

  it('shows error message on select', () => {
    const options = [{ value: 'vietnam', label: 'Vietnam' }];

    renderWithForm(
      ({ register }) => (
        <Select<{ country: string }>
          labelText="Country"
          field="country"
          register={register}
          options={options}
          error="Country is required"
        />
      )
    );

    expect(screen.getByText('Country is required')).toBeInTheDocument();
  });
});

describe('CheckBox Component', () => {
  it('renders checkbox with label', () => {
    renderWithForm(
      ({ register }) => (
        <CheckBox<{ agree: boolean }>
          labelText="I agree to terms"
          field="agree"
          register={register}
        />
      )
    );

    expect(screen.getByLabelText(/I agree to terms/)).toBeInTheDocument();
    expect(screen.getByRole('checkbox')).toBeInTheDocument();
  });

  it('handles default checked state', () => {
    renderWithForm(
      ({ register }) => (
        <CheckBox<{ agree: boolean }>
          labelText="I agree"
          field="agree"
          register={register}
          defaultChecked={true}
        />
      )
    );

    expect(screen.getByRole('checkbox')).toBeChecked();
  });

  it('shows error message', () => {
    renderWithForm(
      ({ register }) => (
        <CheckBox<{ agree: boolean }>
          labelText="I agree"
          field="agree"
          register={register}
          error="You must agree"
        />
      )
    );

    expect(screen.getByText('You must agree')).toBeInTheDocument();
  });
});
