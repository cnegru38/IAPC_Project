package com.example.retrievalpre.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.Set;

/** Converts URL JSON to typed, strictly validated Spring AI filter expressions. */
@Service
public class MetadataFilterService {
    public static final Set<String> ALLOWED_FIELDS = Set.of(
            "doc_id", "version", "source", "page", "chunk_index", "checksum");
    public static final Set<String> ALLOWED_OPERATORS = Set.of("eq", "ne", "gt", "gte", "lt", "lte");
    private static final Set<String> NUMERIC_FIELDS = Set.of("page", "chunk_index");
    private static final int MAX_DEPTH = 5;
    private static final int MAX_NODES = 32;
    private static final int MAX_JSON_LENGTH = 4096;

    private final ObjectMapper mapper;

    public MetadataFilterService(ObjectMapper mapper) { this.mapper = mapper; }

    /** Returns null if no client filter was supplied; callers may enforce server-side access filters. */
    public Filter.Expression parse(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) return null;
        if (rawJson.length() > MAX_JSON_LENGTH) throw invalid("Filter too long");
        final JsonNode root;
        try { root = mapper.readTree(rawJson); }
        catch (JsonProcessingException ex) { throw invalid("Invalid JSON filter"); }
        if (root == null) throw invalid("Filter must be a JSON object");
        return build(root, 0, new int[] {0}).build();
    }

    private FilterExpressionBuilder.Op build(JsonNode node, int depth, int[] count) {
        if (depth > MAX_DEPTH || ++count[0] > MAX_NODES) throw invalid("Filter is too complex");
        if (!node.isObject()) throw invalid("Every filter node must be a JSON object");
        var builder = new FilterExpressionBuilder();
        if (node.has("logic")) {
            requireOnly(node, Set.of("logic", "filters"));
            var logic = node.path("logic");
            var filters = node.path("filters");
            if (!logic.isTextual() || !(logic.asText().equals("AND") || logic.asText().equals("OR")))
                throw invalid("logic must be AND or OR");
            if (!filters.isArray() || filters.size() < 2 || filters.size() > 16)
                throw invalid("filters must contain 2 to 16 conditions");
            var result = build(filters.get(0), depth + 1, count);
            for (int i = 1; i < filters.size(); i++) {
                var next = build(filters.get(i), depth + 1, count);
                result = logic.asText().equals("AND") ? builder.and(result, next) : builder.or(result, next);
            }
            return result;
        }
        requireOnly(node, Set.of("field", "op", "value"));
        var fieldNode = node.path("field");
        var opNode = node.path("op");
        var valueNode = node.path("value");
        if (!fieldNode.isTextual() || !ALLOWED_FIELDS.contains(fieldNode.asText()))
            throw invalid("Unsupported metadata field");
        if (!opNode.isTextual() || !ALLOWED_OPERATORS.contains(opNode.asText()))
            throw invalid("Unsupported operator");
        String field = fieldNode.asText();
        String operator = opNode.asText();
        Object value;
        if (NUMERIC_FIELDS.contains(field)) {
            if (!valueNode.isIntegralNumber() || !valueNode.canConvertToInt() || valueNode.intValue() < 0)
                throw invalid(field + " must be a nonnegative integer");
            value = valueNode.intValue();
        } else {
            if (!valueNode.isTextual() || valueNode.asText().isBlank() || valueNode.asText().length() > 256)
                throw invalid(field + " must be a nonempty string (max 256 characters)");
            if (!(operator.equals("eq") || operator.equals("ne")))
                throw invalid("Only eq/ne permitted for string metadata");
            value = valueNode.asText();
        }
        return switch (operator) {
            case "eq" -> builder.eq(field, value);
            case "ne" -> builder.ne(field, value);
            case "gt" -> builder.gt(field, value);
            case "gte" -> builder.gte(field, value);
            case "lt" -> builder.lt(field, value);
            case "lte" -> builder.lte(field, value);
            default -> throw invalid("Unsupported operator");
        };
    }

    private static void requireOnly(JsonNode node, Set<String> fields) {
        var names = node.fieldNames();
        while (names.hasNext()) if (!fields.contains(names.next())) throw invalid("Unexpected filter property");
        for (String mandatory : fields) if (!node.has(mandatory)) throw invalid("Missing filter property: " + mandatory);
    }

    private static com.example.retrievalpre.filter.InvalidMetadataFilterException invalid(String reason) {
        return new com.example.retrievalpre.filter.InvalidMetadataFilterException(reason + ". Valid fields: " + ALLOWED_FIELDS
                + "; valid operators: " + ALLOWED_OPERATORS + "; combinators: AND, OR");
    }
}
