import { toastSuccess, toastError } from './ToastService';
import { toast } from 'react-toastify';

// Mock react-toastify
jest.mock('react-toastify', () => ({
  toast: {
    success: jest.fn(),
    error: jest.fn(),
  },
}));

describe('ToastService', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('toastSuccess', () => {
    it('should call toast.success with message and default options', () => {
      toastSuccess('Operation successful');

      expect(toast.success).toHaveBeenCalledWith('Operation successful', {
        position: 'top-right',
        autoClose: 3000,
        closeOnClick: true,
        pauseOnHover: false,
        theme: 'colored',
      });
    });

    it('should call toast.success with custom options', () => {
      const customOptions = {
        position: 'bottom-left' as const,
        autoClose: 5000,
      };

      toastSuccess('Saved', customOptions);

      expect(toast.success).toHaveBeenCalledWith('Saved', customOptions);
    });
  });

  describe('toastError', () => {
    it('should call toast.error with message and default options', () => {
      toastError('An error occurred');

      expect(toast.error).toHaveBeenCalledWith('An error occurred', {
        position: 'top-right',
        autoClose: 3000,
        closeOnClick: true,
        pauseOnHover: false,
        theme: 'colored',
      });
    });

    it('should call toast.error with custom options', () => {
      const customOptions = {
        position: 'top-center' as const,
        autoClose: 10000,
      };

      toastError('Failed', customOptions);

      expect(toast.error).toHaveBeenCalledWith('Failed', customOptions);
    });
  });
});
