package com.financialagent.service;

import com.financialagent.entity.Watchlist;
import com.financialagent.exception.DuplicateResourceException;
import com.financialagent.exception.ResourceNotFoundException;
import com.financialagent.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for watchlist operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final UserService userService;
    private final MarketDataService marketDataService;

    /**
     * Get the current authenticated user's ID.
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User not authenticated");
        }
        String username = authentication.getName();
        return userService.getUserByUsername(username).getId();
    }

    /**
     * Add a stock to the user's watchlist.
     */
    @Transactional
    public Watchlist addToWatchlist(String symbol, String token, String exchange, String name) {
        Long userId = getCurrentUserId();
        
        log.info("Adding stock {} to watchlist for user {}", symbol, userId);

        // Check if stock already exists in watchlist
        if (watchlistRepository.existsByUserIdAndSymbol(userId, symbol)) {
            throw new DuplicateResourceException("Stock " + symbol + " is already in your watchlist");
        }

        // Create new watchlist entry
        Watchlist watchlist = Watchlist.builder()
                .userId(userId)
                .symbol(symbol)
                .token(token)
                .exchange(exchange != null ? exchange : "NSE")
                .name(name)
                .alertEnabled(false)
                .build();

        Watchlist saved = watchlistRepository.save(watchlist);
        log.info("Successfully added {} to watchlist", symbol);
        
        return saved;
    }

    /**
     * Remove a stock from the user's watchlist.
     */
    @Transactional
    public void removeFromWatchlist(String symbol) {
        Long userId = getCurrentUserId();
        
        log.info("Removing stock {} from watchlist for user {}", symbol, userId);

        if (!watchlistRepository.existsByUserIdAndSymbol(userId, symbol)) {
            throw new ResourceNotFoundException("Stock " + symbol + " not found in your watchlist");
        }

        watchlistRepository.deleteByUserIdAndSymbol(userId, symbol);
        log.info("Successfully removed {} from watchlist", symbol);
    }

    /**
     * Get all stocks in the user's watchlist.
     */
    public List<Watchlist> getUserWatchlist() {
        Long userId = getCurrentUserId();
        List<Watchlist> watchlist = watchlistRepository.findByUserIdOrderByCreatedAtDesc(userId);
        log.debug("Retrieved {} watchlist items for user {}", watchlist.size(), userId);
        return watchlist;
    }

    /**
     * Get a specific stock from the user's watchlist.
     */
    public Watchlist getWatchlistItem(String symbol) {
        Long userId = getCurrentUserId();
        return watchlistRepository.findByUserIdAndSymbol(userId, symbol)
                .orElseThrow(() -> new ResourceNotFoundException("Stock " + symbol + " not found in your watchlist"));
    }

    /**
     * Update watchlist item notes and preferences.
     */
    @Transactional
    public Watchlist updateWatchlistItem(String symbol, String notes, BigDecimal targetPrice, 
                                         BigDecimal stopLoss, Boolean alertEnabled) {
        Long userId = getCurrentUserId();
        
        Watchlist watchlist = watchlistRepository.findByUserIdAndSymbol(userId, symbol)
                .orElseThrow(() -> new ResourceNotFoundException("Stock " + symbol + " not found in your watchlist"));

        if (notes != null) {
            watchlist.setNotes(notes);
        }
        if (targetPrice != null) {
            watchlist.setTargetPrice(targetPrice);
        }
        if (stopLoss != null) {
            watchlist.setStopLoss(stopLoss);
        }
        if (alertEnabled != null) {
            watchlist.setAlertEnabled(alertEnabled);
        }

        Watchlist updated = watchlistRepository.save(watchlist);
        log.info("Updated watchlist item for {}", symbol);
        
        return updated;
    }

    /**
     * Update price information for a watchlist item.
     * This is called by the price update scheduler.
     */
    @Transactional
    public int updatePriceInfo(Long watchlistId, BigDecimal lastPrice, BigDecimal changePercent,
                               BigDecimal dayHigh, BigDecimal dayLow, Long volume) {
        log.debug("Updating price info for watchlist item {}", watchlistId);
        
        int updated = watchlistRepository.updatePriceInfo(
                watchlistId, lastPrice, changePercent, dayHigh, dayLow, volume, LocalDateTime.now()
        );
        
        if (updated == 0) {
            log.warn("No watchlist item found with id {}", watchlistId);
        }
        
        return updated;
    }

    /**
     * Get watchlist count for the current user.
     */
    public long getWatchlistCount() {
        Long userId = getCurrentUserId();
        return watchlistRepository.countByUserId(userId);
    }

    /**
     * Get watchlist items with alerts enabled.
     */
    public List<Watchlist> getAlertEnabledItems() {
        Long userId = getCurrentUserId();
        return watchlistRepository.findByUserIdAndAlertEnabledTrue(userId);
    }

    /**
     * Get all watchlist items with alerts enabled across all users (for scheduler).
     */
    public List<Watchlist> getAllAlertEnabledItems() {
        return watchlistRepository.findByAlertEnabledTrue();
    }

    /**
     * Refresh price for a single watchlist item.
     */
    @Transactional
    public void refreshSingleItemPrice(Watchlist item) {
        try {
            var candles = marketDataService.fetchCandles(
                    item.getSymbol(),
                    item.getToken(),
                    item.getExchange(),
                    MarketDataService.CandleInterval.ONE_DAY,
                    1
            );

            if (!candles.isEmpty()) {
                var latestCandle = candles.get(0);

                BigDecimal changePercent = BigDecimal.ZERO;
                if (candles.size() > 1) {
                    BigDecimal prevClose = candles.get(1).getClose();
                    BigDecimal currentClose = latestCandle.getClose();
                    if (prevClose.compareTo(BigDecimal.ZERO) > 0) {
                        changePercent = currentClose.subtract(prevClose)
                                .divide(prevClose, 2, BigDecimal.ROUND_HALF_UP)
                                .multiply(BigDecimal.valueOf(100));
                    }
                }

                updatePriceInfo(
                        item.getId(),
                        latestCandle.getClose(),
                        changePercent,
                        latestCandle.getHigh(),
                        latestCandle.getLow(),
                        latestCandle.getVolume()
                );
            }
        } catch (Exception e) {
            log.error("Failed to refresh price for {}: {}", item.getSymbol(), e.getMessage(), e);
        }
    }

    /**
     * Get watchlist item by ID.
     */
    public Watchlist getWatchlistItemById(Long id) {
        return watchlistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Watchlist item not found with id: " + id));
    }

    /**
     * Save watchlist item.
     */
    @Transactional
    public Watchlist saveWatchlistItem(Watchlist watchlist) {
        return watchlistRepository.save(watchlist);
    }

    /**
     * Refresh prices for all items in the user's watchlist.
     * This fetches the latest candle data and updates the price information.
     */
    @Transactional
    public void refreshWatchlistPrices() {
        // Check if AngelOne session is authenticated
        boolean isAuthenticated = marketDataService.getSessionService().isAuthenticated();
        log.info("AngelOne authentication status: {}", isAuthenticated);
        
        if (!isAuthenticated) {
            log.warn("AngelOne session not authenticated. Skipping price refresh. Please connect to AngelOne first.");
            return;
        }

        Long userId = getCurrentUserId();
        List<Watchlist> watchlist = watchlistRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        log.info("Refreshing prices for {} watchlist items", watchlist.size());
        
        for (Watchlist item : watchlist) {
            log.info("Fetching candle data for symbol={}, token={}, exchange={}", 
                    item.getSymbol(), item.getToken(), item.getExchange());
            
            try {
                // Fetch latest candle data (1 day interval)
                var candles = marketDataService.fetchCandles(
                        item.getSymbol(), 
                        item.getToken(), 
                        item.getExchange(), 
                        MarketDataService.CandleInterval.ONE_DAY, 
                        1
                );
                
                log.info("Fetched {} candles for {}", candles.size(), item.getSymbol());
                
                if (!candles.isEmpty()) {
                    var latestCandle = candles.get(0);
                    log.info("Latest candle data for {}: open={}, high={}, low={}, close={}, volume={}", 
                            item.getSymbol(), latestCandle.getOpen(), latestCandle.getHigh(), 
                            latestCandle.getLow(), latestCandle.getClose(), latestCandle.getVolume());
                    
                    // Calculate change percentage (using close price)
                    BigDecimal changePercent = BigDecimal.ZERO;
                    if (candles.size() > 1) {
                        BigDecimal prevClose = candles.get(1).getClose();
                        BigDecimal currentClose = latestCandle.getClose();
                        if (prevClose.compareTo(BigDecimal.ZERO) > 0) {
                            changePercent = currentClose.subtract(prevClose)
                                    .divide(prevClose, 2, BigDecimal.ROUND_HALF_UP)
                                    .multiply(BigDecimal.valueOf(100));
                        }
                    }
                    
                    // Update price information
                    int updated = updatePriceInfo(
                            item.getId(),
                            latestCandle.getClose(),
                            changePercent,
                            latestCandle.getHigh(),
                            latestCandle.getLow(),
                            latestCandle.getVolume()
                    );
                    
                    log.info("Database update result for {}: {} rows affected", item.getSymbol(), updated);
                    log.info("Successfully updated price for {}: close={}, high={}, low={}, volume={}", 
                            item.getSymbol(), latestCandle.getClose(), latestCandle.getHigh(), 
                            latestCandle.getLow(), latestCandle.getVolume());
                } else {
                    log.warn("No candle data returned for {}", item.getSymbol());
                }
            } catch (Exception e) {
                log.error("Failed to update price for {}: {}", item.getSymbol(), e.getMessage(), e);
            }
        }
        
        log.info("Completed price refresh for watchlist");
    }
}
