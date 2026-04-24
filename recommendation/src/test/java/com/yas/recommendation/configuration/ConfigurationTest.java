package com.yas.recommendation.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ConfigurationTest {

    @Test
    void testRecommendationConfig() {
        RecommendationConfig config = new RecommendationConfig();
        config.setApiUrl("http://api.yas.local");
        assertEquals("http://api.yas.local", config.getApiUrl());
    }

    @Test
    void testEmbeddingSearchConfiguration() {
        EmbeddingSearchConfiguration config = new EmbeddingSearchConfiguration(10, 0.7);
        assertEquals(10, config.topK());
        assertEquals(0.7, config.similarityThreshold());
    }
}
