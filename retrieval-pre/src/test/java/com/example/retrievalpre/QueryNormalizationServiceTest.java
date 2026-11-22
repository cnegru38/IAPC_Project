package com.example.retrievalpre;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class QueryNormalizationServiceTest {
    private final QueryNormalizationService service = new QueryNormalizationService();

    @ParameterizedTest(name = "{index}: [{0}] => [{1}]")
    @CsvSource(value = {
            "Could you please explain RAG???|explain RAG",
            "Would you please describe pgvector?|describe pgvector",
            "Can you please list the rules??|list the rules",
            "Please explain embeddings?|explain embeddings",
            "Kindly show examples???|show examples",
            "te rog explică RAG?|explică RAG",
            "te rog frumos să explici RAG?|explici RAG",
            "vă rog explicați GDPR?|explicați GDPR",
            "va rog sa explici RAG??|explici RAG",
            "What is RAG?|What is RAG",
            "What is RAG???|What is RAG",
            "  What is RAG?  |What is RAG",
            "How   does   pgvector work??|How does pgvector work",
            "RAG|RAG",
            "''|''",
            "???|''",
            "Please?|Please",
            "Please explain why? Because?|explain why? Because",
            "Could you please explain RAG？|explain RAG",
            "please do not remove interior ? marks??|do not remove interior ? marks",
            "Please please explain RAG??|explain RAG"
    }, delimiter = '|')
    void normalizationCases(String input, String expected) {
        assertEquals(expected, service.normalize(input));
    }

    @Test
    void normalizationIsIdempotent() {
        String once = service.normalize("  Could you please  explain RAG??? ");
        assertEquals(once, service.normalize(once));
    }

    @Test
    void rejectsNullQuery() {
        assertThrows(IllegalArgumentException.class, () -> service.normalize(null));
    }

    @Test
    void rejectsExcessivelyLongQuery() {
        assertThrows(IllegalArgumentException.class, () -> service.normalize("a".repeat(4097)));
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void adversarialInputFinishesWithoutRegexBacktracking() {
        String adversarial = "Could you please " + "a".repeat(4000) + "????";
        assertTrue(service.normalize(adversarial).startsWith("a"));
    }
}
