package com.financialagent.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Watchlist entity for storing user's stock watchlist.
 */
@Entity
@Table(name = "watchlist", indexes = {
        @Index(name = "idx_watchlist_user", columnList = "user_id"),
        @Index(name = "idx_watchlist_symbol", columnList = "symbol"),
        @Index(name = "idx_watchlist_user_symbol", columnList = "user_id, symbol", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Watchlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String symbol;

    @Column(length = 50)
    private String token;

    @Column(length = 20)
    private String exchange;

    @Column(length = 100)
    private String name;

    // Price information (cached from API)
    @Column(precision = 10, scale = 2)
    private BigDecimal lastPrice;

    @Column(precision = 5, scale = 2)
    private BigDecimal changePercent;

    @Column(precision = 10, scale = 2)
    private BigDecimal dayHigh;

    @Column(precision = 10, scale = 2)
    private BigDecimal dayLow;

    @Column(precision = 15, scale = 2)
    private Long volume;

    // User preferences
    @Column(name = "target_price", precision = 10, scale = 2)
    private BigDecimal targetPrice;

    @Column(name = "stop_loss", precision = 10, scale = 2)
    private BigDecimal stopLoss;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "alert_enabled")
    @Builder.Default
    private Boolean alertEnabled = false;

    // Audit fields
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_price_update")
    private LocalDateTime lastPriceUpdate;
}
