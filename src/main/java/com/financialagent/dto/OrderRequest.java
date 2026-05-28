package com.financialagent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Order placement request for AngelOne SmartAPI.
 * Used by place_order functionality in financial-agent-india.
 */
public class OrderRequest {

    private String variety = "NORMAL";

    @JsonProperty("tradingsymbol")
    private String tradingSymbol;

    @JsonProperty("symboltoken")
    private String symbolToken;

    @JsonProperty("transactiontype")
    private String transactionType;

    private String exchange = "NSE";

    @JsonProperty("ordertype")
    private String orderType;

    @JsonProperty("producttype")
    private String productType = "DELIVERY";

    private String duration = "DAY";

    private int quantity;

    private BigDecimal price = BigDecimal.ZERO;

    @JsonProperty("squareoff")
    private BigDecimal squareOff = BigDecimal.ZERO;

    @JsonProperty("stoploss")
    private BigDecimal stopLoss = BigDecimal.ZERO;

    @JsonProperty("triggerprice")
    private BigDecimal triggerPrice = BigDecimal.ZERO;

    // Constructors
    public OrderRequest() {
    }

    public OrderRequest(String variety, String tradingSymbol, String symbolToken, String transactionType,
                        String exchange, String orderType, String productType, String duration,
                        int quantity, BigDecimal price, BigDecimal squareOff, BigDecimal stopLoss, BigDecimal triggerPrice) {
        this.variety = variety;
        this.tradingSymbol = tradingSymbol;
        this.symbolToken = symbolToken;
        this.transactionType = transactionType;
        this.exchange = exchange;
        this.orderType = orderType;
        this.productType = productType;
        this.duration = duration;
        this.quantity = quantity;
        this.price = price;
        this.squareOff = squareOff;
        this.stopLoss = stopLoss;
        this.triggerPrice = triggerPrice;
    }

    // Getters and Setters
    public String getVariety() {
        return variety;
    }

    public void setVariety(String variety) {
        this.variety = variety;
    }

    public String getTradingSymbol() {
        return tradingSymbol;
    }

    public void setTradingSymbol(String tradingSymbol) {
        this.tradingSymbol = tradingSymbol;
    }

    public String getSymbolToken() {
        return symbolToken;
    }

    public void setSymbolToken(String symbolToken) {
        this.symbolToken = symbolToken;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getSquareOff() {
        return squareOff;
    }

    public void setSquareOff(BigDecimal squareOff) {
        this.squareOff = squareOff;
    }

    public BigDecimal getStopLoss() {
        return stopLoss;
    }

    public void setStopLoss(BigDecimal stopLoss) {
        this.stopLoss = stopLoss;
    }

    public BigDecimal getTriggerPrice() {
        return triggerPrice;
    }

    public void setTriggerPrice(BigDecimal triggerPrice) {
        this.triggerPrice = triggerPrice;
    }

    // Static factory methods
    public static OrderRequest marketBuy(String symbol, String token, int qty) {
        OrderRequest order = new OrderRequest();
        order.setTradingSymbol(symbol);
        order.setSymbolToken(token);
        order.setTransactionType("BUY");
        order.setOrderType("MARKET");
        order.setQuantity(qty);
        return order;
    }

    public static OrderRequest marketSell(String symbol, String token, int qty) {
        OrderRequest order = new OrderRequest();
        order.setTradingSymbol(symbol);
        order.setSymbolToken(token);
        order.setTransactionType("SELL");
        order.setOrderType("MARKET");
        order.setQuantity(qty);
        return order;
    }

    public static OrderRequest limitBuy(String symbol, String token, int qty, BigDecimal limitPrice) {
        OrderRequest order = new OrderRequest();
        order.setTradingSymbol(symbol);
        order.setSymbolToken(token);
        order.setTransactionType("BUY");
        order.setOrderType("LIMIT");
        order.setQuantity(qty);
        order.setPrice(limitPrice);
        return order;
    }

    public static OrderRequest limitSell(String symbol, String token, int qty, BigDecimal limitPrice) {
        OrderRequest order = new OrderRequest();
        order.setTradingSymbol(symbol);
        order.setSymbolToken(token);
        order.setTransactionType("SELL");
        order.setOrderType("LIMIT");
        order.setQuantity(qty);
        order.setPrice(limitPrice);
        return order;
    }

    public static OrderRequest stopLossSell(String symbol, String token, int qty,
                                            BigDecimal limitPrice, BigDecimal trigger) {
        OrderRequest order = new OrderRequest();
        order.setVariety("STOPLOSS");
        order.setTradingSymbol(symbol);
        order.setSymbolToken(token);
        order.setTransactionType("SELL");
        order.setOrderType("STOPLOSS_LIMIT");
        order.setQuantity(qty);
        order.setPrice(limitPrice);
        order.setTriggerPrice(trigger);
        return order;
    }

    // Static builder method as workaround for Lombok annotation processing issue
    public static OrderRequestBuilder builder() {
        return new OrderRequestBuilder();
    }

    public static class OrderRequestBuilder {
        private String variety = "NORMAL";
        private String tradingSymbol;
        private String symbolToken;
        private String transactionType;
        private String exchange = "NSE";
        private String orderType;
        private String productType = "DELIVERY";
        private String duration = "DAY";
        private int quantity;
        private BigDecimal price = BigDecimal.ZERO;
        private BigDecimal squareOff = BigDecimal.ZERO;
        private BigDecimal stopLoss = BigDecimal.ZERO;
        private BigDecimal triggerPrice = BigDecimal.ZERO;

        public OrderRequestBuilder variety(String variety) {
            this.variety = variety;
            return this;
        }

        public OrderRequestBuilder tradingSymbol(String tradingSymbol) {
            this.tradingSymbol = tradingSymbol;
            return this;
        }

        public OrderRequestBuilder symbolToken(String symbolToken) {
            this.symbolToken = symbolToken;
            return this;
        }

        public OrderRequestBuilder transactionType(String transactionType) {
            this.transactionType = transactionType;
            return this;
        }

        public OrderRequestBuilder exchange(String exchange) {
            this.exchange = exchange;
            return this;
        }

        public OrderRequestBuilder orderType(String orderType) {
            this.orderType = orderType;
            return this;
        }

        public OrderRequestBuilder productType(String productType) {
            this.productType = productType;
            return this;
        }

        public OrderRequestBuilder duration(String duration) {
            this.duration = duration;
            return this;
        }

        public OrderRequestBuilder quantity(int quantity) {
            this.quantity = quantity;
            return this;
        }

        public OrderRequestBuilder price(BigDecimal price) {
            this.price = price;
            return this;
        }

        public OrderRequestBuilder squareOff(BigDecimal squareOff) {
            this.squareOff = squareOff;
            return this;
        }

        public OrderRequestBuilder stopLoss(BigDecimal stopLoss) {
            this.stopLoss = stopLoss;
            return this;
        }

        public OrderRequestBuilder triggerPrice(BigDecimal triggerPrice) {
            this.triggerPrice = triggerPrice;
            return this;
        }

        public OrderRequest build() {
            OrderRequest order = new OrderRequest();
            order.variety = this.variety;
            order.tradingSymbol = this.tradingSymbol;
            order.symbolToken = this.symbolToken;
            order.transactionType = this.transactionType;
            order.exchange = this.exchange;
            order.orderType = this.orderType;
            order.productType = this.productType;
            order.duration = this.duration;
            order.quantity = this.quantity;
            order.price = this.price;
            order.squareOff = this.squareOff;
            order.stopLoss = this.stopLoss;
            order.triggerPrice = this.triggerPrice;
            return order;
        }
    }
}
