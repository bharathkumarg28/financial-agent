package com.financialagent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for fetching candle data from AngelOne SmartAPI.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandleRequest {

    private String exchange;

    @JsonProperty("symboltoken")
    private String symbolToken;

    private String interval;

    @JsonProperty("fromdate")
    private String fromDate;

    @JsonProperty("todate")
    private String toDate;

    // Static builder method as workaround for Lombok annotation processing issue
    public static CandleRequestBuilder builder() {
        return new CandleRequestBuilder();
    }

    // Setter methods as workaround for Lombok annotation processing issue
    public void setFromDate(String fromDate) {
        this.fromDate = fromDate;
    }

    public void setToDate(String toDate) {
        this.toDate = toDate;
    }

    public static class CandleRequestBuilder {
        private String exchange;
        private String symbolToken;
        private String interval;
        private String fromDate;
        private String toDate;

        public CandleRequestBuilder exchange(String exchange) {
            this.exchange = exchange;
            return this;
        }

        public CandleRequestBuilder symbolToken(String symbolToken) {
            this.symbolToken = symbolToken;
            return this;
        }

        public CandleRequestBuilder interval(String interval) {
            this.interval = interval;
            return this;
        }

        public CandleRequestBuilder fromDate(String fromDate) {
            this.fromDate = fromDate;
            return this;
        }

        public CandleRequestBuilder toDate(String toDate) {
            this.toDate = toDate;
            return this;
        }

        public CandleRequest build() {
            CandleRequest request = new CandleRequest();
            request.exchange = this.exchange;
            request.symbolToken = this.symbolToken;
            request.interval = this.interval;
            request.fromDate = this.fromDate;
            request.toDate = this.toDate;
            return request;
        }
    }
}
