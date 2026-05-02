const nextJest = require('next/jest');

const createJestConfig = nextJest({ dir: './' });

const customJestConfig = {
  testEnvironment: 'jsdom',
  setupFilesAfterEnv: ['<rootDir>/jest.setup.js'],
  moduleNameMapper: {
    '^@commonServices/(.*)$': '<rootDir>/common/services/$1',
    '^@commonItems/(.*)$': '<rootDir>/common/items/$1',
    '^@locationComponents/(.*)$': '<rootDir>/modules/location/components/$1',
    '^@locationModels/(.*)$': '<rootDir>/modules/location/models/$1',
    '^@locationServices/(.*)$': '<rootDir>/modules/location/services/$1',
    '^@taxServices/(.*)$': '<rootDir>/modules/tax/services/$1',
    '^@taxComponents/(.*)$': '<rootDir>/modules/tax/components/$1',
    '^@taxModels/(.*)$': '<rootDir>/modules/tax/models/$1',
    '^@constants/(.*)$': '<rootDir>/constants/$1',
    '^@catalogModels/(.*)$': '<rootDir>/modules/catalog/models/$1',
    '^@catalogServices/(.*)$': '<rootDir>/modules/catalog/services/$1',
    '^@catalogComponents/(.*)$': '<rootDir>/modules/catalog/components/$1',
    '^@inventoryServices/(.*)$': '<rootDir>/modules/inventory/services/$1',
    '^@inventoryModels/(.*)$': '<rootDir>/modules/inventory/models/$1',
    '^@inventoryComponents/(.*)$': '<rootDir>/modules/inventory/components/$1',
    '^@webhookComponents/(.*)$': '<rootDir>/modules/webhook/components/$1',
    '^@webhookServices/(.*)$': '<rootDir>/modules/webhook/services/$1',
    '^@webhookModels/(.*)$': '<rootDir>/modules/webhook/models/$1',
    '^.+\\.module\\.(css|sass|scss)$': 'identity-obj-proxy',
    '^.+\\.(css|sass|scss)$': '<rootDir>/__mocks__/styleMock.js',
    '^.+\\.(png|jpg|jpeg|gif|webp|svg)$': '<rootDir>/__mocks__/fileMock.js',
  },
  testPathIgnorePatterns: ['<rootDir>/.next/', '<rootDir>/node_modules/'],
};

module.exports = createJestConfig(customJestConfig);
