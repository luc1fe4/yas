import { formatPrice } from './formatPrice';

describe('formatPrice', () => {
  it('should format price as USD currency', () => {
    expect(formatPrice(1000)).toBe('$1,000.00');
    expect(formatPrice(0)).toBe('$0.00');
    expect(formatPrice(1234567.89)).toBe('$1,234,567.89');
  });

  it('should handle negative prices', () => {
    expect(formatPrice(-100)).toBe('-$100.00');
  });

  it('should format decimals correctly', () => {
    expect(formatPrice(99.9)).toBe('$99.90');
    expect(formatPrice(99.99)).toBe('$99.99');
    expect(formatPrice(99.999)).toBe('$100.00');
  });
});
