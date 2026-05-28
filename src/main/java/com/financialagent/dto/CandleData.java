package com.financialagent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a single OHLCV candle.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandleData {

    private LocalDateTime timestamp;
    private String dateString;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private Long volume;

    // Getter methods as workaround for Lombok annotation processing issue
    public String getDateString() {
        return dateString;
    }

    public BigDecimal getOpen() {
        return open;
    }

    public BigDecimal getHigh() {
        return high;
    }

    public BigDecimal getLow() {
        return low;
    }

    public BigDecimal getClose() {
        return close;
    }

    public Long getVolume() {
        return volume;
    }

    // Static builder method as workaround for Lombok annotation processing issue
    public static CandleDataBuilder builder() {
        return new CandleDataBuilder();
    }

    public static class CandleDataBuilder {
        private LocalDateTime timestamp;
        private String dateString;
        private BigDecimal open;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal close;
        private Long volume;

        public CandleDataBuilder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public CandleDataBuilder dateString(String dateString) {
            this.dateString = dateString;
            return this;
        }

        public CandleDataBuilder open(BigDecimal open) {
            this.open = open;
            return this;
        }

        public CandleDataBuilder high(BigDecimal high) {
            this.high = high;
            return this;
        }

        public CandleDataBuilder low(BigDecimal low) {
            this.low = low;
            return this;
        }

        public CandleDataBuilder close(BigDecimal close) {
            this.close = close;
            return this;
        }

        public CandleDataBuilder volume(Long volume) {
            this.volume = volume;
            return this;
        }

        public CandleData build() {
            CandleData candle = new CandleData();
            candle.timestamp = this.timestamp;
            candle.dateString = this.dateString;
            candle.open = this.open;
            candle.high = this.high;
            candle.low = this.low;
            candle.close = this.close;
            candle.volume = this.volume;
            return candle;
        }
    }

    /**
     * Creates CandleData from SmartAPI response array.
     * Format: [timestamp, open, high, low, close, volume]
     */
    public static CandleData fromArray(Object[] data) {
        if (data == null || data.length < 6) {
            return null;
        }

        String timestampStr = String.valueOf(data[0]);
        String dateStr = timestampStr.length() >= 10 ? timestampStr.substring(0, 10) : timestampStr;

        return CandleData.builder()
                .dateString(dateStr)
                .open(toBigDecimal(data[1]))
                .high(toBigDecimal(data[2]))
                .low(toBigDecimal(data[3]))
                .close(toBigDecimal(data[4]))
                .volume(toLong(data[5]))
                .build();
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private static Long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
