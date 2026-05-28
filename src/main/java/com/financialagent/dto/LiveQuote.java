package com.financialagent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Represents a live market quote with real-time price data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveQuote {

    private String symbol;
    private String token;
    private String exchange;

    // Price data
    private BigDecimal ltp;        // Last Traded Price
    private BigDecimal open;       // Opening price
    private BigDecimal high;       // Highest price of the day
    private BigDecimal low;        // Lowest price of the day
    private BigDecimal close;      // Previous close price

    // Bid/Ask data
    private BigDecimal bidPrice;   // Highest bid price
    private Integer bidQuantity;   // Bid quantity
    private BigDecimal askPrice;   // Lowest ask price  
    private Integer askQuantity;   // Ask quantity

    // Volume and timestamp
    private Long volume;           // Traded volume
    private Long timestamp;        // Unix timestamp

    // Computed fields
    private BigDecimal change;     // Price change from previous close
    private BigDecimal changePercent; // Percentage change

    // Getter methods as workaround for Lombok annotation processing issue
    public String getSymbol() {
        return symbol;
    }

    public String getToken() {
        return token;
    }

    public BigDecimal getLtp() {
        return ltp;
    }

    // Static builder method as workaround for Lombok annotation processing issue
    public static LiveQuoteBuilder builder() {
        return new LiveQuoteBuilder();
    }

    public static class LiveQuoteBuilder {
        private String symbol;
        private String token;
        private String exchange;
        private BigDecimal ltp;
        private BigDecimal open;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal close;
        private BigDecimal bidPrice;
        private Integer bidQuantity;
        private BigDecimal askPrice;
        private Integer askQuantity;
        private Long volume;
        private Long timestamp;
        private BigDecimal change;
        private BigDecimal changePercent;

        public LiveQuoteBuilder symbol(String symbol) {
            this.symbol = symbol;
            return this;
        }

        public LiveQuoteBuilder token(String token) {
            this.token = token;
            return this;
        }

        public LiveQuoteBuilder exchange(String exchange) {
            this.exchange = exchange;
            return this;
        }

        public LiveQuoteBuilder ltp(BigDecimal ltp) {
            this.ltp = ltp;
            return this;
        }

        public LiveQuoteBuilder open(BigDecimal open) {
            this.open = open;
            return this;
        }

        public LiveQuoteBuilder high(BigDecimal high) {
            this.high = high;
            return this;
        }

        public LiveQuoteBuilder low(BigDecimal low) {
            this.low = low;
            return this;
        }

        public LiveQuoteBuilder close(BigDecimal close) {
            this.close = close;
            return this;
        }

        public LiveQuoteBuilder bidPrice(BigDecimal bidPrice) {
            this.bidPrice = bidPrice;
            return this;
        }

        public LiveQuoteBuilder bidQuantity(Integer bidQuantity) {
            this.bidQuantity = bidQuantity;
            return this;
        }

        public LiveQuoteBuilder askPrice(BigDecimal askPrice) {
            this.askPrice = askPrice;
            return this;
        }

        public LiveQuoteBuilder askQuantity(Integer askQuantity) {
            this.askQuantity = askQuantity;
            return this;
        }

        public LiveQuoteBuilder volume(Long volume) {
            this.volume = volume;
            return this;
        }

        public LiveQuoteBuilder timestamp(Long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public LiveQuoteBuilder change(BigDecimal change) {
            this.change = change;
            return this;
        }

        public LiveQuoteBuilder changePercent(BigDecimal changePercent) {
            this.changePercent = changePercent;
            return this;
        }

        public LiveQuote build() {
            LiveQuote quote = new LiveQuote();
            quote.symbol = this.symbol;
            quote.token = this.token;
            quote.exchange = this.exchange;
            quote.ltp = this.ltp;
            quote.open = this.open;
            quote.high = this.high;
            quote.low = this.low;
            quote.close = this.close;
            quote.bidPrice = this.bidPrice;
            quote.bidQuantity = this.bidQuantity;
            quote.askPrice = this.askPrice;
            quote.askQuantity = this.askQuantity;
            quote.volume = this.volume;
            quote.timestamp = this.timestamp;
            quote.change = this.change;
            quote.changePercent = this.changePercent;
            return quote;
        }
    }

    /**
     * Get the timestamp as LocalDateTime.
     */
    public LocalDateTime getTimestampDateTime() {
        if (timestamp == null) {
            return null;
        }
        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                ZoneId.systemDefault()
        );
    }

    /**
     * Calculate change and change percent if not already set.
     */
    public void calculateChange() {
        if (close != null && ltp != null) {
            change = ltp.subtract(close);
            if (close.compareTo(BigDecimal.ZERO) != 0) {
                changePercent = change.divide(close, 4, BigDecimal.ROUND_HALF_UP)
                        .multiply(new BigDecimal("100"));
            }
        }
    }

    /**
     * Check if the price is up (green).
     */
    public boolean isUp() {
        return change != null && change.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Check if the price is down (red).
     */
    public boolean isDown() {
        return change != null && change.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Get formatted LTP string.
     */
    public String getFormattedLtp() {
        return ltp != null ? ltp.setScale(2, BigDecimal.ROUND_HALF_UP).toString() : "0.00";
    }

    /**
     * Get formatted change string with sign.
     */
    public String getFormattedChange() {
        if (change == null) return "0.00";
        String sign = isUp() ? "+" : "";
        return sign + change.setScale(2, BigDecimal.ROUND_HALF_UP).toString();
    }

    /**
     * Get formatted change percent string with sign.
     */
    public String getFormattedChangePercent() {
        if (changePercent == null) return "0.00%";
        String sign = isUp() ? "+" : "";
        return sign + changePercent.setScale(2, BigDecimal.ROUND_HALF_UP).toString() + "%";
    }

    /**
     * Get formatted bid-ask spread.
     */
    public String getFormattedSpread() {
        if (bidPrice == null || askPrice == null) return "0.00";
        return askPrice.subtract(bidPrice).setScale(2, BigDecimal.ROUND_HALF_UP).toString();
    }
}
