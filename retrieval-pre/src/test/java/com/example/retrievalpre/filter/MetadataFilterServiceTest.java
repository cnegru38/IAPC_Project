package com.example.retrievalpre.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetadataFilterServiceTest {
    private MetadataFilterService service;

    @BeforeEach void setup() { service = new MetadataFilterService(new ObjectMapper()); }

    @Test void nullOrBlankFilterUsesDefaultFlow() {
        assertNull(service.parse(null));
        assertNull(service.parse(""));
        assertNull(service.parse("   "));
    }

    @Test void validDocumentSourceFilter() {
        var expression = service.parse(""" 
                {"field":"source","op":"eq","value":"pdf3.pdf"}
                """);
        assertNotNull(expression);
        assertTrue(expression.toString().contains("source"));
    }

    @Test void validAndFilter() {
        var expression = service.parse("""
                {"logic":"AND","filters":[
                   {"field":"source","op":"eq","value":"pdf3.pdf"},
                   {"field":"page","op":"eq","value":1}]}
                """);
        assertNotNull(expression);
    }

    @Test void validOrFilter() {
        var expression = service.parse("""
                {"logic":"OR","filters":[
                   {"field":"source","op":"eq","value":"pdf1.pdf"},
                   {"field":"source","op":"eq","value":"pdf3.pdf"}]}
                """);
        assertNotNull(expression);
    }

    @Test void nestedAndOrFilter() {
        var expression = service.parse("""
                {"logic":"AND","filters":[
                    {"field":"page","op":"gte","value":1},
                    {"logic":"OR","filters":[
                        {"field":"source","op":"eq","value":"pdf1.pdf"},
                        {"field":"source","op":"eq","value":"pdf3.pdf"}]}]}
                """);
        assertNotNull(expression);
    }

    @Test void unknownFieldRejectedWithWhitelist() {
        var ex = assertThrows(InvalidMetadataFilterException.class,
                () -> service.parse("{\"field\":\"password\",\"op\":\"eq\",\"value\":\"x\"}"));
        assertTrue(ex.getMessage().contains("Valid fields"));
        assertTrue(ex.getMessage().contains("doc_id"));
    }

    @Test void unsupportedOperatorRejected() {
        assertThrows(InvalidMetadataFilterException.class,
                () -> service.parse("{\"field\":\"source\",\"op\":\"contains\",\"value\":\"pdf\"}"));
    }

    @Test void incorrectNumericTypeRejected() {
        assertThrows(InvalidMetadataFilterException.class,
                () -> service.parse("{\"field\":\"page\",\"op\":\"eq\",\"value\":\"first\"}"));
    }

    @Test void invalidJsonRejected() {
        assertThrows(InvalidMetadataFilterException.class, () -> service.parse("{not json}"));
    }

    @Test void unknownPropertyRejected() {
        assertThrows(InvalidMetadataFilterException.class,
                () -> service.parse("{\"field\":\"source\",\"op\":\"eq\",\"value\":\"x\",\"unsafe\":true}"));
    }

    @Test void excessiveDepthRejected() {
        String nested = "{\"field\":\"page\",\"op\":\"eq\",\"value\":1}";
        for (int i = 0; i < 7; i++) nested = "{\"logic\":\"AND\",\"filters\":[" + nested + "," + "{\"field\":\"page\",\"op\":\"eq\",\"value\":1}" + "]}";
        String filter = nested;
        assertThrows(InvalidMetadataFilterException.class, () -> service.parse(filter));
    }
}
