package com.financialagent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.util.List;

/**
 * Response from AngelOne SmartAPI candle data endpoint.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CandleResponse {

    private boolean status;
    private String message;
    private String errorcode;

    // Use Object to handle both List<Object[]> and String error messages
    private Object data;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Getter methods as workaround for Lombok annotation processing issue
    public boolean isStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    /**
     * Get data as List<Object[]> or return null if data is not in expected format.
     * This handles cases where API returns error messages as strings.
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> getData() {
        if (data == null) {
            return null;
        }

        // If data is already a List<Object[], return it
        if (data instanceof List) {
            List<?> list = (List<?>) data;
            if (list.isEmpty() || list.get(0) instanceof Object[]) {
                return (List<Object[]>) data;
            }
        }

        // If data is a string, it's likely an error message
        if (data instanceof String) {
            String errorMsg = (String) data;
            System.err.println("API returned error message in data field: " + errorMsg);
            return null;
        }

        // Try to handle JsonNode or other formats
        try {
            JsonNode jsonNode = objectMapper.valueToTree(data);
            if (jsonNode.isArray()) {
                return objectMapper.convertValue(jsonNode,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Object[].class));
            }
        } catch (Exception e) {
            System.err.println("Failed to parse data field: " + e.getMessage());
        }

        System.err.println("Unexpected data format: " + data.getClass().getSimpleName() + " - " + data);
        return null;
    }
}
