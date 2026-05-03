import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import Star from './Star';

// Mock react-star-ratings
jest.mock('react-star-ratings', () => {
  return function MockStarRatings({ rating, numberOfStars, starDimension, starSpacing }: any) {
    return (
      <div data-testid="star-ratings" data-rating={rating} data-max={numberOfStars}>
        {Array.from({ length: numberOfStars }).map((_, i) => (
          <span key={i} data-testid={`star-${i + 1}`} data-filled={i < rating}>
            ★
          </span>
        ))}
      </div>
    );
  };
});

describe('Star Component', () => {
  it('renders star ratings with correct rating value', () => {
    render(<Star star={4} />);

    const starsContainer = screen.getByTestId('star-ratings');
    expect(starsContainer).toHaveAttribute('data-rating', '4');
    expect(starsContainer).toHaveAttribute('data-max', '5');
  });

  it('renders 5 stars', () => {
    render(<Star star={3} />);

    for (let i = 1; i <= 5; i++) {
      expect(screen.getByTestId(`star-${i}`)).toBeInTheDocument();
    }
  });

  it('defaults to 0 when star is 0 or undefined', () => {
    const { rerender } = render(<Star star={0} />);

    expect(screen.getByTestId('star-ratings')).toHaveAttribute('data-rating', '0');

    rerender(<Star star={undefined as any} />);
    expect(screen.getByTestId('star-ratings')).toHaveAttribute('data-rating', '0');
  });

  it('handles maximum rating of 5', () => {
    render(<Star star={5} />);

    const starsContainer = screen.getByTestId('star-ratings');
    expect(starsContainer).toHaveAttribute('data-max', '5');
  });

  it('applies correct filled state for each star', () => {
    render(<Star star={3} />);

    // Stars 1-3 should be filled (data-filled=true)
    expect(screen.getByTestId('star-1')).toHaveAttribute('data-filled', 'true');
    expect(screen.getByTestId('star-2')).toHaveAttribute('data-filled', 'true');
    expect(screen.getByTestId('star-3')).toHaveAttribute('data-filled', 'true');

    // Stars 4-5 should be not filled (data-filled=false)
    expect(screen.getByTestId('star-4')).toHaveAttribute('data-filled', 'false');
    expect(screen.getByTestId('star-5')).toHaveAttribute('data-filled', 'false');
  });
});
