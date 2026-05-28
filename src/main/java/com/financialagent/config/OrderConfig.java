package com.financialagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for order placement safety.
 */
@Data
@ConfigurationProperties(prefix = "order")
public class OrderConfig {

    /**
     * When true, no real orders are placed - only simulated.
     * Default is true for safety.
     */
    private boolean dryRun = true;

    // Getter method as workaround for Lombok annotation processing issue
    public boolean isDryRun() {
        return dryRun;
    }
}
