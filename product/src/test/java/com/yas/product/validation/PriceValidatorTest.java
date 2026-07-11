package com.yas.product.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class PriceValidatorTest {

    private PriceValidator priceValidator;

    @Mock
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        priceValidator = new PriceValidator();
    }

    @Test
    void isValid_whenPriceIsPositive_shouldReturnTrue() {
        assertThat(priceValidator.isValid(99.99, context)).isTrue();
    }

    @Test
    void isValid_whenPriceIsZero_shouldReturnTrue() {
        assertThat(priceValidator.isValid(0.0, context)).isTrue();
    }

    @Test
    void isValid_whenPriceIsNegative_shouldReturnFalse() {
        assertThat(priceValidator.isValid(-1.0, context)).isFalse();
    }

    @Test
    void isValid_whenPriceIsNegativeLarge_shouldReturnFalse() {
        assertThat(priceValidator.isValid(-999.99, context)).isFalse();
    }

    @Test
    void isValid_whenPriceIsNull_shouldThrowNullPointerException() {
        // Logic hiện tại: `productPrice >= 0` sẽ auto-unbox null Double → NPE
        assertThrows(NullPointerException.class, () -> priceValidator.isValid(null, context));
    }

    @Test
    void initialize_shouldNotThrowException() {
        // Đảm bảo initialize() không throw exception
        priceValidator.initialize(null);
    }
}
