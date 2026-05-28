package com.financialagent.service;

import com.financialagent.entity.Watchlist;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Scheduler for checking price alerts on watchlist items.
 * Runs periodically to check if any watchlist items have hit their target price or stop-loss.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertScheduler {

    private final WatchlistService watchlistService;
    private final NotificationService notificationService;
    private final MarketDataService marketDataService;

    /**
     * Check price alerts every 5 minutes during market hours.
     * Market hours: 9:15 AM to 3:30 PM IST on weekdays.
     */
    @Scheduled(cron = "0 */5 9-15 * * MON-FRI", zone = "Asia/Kolkata")
    public void checkPriceAlerts() {
        log.info("Running scheduled price alert check");
        
        try {
            // Check if AngelOne is authenticated
            if (!marketDataService.getSessionService().isAuthenticated()) {
                log.warn("AngelOne not authenticated, skipping price alert check");
                return;
            }

            // Get all watchlist items with alerts enabled across all users
            List<Watchlist> alertEnabledItems = watchlistService.getAllAlertEnabledItems();
            
            if (alertEnabledItems.isEmpty()) {
                log.debug("No watchlist items with alerts enabled");
                return;
            }

            log.info("Checking {} watchlist items with alerts enabled", alertEnabledItems.size());
            
            for (Watchlist item : alertEnabledItems) {
                checkAndTriggerAlert(item);
            }
            
        } catch (Exception e) {
            log.error("Error during scheduled price alert check: {}", e.getMessage(), e);
        }
    }

    /**
     * Check if a watchlist item has hit its target price or stop-loss and trigger alert if needed.
     */
    private void checkAndTriggerAlert(Watchlist item) {
        try {
            // Refresh price for this item
            watchlistService.refreshSingleItemPrice(item);
            
            // Reload item to get updated price
            Watchlist updatedItem = watchlistService.getWatchlistItemById(item.getId());
            
            if (updatedItem.getLastPrice() == null) {
                log.warn("No price data available for {}", item.getSymbol());
                return;
            }

            double currentPrice = updatedItem.getLastPrice().doubleValue();
            
            // Check target price
            if (updatedItem.getTargetPrice() != null) {
                double targetPrice = updatedItem.getTargetPrice().doubleValue();
                
                // Trigger alert if price reaches or exceeds target
                if (currentPrice >= targetPrice) {
                    log.info("Target price alert triggered for {}: current={}, target={}", 
                            item.getSymbol(), currentPrice, targetPrice);
                    
                    notificationService.sendPriceAlert(
                            updatedItem.getUserId(),
                            item.getSymbol(),
                            "TARGET_PRICE",
                            currentPrice,
                            targetPrice
                    );
                    
                    // Disable alert after triggering to avoid duplicate alerts
                    updatedItem.setAlertEnabled(false);
                    watchlistService.saveWatchlistItem(updatedItem);
                }
            }
            
            // Check stop-loss
            if (updatedItem.getStopLoss() != null) {
                double stopLoss = updatedItem.getStopLoss().doubleValue();
                
                // Trigger alert if price falls to or below stop-loss
                if (currentPrice <= stopLoss) {
                    log.info("Stop-loss alert triggered for {}: current={}, stop-loss={}", 
                            item.getSymbol(), currentPrice, stopLoss);
                    
                    notificationService.sendPriceAlert(
                            updatedItem.getUserId(),
                            item.getSymbol(),
                            "STOP_LOSS",
                            currentPrice,
                            stopLoss
                    );
                    
                    // Disable alert after triggering to avoid duplicate alerts
                    updatedItem.setAlertEnabled(false);
                    watchlistService.saveWatchlistItem(updatedItem);
                }
            }
            
        } catch (Exception e) {
            log.error("Error checking alert for {}: {}", item.getSymbol(), e.getMessage(), e);
        }
    }
}
