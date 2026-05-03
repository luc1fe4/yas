import { concatQueryString } from './concatQueryString';

describe('concatQueryString', () => {
  it('should return original URL if array is empty', () => {
    expect(concatQueryString([], 'http://example.com')).toBe('http://example.com');
  });

  it('should concatenate query parameters with ? for first item', () => {
    expect(concatQueryString(['filter=active'], 'http://example.com')).toBe('http://example.com?filter=active');
  });

  it('should concatenate query parameters with & for subsequent items', () => {
    expect(concatQueryString(['filter=active', 'sort=name'], 'http://example.com'))
      .toBe('http://example.com?filter=active&sort=name');
  });

  it('should handle multiple query parameters', () => {
    const url = 'http://example.com/path';
    const params = ['a=1', 'b=2', 'c=3'];
    expect(concatQueryString(params, url)).toBe('http://example.com/path?a=1&b=2&c=3');
  });

  it('should handle single parameter', () => {
    expect(concatQueryString(['key=value'], 'http://api.example.com'))
      .toBe('http://api.example.com?key=value');
  });
});
