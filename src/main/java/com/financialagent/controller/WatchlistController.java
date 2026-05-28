package com.financialagent.controller;

import com.financialagent.dto.ApiResponse;
import com.financialagent.entity.Watchlist;
import com.financialagent.service.MarketDataService;
import com.financialagent.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST Controller for watchlist operations.
 */
@RestController
@RequestMapping("/api/watchlist")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;
    private final MarketDataService marketDataService;

    /**
     * Get all stocks in the user's watchlist.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Watchlist>>> getWatchlist() {
        // Auto-refresh prices before returning watchlist
        watchlistService.refreshWatchlistPrices();
        List<Watchlist> watchlist = watchlistService.getUserWatchlist();
        return ResponseEntity.ok(ApiResponse.ok(watchlist, "Watchlist retrieved successfully"));
    }

    /**
     * Get a specific stock from the user's watchlist.
     */
    @GetMapping("/{symbol}")
    public ResponseEntity<ApiResponse<Watchlist>> getWatchlistItem(@PathVariable String symbol) {
        Watchlist item = watchlistService.getWatchlistItem(symbol);
        return ResponseEntity.ok(ApiResponse.ok(item, "Watchlist item retrieved successfully"));
    }

    /**
     * Add a stock to the user's watchlist.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Watchlist>> addToWatchlist(
            @RequestParam String symbol,
            @RequestParam String token,
            @RequestParam(required = false) String exchange,
            @RequestParam(required = false) String name) {
        Watchlist watchlist = watchlistService.addToWatchlist(symbol, token, exchange, name);
        return ResponseEntity.ok(ApiResponse.ok(watchlist, "Stock added to watchlist successfully"));
    }

    /**
     * Remove a stock from the user's watchlist.
     */
    @DeleteMapping("/{symbol}")
    public ResponseEntity<ApiResponse<Void>> removeFromWatchlist(@PathVariable String symbol) {
        watchlistService.removeFromWatchlist(symbol);
        return ResponseEntity.ok(ApiResponse.ok(null, "Stock removed from watchlist successfully"));
    }

    /**
     * Update watchlist item notes and preferences.
     */
    @PutMapping("/{symbol}")
    public ResponseEntity<ApiResponse<Watchlist>> updateWatchlistItem(
            @PathVariable String symbol,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) BigDecimal targetPrice,
            @RequestParam(required = false) BigDecimal stopLoss,
            @RequestParam(required = false) Boolean alertEnabled) {
        Watchlist updated = watchlistService.updateWatchlistItem(symbol, notes, targetPrice, stopLoss, alertEnabled);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Watchlist item updated successfully"));
    }

    /**
     * Get watchlist count for the current user.
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getWatchlistCount() {
        long count = watchlistService.getWatchlistCount();
        return ResponseEntity.ok(ApiResponse.ok(count, "Watchlist count retrieved successfully"));
    }

    /**
     * Refresh prices for all items in the user's watchlist.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Void>> refreshPrices() {
        watchlistService.refreshWatchlistPrices();
        return ResponseEntity.ok(ApiResponse.ok(null, "Watchlist prices refreshed successfully"));
    }

    /**
     * Get available symbols from market data service.
     * This helps users find stocks to add to their watchlist.
     */
    @GetMapping("/symbols")
    public ResponseEntity<ApiResponse<List<MarketDataService.SymbolInfo>>> getAvailableSymbols() {
        List<MarketDataService.SymbolInfo> symbols = marketDataService.getAllSymbols();
        return ResponseEntity.ok(ApiResponse.ok(symbols, "Available symbols retrieved successfully"));
    }
}
