import { renderHook, act } from '@testing-library/react';
import { useDebounce } from './useDebounce';

describe('useDebounce', () => {
  beforeEach(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('should return initial value immediately', () => {
    const { result } = renderHook(() => useDebounce('initial', 500));
    expect(result.current).toBe('initial');
  });

  it('should update value after delay', () => {
    const { result, rerender } = renderHook(({ value, delay }) => useDebounce(value, delay), {
      initialProps: { value: 'initial', delay: 500 },
    });

    act(() => {
      rerender({ value: 'updated', delay: 500 });
    });

    // Value hasn't changed yet
    expect(result.current).toBe('initial');

    // Fast-forward time
    act(() => {
      jest.advanceTimersByTime(500);
    });

    // Now value should be updated
    expect(result.current).toBe('updated');
  });

  it('should cancel previous timeout when value changes rapidly', () => {
    const { result, rerender } = renderHook(({ value, delay }) => useDebounce(value, delay), {
      initialProps: { value: 'initial', delay: 500 },
    });

    act(() => {
      rerender({ value: 'first', delay: 500 });
    });
    act(() => {
      rerender({ value: 'second', delay: 500 });
    });
    act(() => {
      rerender({ value: 'third', delay: 500 });
    });

    // Fast-forward less than delay
    act(() => {
      jest.advanceTimersByTime(300);
    });

    expect(result.current).toBe('initial');

    // Complete the delay
    act(() => {
      jest.advanceTimersByTime(200);
    });

    // Should have the latest value
    expect(result.current).toBe('third');
  });

  it('should handle undefined value', () => {
    const { result, rerender } = renderHook(({ value, delay }) => useDebounce(value, delay), {
      initialProps: { value: 'initial', delay: 500 },
    });

    act(() => {
      rerender({ value: undefined, delay: 500 });
    });

    act(() => {
      jest.advanceTimersByTime(500);
    });

    expect(result.current).toBeUndefined();
  });

  it('should handle different delays', () => {
    const { result, rerender } = renderHook(({ value, delay }) => useDebounce(value, delay), {
      initialProps: { value: 'initial', delay: 1000 },
    });

    act(() => {
      rerender({ value: 'fast', delay: 200 });
    });

    act(() => {
      jest.advanceTimersByTime(200);
    });

    expect(result.current).toBe('fast');
  });

  it('should clear timeout on unmount', () => {
    const { unmount } = renderHook(() => useDebounce('test', 500));

    unmount();

    // Should not throw after unmount even if timer fires
    act(() => {
      jest.advanceTimersByTime(500);
    });
  });
});
