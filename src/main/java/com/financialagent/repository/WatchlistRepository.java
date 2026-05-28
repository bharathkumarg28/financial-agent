package com.financialagent.repository;

import com.financialagent.entity.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Watchlist entity operations.
 */
@Repository
public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {

    /**
     * Find all watchlist items for a specific user.
     */
    List<Watchlist> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find a specific watchlist item by user and symbol.
     */
    Optional<Watchlist> findByUserIdAndSymbol(Long userId, String symbol);

    /**
     * Check if a stock is already in user's watchlist.
     */
    boolean existsByUserIdAndSymbol(Long userId, String symbol);

    /**
     * Delete a watchlist item by user and symbol.
     */
    void deleteByUserIdAndSymbol(Long userId, String symbol);

    /**
     * Update price information for a watchlist item.
     */
    @Modifying
    @Query("UPDATE Watchlist w SET w.lastPrice = :lastPrice, w.changePercent = :changePercent, " +
            "w.dayHigh = :dayHigh, w.dayLow = :dayLow, w.volume = :volume, " +
            "w.lastPriceUpdate = :lastPriceUpdate WHERE w.id = :id")
    int updatePriceInfo(@Param("id") Long id, 
                       @Param("lastPrice") java.math.BigDecimal lastPrice,
                       @Param("changePercent") java.math.BigDecimal changePercent,
                       @Param("dayHigh") java.math.BigDecimal dayHigh,
                       @Param("dayLow") java.math.BigDecimal dayLow,
                       @Param("volume") Long volume,
                       @Param("lastPriceUpdate") java.time.LocalDateTime lastPriceUpdate);

    /**
     * Count watchlist items for a user.
     */
    long countByUserId(Long userId);

    /**
     * Find watchlist items with alerts enabled.
     */
    List<Watchlist> findByUserIdAndAlertEnabledTrue(Long userId);

    /**
     * Find all watchlist items with alerts enabled across all users.
     */
    List<Watchlist> findByAlertEnabledTrue();
}
