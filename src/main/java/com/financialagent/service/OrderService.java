package com.financialagent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financialagent.config.OrderConfig;
import com.financialagent.dto.OrderRequest;
import com.financialagent.dto.OrderResponse;
import com.financialagent.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;

/**
 * Order placement service for AngelOne SmartAPI.
 * Equivalent to place_order.py in the Python version.
 * <p>
 * WARNING: This service can place REAL orders with real money when dryRun is false.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final AngelOneSessionService sessionService;
    private final OrderConfig orderConfig;
    private final ObjectMapper objectMapper;

    private static final String PLACE_ORDER_ENDPOINT = "/rest/secure/angelbroking/order/v1/placeOrder";
    private static final String ORDER_BOOK_ENDPOINT = "/rest/secure/angelbroking/order/v1/getOrderBook";
    private static final String CANCEL_ORDER_ENDPOINT = "/rest/secure/angelbroking/order/v1/cancelOrder";

    /**
     * Place a buy order.
     *
     * @param symbol    Trading symbol (e.g., "RELIANCE-EQ")
     * @param token     SmartAPI symbol token
     * @param quantity  Number of shares
     * @param price     Limit price (0 for market orders)
     * @param orderType "MARKET" or "LIMIT"
     * @return Order ID if placed, null if dry run or failed
     */
    public String placeBuyOrder(String symbol, String token, int quantity,
                                BigDecimal price, String orderType) {
        OrderRequest request = OrderRequest.builder()
                .tradingSymbol(symbol)
                .symbolToken(token)
                .transactionType("BUY")
                .orderType(orderType)
                .quantity(quantity)
                .price(orderType.equals("LIMIT") ? price : BigDecimal.ZERO)
                .build();

        return placeOrder(request, "BUY");
    }

    /**
     * Place a market buy order.
     */
    public String placeMarketBuy(String symbol, String token, int quantity) {
        return placeBuyOrder(symbol, token, quantity, BigDecimal.ZERO, "MARKET");
    }

    /**
     * Place a limit buy order.
     */
    public String placeLimitBuy(String symbol, String token, int quantity, BigDecimal price) {
        return placeBuyOrder(symbol, token, quantity, price, "LIMIT");
    }

    /**
     * Place a sell order.
     */
    public String placeSellOrder(String symbol, String token, int quantity,
                                 BigDecimal price, String orderType) {
        OrderRequest request = OrderRequest.builder()
                .tradingSymbol(symbol)
                .symbolToken(token)
                .transactionType("SELL")
                .orderType(orderType)
                .quantity(quantity)
                .price(orderType.equals("LIMIT") ? price : BigDecimal.ZERO)
                .build();

        return placeOrder(request, "SELL");
    }

    /**
     * Place a market sell order.
     */
    public String placeMarketSell(String symbol, String token, int quantity) {
        return placeSellOrder(symbol, token, quantity, BigDecimal.ZERO, "MARKET");
    }

    /**
     * Place a limit sell order.
     */
    public String placeLimitSell(String symbol, String token, int quantity, BigDecimal price) {
        return placeSellOrder(symbol, token, quantity, price, "LIMIT");
    }

    /**
     * Place a stop-loss limit order.
     *
     * @param symbol       Trading symbol
     * @param token        Symbol token
     * @param quantity     Number of shares
     * @param limitPrice   Limit price for the order
     * @param triggerPrice Price at which the order activates
     * @return Order ID if placed, null if dry run or failed
     */
    public String placeStopLossOrder(String symbol, String token, int quantity,
                                     BigDecimal limitPrice, BigDecimal triggerPrice) {
        if (triggerPrice.compareTo(BigDecimal.ZERO) <= 0 || limitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("Stop-loss orders need both price and trigger_price > 0");
            return null;
        }

        OrderRequest request = OrderRequest.builder()
                .variety("STOPLOSS")
                .tradingSymbol(symbol)
                .symbolToken(token)
                .transactionType("SELL")
                .orderType("STOPLOSS_LIMIT")
                .quantity(quantity)
                .price(limitPrice)
                .triggerPrice(triggerPrice)
                .build();

        return placeOrder(request, "STOP-LOSS");
    }

    /**
     * Internal method to place an order.
     */
    private String placeOrder(OrderRequest request, String actionType) {
        logOrderDetails(request, actionType);

        if (orderConfig.isDryRun()) {
            log.info("[DRY RUN] Order NOT placed. Set order.dry-run=false to execute.");
            return null;
        }

        try {
            WebClient client = sessionService.createAuthenticatedClient();

            OrderResponse response = client.post()
                    .uri(PLACE_ORDER_ENDPOINT)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OrderResponse.class)
                    .block();

            if (response == null) {
                log.error("Empty response from order placement");
                return null;
            }

            if (response.isSuccessful()) {
                String orderId = response.getOrderId();
                log.info("Order placed! Order ID: {}", orderId);
                return orderId;
            } else {
                log.error("Order failed: {}", response.getMessage());
                return null;
            }

        } catch (WebClientResponseException e) {
            log.error("Order failed: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ApiException("Order placement failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Order failed: {}", e.getMessage());
            throw new ApiException("Order placement failed: " + e.getMessage(), e);
        }
    }

    /**
     * Check the status of an order by order ID.
     */
    public JsonNode checkOrderStatus(String orderId) {
        if (orderConfig.isDryRun()) {
            log.info("[DRY RUN] Would check status of order: {}", orderId);
            return null;
        }

        try {
            WebClient client = sessionService.createAuthenticatedClient();

            String responseBody = client.get()
                    .uri(ORDER_BOOK_ENDPOINT)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode data = root.path("data");

            if (data.isArray()) {
                for (JsonNode order : data) {
                    if (orderId.equals(order.path("orderid").asText())) {
                        log.info("Order {}: Status={}, Symbol={}, Qty={}, Price={}",
                                orderId,
                                order.path("orderstatus").asText(),
                                order.path("tradingsymbol").asText(),
                                order.path("quantity").asText(),
                                order.path("price").asText());
                        return order;
                    }
                }
            }

            log.warn("Order {} not found in order book", orderId);
            return null;

        } catch (Exception e) {
            log.error("Failed to fetch order status: {}", e.getMessage());
            throw new ApiException("Failed to fetch order status: " + e.getMessage(), e);
        }
    }

    /**
     * Cancel an open order.
     *
     * @param orderId Order ID to cancel
     * @param variety "NORMAL" for regular orders, "STOPLOSS" for SL orders
     * @return true if cancelled successfully
     */
    public boolean cancelOrder(String orderId, String variety) {
        log.info("Cancelling order: {} (variety: {})", orderId, variety);

        if (orderConfig.isDryRun()) {
            log.info("[DRY RUN] Order NOT cancelled.");
            return false;
        }

        try {
            WebClient client = sessionService.createAuthenticatedClient();

            CancelOrderRequest request = new CancelOrderRequest(variety, orderId);

            String responseBody = client.post()
                    .uri(CANCEL_ORDER_ENDPOINT)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(responseBody);
            boolean status = root.path("status").asBoolean();

            if (status) {
                log.info("Order cancelled: {}", orderId);
                return true;
            } else {
                log.error("Cancel failed: {}", root.path("message").asText());
                return false;
            }

        } catch (Exception e) {
            log.error("Cancel failed: {}", e.getMessage());
            throw new ApiException("Order cancellation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Cancel a normal order.
     */
    public boolean cancelOrder(String orderId) {
        return cancelOrder(orderId, "NORMAL");
    }

    /**
     * Check if running in dry-run mode.
     */
    public boolean isDryRunMode() {
        return orderConfig.isDryRun();
    }

    /**
     * Log order details.
     */
    private void logOrderDetails(OrderRequest request, String actionType) {
        log.info("Order details:");
        log.info("   Action:     {}", actionType);
        log.info("   Symbol:     {}", request.getTradingSymbol());
        log.info("   Quantity:   {}", request.getQuantity());
        log.info("   Type:       {}", request.getOrderType());
        if ("LIMIT".equals(request.getOrderType()) || "STOPLOSS_LIMIT".equals(request.getOrderType())) {
            log.info("   Price:      ₹{}", request.getPrice());
        }
        if (request.getTriggerPrice().compareTo(BigDecimal.ZERO) > 0) {
            log.info("   Trigger:    ₹{}", request.getTriggerPrice());
        }
        log.info("   Product:    {}", request.getProductType());
    }

    // Inner class for cancel order request
    private record CancelOrderRequest(String variety, String orderid) {
    }
}
