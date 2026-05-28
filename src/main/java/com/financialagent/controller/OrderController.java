package com.financialagent.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.financialagent.dto.ApiResponse;
import com.financialagent.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * REST controller for order placement.
 * Equivalent to place_order.py functionality.
 * <p>
 * WARNING: This controller can place REAL orders when dry-run mode is disabled.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Check if running in dry-run mode.
     */
    @GetMapping("/mode")
    public ResponseEntity<ApiResponse<OrderModeResponse>> getMode() {
        boolean dryRun = orderService.isDryRunMode();

        OrderModeResponse response = new OrderModeResponse(
                dryRun,
                dryRun ? "DRY RUN - No real orders will be placed"
                        : "LIVE MODE - Orders WILL be placed with real money!"
        );

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Place a market buy order.
     */
    @PostMapping("/buy/market")
    public ResponseEntity<ApiResponse<OrderResultResponse>> marketBuy(
            @RequestParam String symbol,
            @RequestParam String token,
            @RequestParam int quantity) {

        try {
            String orderId = orderService.placeMarketBuy(symbol, token, quantity);
            return createOrderResponse(orderId, "BUY", "MARKET", symbol, quantity, null);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("Order failed", e.getMessage()));
        }
    }

    /**
     * Place a limit buy order.
     */
    @PostMapping("/buy/limit")
    public ResponseEntity<ApiResponse<OrderResultResponse>> limitBuy(
            @RequestParam String symbol,
            @RequestParam String token,
            @RequestParam int quantity,
            @RequestParam BigDecimal price) {

        try {
            String orderId = orderService.placeLimitBuy(symbol, token, quantity, price);
            return createOrderResponse(orderId, "BUY", "LIMIT", symbol, quantity, price);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("Order failed", e.getMessage()));
        }
    }

    /**
     * Place a market sell order.
     */
    @PostMapping("/sell/market")
    public ResponseEntity<ApiResponse<OrderResultResponse>> marketSell(
            @RequestParam String symbol,
            @RequestParam String token,
            @RequestParam int quantity) {

        try {
            String orderId = orderService.placeMarketSell(symbol, token, quantity);
            return createOrderResponse(orderId, "SELL", "MARKET", symbol, quantity, null);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("Order failed", e.getMessage()));
        }
    }

    /**
     * Place a limit sell order.
     */
    @PostMapping("/sell/limit")
    public ResponseEntity<ApiResponse<OrderResultResponse>> limitSell(
            @RequestParam String symbol,
            @RequestParam String token,
            @RequestParam int quantity,
            @RequestParam BigDecimal price) {

        try {
            String orderId = orderService.placeLimitSell(symbol, token, quantity, price);
            return createOrderResponse(orderId, "SELL", "LIMIT", symbol, quantity, price);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("Order failed", e.getMessage()));
        }
    }

    /**
     * Place a stop-loss order.
     */
    @PostMapping("/stoploss")
    public ResponseEntity<ApiResponse<OrderResultResponse>> stopLoss(
            @RequestParam String symbol,
            @RequestParam String token,
            @RequestParam int quantity,
            @RequestParam BigDecimal limitPrice,
            @RequestParam BigDecimal triggerPrice) {

        try {
            String orderId = orderService.placeStopLossOrder(
                    symbol, token, quantity, limitPrice, triggerPrice);

            OrderResultResponse response = new OrderResultResponse(
                    orderId,
                    orderId == null && orderService.isDryRunMode(),
                    "STOP-LOSS",
                    "STOPLOSS_LIMIT",
                    symbol,
                    quantity,
                    limitPrice,
                    triggerPrice,
                    orderId != null ? "Order placed successfully"
                            : (orderService.isDryRunMode() ? "Dry run - order not placed" : "Order failed")
            );

            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("Order failed", e.getMessage()));
        }
    }

    /**
     * Place an order using request body.
     */
    @PostMapping("/place")
    public ResponseEntity<ApiResponse<OrderResultResponse>> placeOrder(
            @RequestBody PlaceOrderRequest request) {

        try {
            String orderId;

            if ("BUY".equalsIgnoreCase(request.action())) {
                if ("MARKET".equalsIgnoreCase(request.orderType())) {
                    orderId = orderService.placeMarketBuy(request.symbol(), request.token(), request.quantity());
                } else {
                    orderId = orderService.placeLimitBuy(request.symbol(), request.token(),
                            request.quantity(), request.price());
                }
            } else {
                if ("MARKET".equalsIgnoreCase(request.orderType())) {
                    orderId = orderService.placeMarketSell(request.symbol(), request.token(), request.quantity());
                } else {
                    orderId = orderService.placeLimitSell(request.symbol(), request.token(),
                            request.quantity(), request.price());
                }
            }

            return createOrderResponse(orderId, request.action(), request.orderType(),
                    request.symbol(), request.quantity(), request.price());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("Order failed", e.getMessage()));
        }
    }

    /**
     * Check order status.
     */
    @GetMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<JsonNode>> getOrderStatus(@PathVariable String orderId) {
        try {
            JsonNode status = orderService.checkOrderStatus(orderId);

            if (status == null) {
                if (orderService.isDryRunMode()) {
                    return ResponseEntity.ok(ApiResponse.ok(null,
                            "Dry run mode - would check status of order: " + orderId));
                }
                return ResponseEntity.ok(ApiResponse.fail("Order not found in order book"));
            }

            return ResponseEntity.ok(ApiResponse.ok(status, "Order status retrieved"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("Failed to get order status", e.getMessage()));
        }
    }

    /**
     * Cancel an order.
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<ApiResponse<CancelResultResponse>> cancelOrder(
            @PathVariable String orderId,
            @RequestParam(defaultValue = "NORMAL") String variety) {

        try {
            boolean cancelled = orderService.cancelOrder(orderId, variety);

            CancelResultResponse response = new CancelResultResponse(
                    orderId,
                    cancelled,
                    orderService.isDryRunMode(),
                    cancelled ? "Order cancelled successfully"
                            : (orderService.isDryRunMode() ? "Dry run - order not cancelled" : "Cancel failed")
            );

            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("Cancel failed", e.getMessage()));
        }
    }

    /**
     * Helper to create order response.
     */
    private ResponseEntity<ApiResponse<OrderResultResponse>> createOrderResponse(
            String orderId, String action, String orderType, String symbol,
            int quantity, BigDecimal price) {

        OrderResultResponse response = new OrderResultResponse(
                orderId,
                orderId == null && orderService.isDryRunMode(),
                action,
                orderType,
                symbol,
                quantity,
                price,
                null,
                orderId != null ? "Order placed successfully"
                        : (orderService.isDryRunMode() ? "Dry run - order not placed" : "Order failed")
        );

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // Request/Response records
    public record PlaceOrderRequest(
            String symbol,
            String token,
            String action,
            String orderType,
            int quantity,
            BigDecimal price
    ) {
    }

    public record OrderModeResponse(
            boolean dryRun,
            String message
    ) {
    }

    public record OrderResultResponse(
            String orderId,
            boolean dryRun,
            String action,
            String orderType,
            String symbol,
            int quantity,
            BigDecimal price,
            BigDecimal triggerPrice,
            String message
    ) {
    }

    public record CancelResultResponse(
            String orderId,
            boolean cancelled,
            boolean dryRun,
            String message
    ) {
    }
}
