package com.yas.search.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import java.util.Locale;

class MessagesUtilsTest {

    @Test
    void getMessage_whenCalled_returnMessage() {
        MessageSource messageSource = mock(MessageSource.class);
        MessagesUtils messagesUtils = new MessagesUtils(messageSource);
        
        String code = "test.code";
        Object[] args = new Object[]{"arg1"};
        String expectedMessage = "Test Message arg1";
        
        when(messageSource.getMessage(code, args, Locale.getDefault())).thenReturn(expectedMessage);
        
        String actualMessage = messagesUtils.getMessage(code, args);
        
        assertEquals(expectedMessage, actualMessage);
    }
}
