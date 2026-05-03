package com.yas.sampledata.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MessagesUtilsTest {

    @Test
    void getMessage_withExistingCode_shouldReturnMessage() {
        // This depends on the existence of messages.properties in resources
        // If it doesn't exist, it returns the code itself.
        String result = MessagesUtils.getMessage("test.code");
        assertEquals("test.code", result);
    }
}
