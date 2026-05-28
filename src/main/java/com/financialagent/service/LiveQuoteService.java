package com.financialagent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financialagent.dto.LiveQuote;
import com.financialagent.exception.ApiException;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Service for handling live market data via WebSocket connection to AngelOne.
 * Provides real-time quotes for subscribed symbols.
 */
@Service
@RequiredArgsConstructor
public class LiveQuoteService {

    private static final Logger log = LoggerFactory.getLogger(LiveQuoteService.class);

    private final AngelOneSessionService sessionService;
    private final ObjectMapper objectMapper;

    private WebSocketSession webSocketSession;
    private final Map<String, Consumer<LiveQuote>> subscribers = new ConcurrentHashMap<>();
    private final Map<String, LiveQuote> latestQuotes = new ConcurrentHashMap<>();

    private static final String WEBSOCKET_URL = "wss://ws.smartapi.angelbroking.com/websocket/marketdata";

    /**
     * Subscribe to live quotes for a symbol.
     *
     * @param symbol   Stock symbol (e.g., "RELIANCE")
     * @param token    SmartAPI symbol token (e.g., "2885")
     * @param callback Consumer to handle quote updates
     */
    public void subscribe(String symbol, String token, Consumer<LiveQuote> callback) {
        String key = symbol + ":" + token;
        subscribers.put(key, callback);

        // Initialize WebSocket if not already connected
        if (webSocketSession == null || !webSocketSession.isOpen()) {
            connectWebSocket();
        }

        // Send subscription message
        subscribeToSymbol(symbol, token);

        log.info("Subscribed to live quotes for {}", symbol);
    }

    /**
     * Unsubscribe from live quotes for a symbol.
     */
    public void unsubscribe(String symbol, String token) {
        String key = symbol + ":" + token;
        subscribers.remove(key);

        // Send unsubscribe message
        unsubscribeFromSymbol(symbol, token);

        log.info("Unsubscribed from live quotes for {}", symbol);
    }

    /**
     * Get the latest quote for a symbol.
     */
    public LiveQuote getLatestQuote(String symbol, String token) {
        String key = symbol + ":" + token;
        return latestQuotes.get(key);
    }

    /**
     * Connect to AngelOne WebSocket for live market data.
     */
    private void connectWebSocket() {
        try {
            String feedToken = sessionService.getFeedToken();
            if (feedToken == null) {
                throw new ApiException("No feed token available. Please authenticate first.");
            }

            WebSocketClient client = new StandardWebSocketClient();

            webSocketSession = client.doHandshake(
                    new AngelOneWebSocketHandler(),
                    null,
                    WEBSOCKET_URL + "?clientcode=" + sessionService.getJwtToken() + "&feedtoken=" + feedToken
            ).get();

            log.info("Connected to AngelOne WebSocket for live market data");

        } catch (Exception e) {
            log.error("Failed to connect to WebSocket: {}", e.getMessage());
            throw new ApiException("Failed to connect to live market data: " + e.getMessage(), e);
        }
    }

    /**
     * Send subscription message for a symbol.
     */
    private void subscribeToSymbol(String symbol, String token) {
        try {
            if (webSocketSession != null && webSocketSession.isOpen()) {
                String subscribeMessage = String.format(
                        "{\"action\":\"subscribe\",\"params\":{\"symbol\":\"%s\",\"token\":\"%s\"}}",
                        token, token
                );
                webSocketSession.sendMessage(new TextMessage(subscribeMessage));
                log.debug("Sent subscription for {}", symbol);
            }
        } catch (Exception e) {
            log.error("Failed to subscribe to {}: {}", symbol, e.getMessage());
        }
    }

    /**
     * Send unsubscribe message for a symbol.
     */
    private void unsubscribeFromSymbol(String symbol, String token) {
        try {
            if (webSocketSession != null && webSocketSession.isOpen()) {
                String unsubscribeMessage = String.format(
                        "{\"action\":\"unsubscribe\",\"params\":{\"symbol\":\"%s\",\"token\":\"%s\"}}",
                        token, token
                );
                webSocketSession.sendMessage(new TextMessage(unsubscribeMessage));
                log.debug("Sent unsubscribe for {}", symbol);
            }
        } catch (Exception e) {
            log.error("Failed to unsubscribe from {}: {}", symbol, e.getMessage());
        }
    }

    /**
     * WebSocket message handler for AngelOne live data.
     */
    private class AngelOneWebSocketHandler implements org.springframework.web.socket.WebSocketHandler {

        @Override
        public void afterConnectionEstablished(WebSocketSession session) {
            log.info("WebSocket connection established");
        }

        @Override
        public void handleMessage(WebSocketSession session, org.springframework.web.socket.WebSocketMessage<?> message) throws Exception {
            try {
                if (message instanceof org.springframework.web.socket.TextMessage) {
                    JsonNode payload = objectMapper.readTree(((org.springframework.web.socket.TextMessage) message).getPayload());
                    processMessage(payload);
                }
            } catch (Exception e) {
                log.error("Error processing WebSocket message: {}", e.getMessage());
            }
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) {
            log.error("WebSocket transport error: {}", exception.getMessage());
            // Attempt to reconnect
            try {
                Thread.sleep(5000);
                connectWebSocket();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
            log.warn("WebSocket connection closed: {}", closeStatus);
            webSocketSession = null;
        }

        @Override
        public boolean supportsPartialMessages() {
            return false;
        }

        /**
         * Process incoming WebSocket message and update quotes.
         */
        private void processMessage(JsonNode payload) {
            try {
                if (payload.has("type") && "quote".equals(payload.get("type").asText())) {
                    JsonNode data = payload.get("data");
                    if (data != null) {
                        LiveQuote quote = parseQuote(data);
                        String key = quote.getSymbol() + ":" + quote.getToken();

                        latestQuotes.put(key, quote);

                        // Notify subscribers
                        Consumer<LiveQuote> callback = subscribers.get(key);
                        if (callback != null) {
                            callback.accept(quote);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error processing quote message: {}", e.getMessage());
            }
        }

        /**
         * Parse quote data from WebSocket message.
         */
        private LiveQuote parseQuote(JsonNode data) {
            return LiveQuote.builder()
                    .symbol(data.get("symbol").asText())
                    .token(data.get("token").asText())
                    .ltp(data.get("ltp").decimalValue())
                    .open(data.get("open").decimalValue())
                    .high(data.get("high").decimalValue())
                    .low(data.get("low").decimalValue())
                    .close(data.get("close").decimalValue())
                    .bidPrice(data.get("bidprice").decimalValue())
                    .bidQuantity(data.get("bidqty").asInt())
                    .askPrice(data.get("askprice").decimalValue())
                    .askQuantity(data.get("askqty").asInt())
                    .volume(data.get("volume").asLong())
                    .timestamp(data.get("timestamp").asLong())
                    .build();
        }
    }

    /**
     * Clean up WebSocket connection on shutdown.
     */
    @PreDestroy
    public void disconnect() {
        try {
            if (webSocketSession != null && webSocketSession.isOpen()) {
                webSocketSession.close();
                log.info("WebSocket connection closed");
            }
        } catch (Exception e) {
            log.error("Error closing WebSocket connection: {}", e.getMessage());
        }
    }
}
