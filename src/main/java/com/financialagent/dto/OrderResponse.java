package com.financialagent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Order placement response from AngelOne SmartAPI.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderResponse {

    private boolean status;
    private String message;
    private String errorcode;

    // Getter method as workaround for Lombok annotation processing issue
    public String getMessage() {
        return message;
    }

    @JsonProperty("data")
    private OrderData orderData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderData {
        @JsonProperty("orderid")
        private String orderId;

        @JsonProperty("uniqueorderid")
        private String uniqueOrderId;

        // Getter method as workaround for Lombok annotation processing issue
        public String getOrderId() {
            return orderId;
        }
    }

    public String getOrderId() {
        return orderData != null ? orderData.getOrderId() : null;
    }

    public boolean isSuccessful() {
        return status && orderData != null && orderData.getOrderId() != null;
    }
}
