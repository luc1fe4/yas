import { formatPriceVND, formatPriceUSD } from './formatPrice';

describe('formatPriceVND', () => {
  it('should format price as VND currency', () => {
    expect(formatPriceVND(1000)).toBe('1,000 ₫');
    expect(formatPriceVND(0)).toBe('0 ₫');
    expect(formatPriceVND(1234567)).toBe('1,234,567 ₫');
  });

  it('should handle large numbers', () => {
    expect(formatPriceVND(10000000)).toBe('10,000,000 ₫');
  });

  it('should handle decimals (rounded)', () => {
    expect(formatPriceVND(99.99)).toContain('₫');
  });
});

describe('formatPriceUSD', () => {
  it('should format price as USD currency', () => {
    expect(formatPriceUSD(1000)).toBe('$1,000.00');
    expect(formatPriceUSD(0)).toBe('$0.00');
    expect(formatPriceUSD(1234567.89)).toBe('$1,234,567.89');
  });

  it('should handle negative prices', () => {
    expect(formatPriceUSD(-100)).toBe('-$100.00');
  });

  it('should format decimals correctly', () => {
    expect(formatPriceUSD(99.9)).toBe('$99.90');
    expect(formatPriceUSD(99.99)).toBe('$99.99');
  });
});
