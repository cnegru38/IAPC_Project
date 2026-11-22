package com.example.retrievalpre;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/** Normalizes user queries before retrieval; does not modify the original input. */
@Service
public class QueryNormalizationService {
    private static final Logger log = LoggerFactory.getLogger(QueryNormalizationService.class);
    private static final int MAX_QUERY_LENGTH = 4096;

    // All regexes are compiled once, have no nested quantifiers, and run in a fixed order.
    private static final Pattern LOG_LINE_BREAKS = Pattern.compile("[\\r\\n]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern TRAILING_QUESTIONS = Pattern.compile("[?？][?？\\s]*$");
    private static final List<Pattern> POLITE_PREFIXES = List.of(
            Pattern.compile("^(?:could|would) you please\\s+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^can you please\\s+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^please\\s+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^kindly\\s+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^te rog(?: frumos)?(?: să| sa)?\\s+", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
            Pattern.compile("^vă rog(?: frumos)?(?: să| sa)?\\s+", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
            Pattern.compile("^va rog(?: frumos)?(?: să| sa)?\\s+", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
    );

    public String normalize(String originalQuery) {
        if (originalQuery == null) {
            throw new IllegalArgumentException("Query must not be null");
        }
        if (originalQuery.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException("Query exceeds " + MAX_QUERY_LENGTH + " characters");
        }
        // Preserve the original query in application logs, as required by the story.
        // Avoid logging credentials or confidential text in production without access controls.
        log.info("Query normalization received originalQuery={}", LOG_LINE_BREAKS.matcher(originalQuery).replaceAll(" "));

        String result = WHITESPACE.matcher(originalQuery.trim()).replaceAll(" ");
        // Remove consecutive polite prefixes to preserve idempotence even for repeated forms.
        boolean changed;
        do {
            changed = false;
            for (Pattern prefix : POLITE_PREFIXES) {
                String stripped = prefix.matcher(result).replaceFirst("");
                if (!stripped.equals(result)) {
                    result = stripped;
                    changed = true;
                    break;
                }
            }
        } while (changed);
        result = TRAILING_QUESTIONS.matcher(result).replaceFirst("").trim();
        log.debug("Normalized query={}", result);
        return result;
    }
}
