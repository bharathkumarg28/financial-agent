package com.financialagent.controller;

import com.financialagent.dto.ApiResponse;
import com.financialagent.dto.CandleData;
import com.financialagent.dto.LiveQuote;
import com.financialagent.dto.OptionGreekResponse;
import com.financialagent.service.LiveQuoteService;
import com.financialagent.service.MarketDataService;
import com.financialagent.service.MarketDataService.HolidayInfo;
import com.financialagent.service.MarketDataService.SymbolInfo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for fetching market data.
 * Equivalent to fetch_data.py functionality.
 */
@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketDataController {

    private static final Logger log = LoggerFactory.getLogger(MarketDataController.class);

    private final MarketDataService marketDataService;
    private final LiveQuoteService liveQuoteService;

    /**
     * Fetch historical candle data for a stock.
     *
     * @param symbol Stock symbol (default: RELIANCE)
     * @param token  SmartAPI symbol token (default: 2885)
     * @param days   Number of days of history (default: 30)
     */
    @GetMapping("/candles")
    public ResponseEntity<ApiResponse<CandleDataResponse>> getCandles(
            @RequestParam(defaultValue = "RELIANCE") String symbol,
            @RequestParam(defaultValue = "2885") String token,
            @RequestParam(defaultValue = "30") int days) {

        try {
            List<CandleData> candles = marketDataService.fetchCandles(symbol, token, days);

            if (candles.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.fail("No candle data returned. " +
                        "The symbol token might be wrong, or the market might be closed."));
            }

            CandleDataResponse response = new CandleDataResponse(
                    symbol,
                    token,
                    days,
                    candles.size(),
                    candles
            );

            return ResponseEntity.ok(ApiResponse.ok(response,
                    String.format("Fetched %d candles for %s", candles.size(), symbol)));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("Failed to fetch candle data", e.getMessage()));
        }
    }

    /**
     * Fetch option chain data for a stock.
     *
     * @param symbol Stock symbol (default: RELIANCE)
     */
    @GetMapping("/options")
    public ResponseEntity<ApiResponse<OptionChainDataResponse>> getOptionChain(
            @RequestParam(defaultValue = "RELIANCE") String symbol) {

        try {
            OptionGreekResponse.StrikeInfo[] optionData = marketDataService.fetchOptionChain(symbol);

            if (optionData == null || optionData.length == 0) {
                return ResponseEntity.ok(ApiResponse.fail(
                        "No option chain data returned. The stock might not have F&O contracts."));
            }

            OptionChainDataResponse response = new OptionChainDataResponse(
                    symbol,
                    optionData.length,
                    optionData
            );

            return ResponseEntity.ok(ApiResponse.ok(response,
                    String.format("Fetched option chain with %d strikes for %s",
                            optionData.length, symbol)));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("Failed to fetch option chain", e.getMessage()));
        }
    }

    /**
     * Get formatted candle data as text table.
     */
    @GetMapping("/candles/formatted")
    public ResponseEntity<ApiResponse<String>> getFormattedCandles(
            @RequestParam(defaultValue = "RELIANCE") String symbol,
            @RequestParam(defaultValue = "2885") String token,
            @RequestParam(defaultValue = "30") int days) {

        try {
            List<CandleData> candles = marketDataService.fetchCandles(symbol, token, days);

            if (candles.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.fail("No candle data returned."));
            }

            String formattedTable = marketDataService.formatCandlesAsTable(candles, symbol);
            return ResponseEntity.ok(ApiResponse.ok(formattedTable));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("Failed to fetch candle data", e.getMessage()));
        }
    }

    /**
     * Get all available symbols and their tokens.
     * Returns a curated list of commonly traded NSE symbols.
     */
    @GetMapping("/symbols")
    public ResponseEntity<ApiResponse<List<SymbolInfo>>> getAllSymbols() {
        try {
            List<SymbolInfo> symbols = marketDataService.getAllSymbols();
            return ResponseEntity.ok(ApiResponse.ok(symbols,
                    String.format("Fetched %d symbols", symbols.size())));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("Failed to fetch symbols", e.getMessage()));
        }
    }

    /**
     * Get market holidays for a specific year.
     * Returns a list of Indian market holidays (NSE/BSE).
     */
    @GetMapping("/holidays")
    public ResponseEntity<ApiResponse<List<HolidayInfo>>> getMarketHolidays(
            @RequestParam(defaultValue = "2024") int year) {
        try {
            List<HolidayInfo> holidays = marketDataService.getMarketHolidays(year);
            return ResponseEntity.ok(ApiResponse.ok(holidays,
                    String.format("Fetched %d holidays for %d", holidays.size(), year)));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("Failed to fetch holidays", e.getMessage()));
        }
    }

    /**
     * Get live quote for a specific symbol.
     * Returns the most recent market data including LTP, OHLC, bid/ask.
     */
    @GetMapping("/live")
    public ResponseEntity<ApiResponse<LiveQuote>> getLiveQuote(
            @RequestParam(defaultValue = "RELIANCE") String symbol,
            @RequestParam(defaultValue = "2885") String token) {

        try {
            LiveQuote quote = liveQuoteService.getLatestQuote(symbol, token);
            if (quote != null) {
                quote.calculateChange();
                return ResponseEntity.ok(ApiResponse.ok(quote,
                        String.format("Live quote for %s", symbol)));
            } else {
                // If no live data available, return cached or fallback data
                LiveQuote fallbackQuote = createFallbackQuote(symbol, token);
                return ResponseEntity.ok(ApiResponse.ok(fallbackQuote,
                        String.format("Fallback quote for %s (live data unavailable)", symbol)));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("Failed to fetch live quote", e.getMessage()));
        }
    }

    /**
     * Subscribe to live quotes for multiple symbols.
     * Returns subscription confirmation.
     */
    @GetMapping("/live/subscribe")
    public ResponseEntity<ApiResponse<String>> subscribeToLiveQuotes(
            @RequestParam String symbols,
            @RequestParam String tokens) {

        try {
            String[] symbolArray = symbols.split(",");
            String[] tokenArray = tokens.split(",");

            if (symbolArray.length != tokenArray.length) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.fail("Symbols and tokens arrays must have same length"));
            }

            for (int i = 0; i < symbolArray.length; i++) {
                String symbol = symbolArray[i].trim();
                String token = tokenArray[i].trim();

                liveQuoteService.subscribe(symbol, token, quote -> {
                    // Handle live quote updates (could be stored in cache or sent via WebSocket)
                    log.debug("Received live quote update for {}: {}", symbol, quote.getLtp());
                });
            }

            return ResponseEntity.ok(ApiResponse.ok("Subscribed to live quotes",
                    String.format("Subscribed to %d symbols", symbolArray.length)));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("Failed to subscribe to live quotes", e.getMessage()));
        }
    }

    /**
     * Create a fallback quote when live data is unavailable.
     */
    private LiveQuote createFallbackQuote(String symbol, String token) {
        return LiveQuote.builder()
                .symbol(symbol)
                .token(token)
                .exchange("NSE")
                .ltp(null)
                .open(null)
                .high(null)
                .low(null)
                .close(null)
                .bidPrice(null)
                .bidQuantity(0)
                .askPrice(null)
                .askQuantity(0)
                .volume(0L)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    // Response records
    public record CandleDataResponse(
            String symbol,
            String token,
            int requestedDays,
            int candleCount,
            List<CandleData> candles
    ) {
    }

    public record OptionChainDataResponse(
            String symbol,
            int strikeCount,
            OptionGreekResponse.StrikeInfo[] strikes
    ) {
    }
}
