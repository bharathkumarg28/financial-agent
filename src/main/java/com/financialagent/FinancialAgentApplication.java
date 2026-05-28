package com.financialagent;

import com.financialagent.config.AngelOneConfig;
import com.financialagent.config.AnthropicConfig;
import com.financialagent.config.OrderConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Financial Agent for Indian Markets
 * <p>
 * AI-powered financial analysis agent for Indian equities using
 * AngelOne SmartAPI and Claude (Anthropic's API).
 */
@SpringBootApplication
@EnableConfigurationProperties({AngelOneConfig.class, AnthropicConfig.class, OrderConfig.class})
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
public class FinancialAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinancialAgentApplication.class, args);
    }
}
