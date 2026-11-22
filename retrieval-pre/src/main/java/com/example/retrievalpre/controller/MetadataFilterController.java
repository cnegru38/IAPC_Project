package com.example.retrievalpre.controller;

import com.example.retrievalpre.filter.InvalidMetadataFilterException;
import com.example.retrievalpre.filter.MetadataFilterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Diagnostic endpoint; production retrieval should call the same parser and pass the Filter.Expression to SearchRequest. */
@RestController
public class MetadataFilterController {
    private static final Logger LOG = LoggerFactory.getLogger(MetadataFilterController.class);
    private final MetadataFilterService filters;
    public MetadataFilterController(MetadataFilterService filters) { this.filters = filters; }

    @GetMapping("/api/retrieval-pre/filter/validate")
    public Map<String, Object> validate(@RequestParam(required = false) String filter) {
        var expression = filters.parse(filter);
        return Map.of("filtered", expression != null, "expression", expression == null ? "none" : expression.toString());
    }

    @ExceptionHandler(InvalidMetadataFilterException.class)
    public ResponseEntity<Map<String, Object>> invalid(InvalidMetadataFilterException ex) {
        LOG.warn("Rejected metadata filter: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("status", 400, "error", "Invalid metadata filter", "message", ex.getMessage()));
    }
}
