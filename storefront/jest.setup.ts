// Jest setup file
// Mock window.matchMedia
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: jest.fn().mockImplementation(query => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: jest.fn(),
    removeListener: jest.fn(),
    addEventListener: jest.fn(),
    removeEventListener: jest.fn(),
    dispatchEvent: jest.fn(),
  })),
});

// Mock ResizeObserver
global.ResizeObserver = jest.fn().mockImplementation(() => ({
  observe: jest.fn(),
  unobserve: jest.fn(),
  disconnect: jest.fn(),
}));

// Mock window.alert
window.alert = jest.fn();

// Mock console.error and console.warn to fail tests on errors
const originalError = console.error;
const originalWarn = console.warn;

beforeEach(() => {
  console.error = jest.fn((...args) => {
    originalError.call(console, ...args);
  });
  console.warn = jest.fn((...args) => {
    originalWarn.call(console, ...args);
  });
});

afterEach(() => {
  console.error = originalError;
  console.warn = originalWarn;
});
