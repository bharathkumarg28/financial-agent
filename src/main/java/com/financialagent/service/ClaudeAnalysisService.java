package com.financialagent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financialagent.config.AnthropicConfig;
import com.financialagent.dto.CandleData;
import com.financialagent.dto.OptionGreekResponse;
import com.financialagent.dto.StockAnalysisResult;
import com.financialagent.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

/**
 * AI-powered stock analysis using Claude (Anthropic API).
 * Equivalent to agent.py and agent_with_options.py in the Python version.
 */
@Service
@RequiredArgsConstructor
public class ClaudeAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ClaudeAnalysisService.class);

    private final AnthropicConfig anthropicConfig;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private static final String BASIC_SYSTEM_PROMPT = """
            You are a financial analyst specializing in Indian equities (NSE/BSE).
            
            You analyze stock price data and provide clear, actionable technical analysis. You are
            practical and direct — no fluff, no hedging every sentence with disclaimers. Give your
            honest read of what the data shows.
            
            When given OHLCV data, you analyze:
            - Price trend (direction, momentum, key patterns)
            - Support and resistance levels (based on price action, not arbitrary round numbers)
            - Volume patterns (is volume confirming or diverging from price?)
            - Any notable patterns (breakouts, consolidations, reversals)
            
            You MUST respond with ONLY a valid JSON object. No markdown, no code fences, no explanation
            outside the JSON. The JSON must have exactly these fields:
            
            {
                "trend": "bullish" | "bearish" | "sideways",
                "support": <number — key support level>,
                "resistance": <number — key resistance level>,
                "avg_volume": <number — average daily volume>,
                "volume_trend": "increasing" | "decreasing" | "stable",
                "summary": "<3-4 sentences of plain English analysis>",
                "confidence": "high" | "medium" | "low"
            }""";

    private static final String OPTIONS_SYSTEM_PROMPT = """
            You are a financial analyst specializing in Indian equities and derivatives (NSE F&O).
            
            You analyze both stock price data AND options chain data to give a complete picture. You are
            practical and direct — you say what the data shows without excessive hedging.
            
            When given OHLCV data and an options chain, you cross-reference:
            - Price trend from the stock data
            - What the options market is pricing in (from IV, OI distribution, put/call ratios)
            - Whether options sentiment confirms or contradicts the price action
            - Key support/resistance implied by max pain or high OI strikes
            
            You MUST respond with ONLY a valid JSON object. No markdown, no code fences, no text outside
            the JSON. The JSON must have exactly these fields:
            
            {
                "trend": "bullish" | "bearish" | "sideways",
                "support": <number>,
                "resistance": <number>,
                "avg_volume": <number>,
                "volume_trend": "increasing" | "decreasing" | "stable",
                "options_sentiment": "bullish" | "bearish" | "neutral",
                "iv_percentile": "<description of where current IV sits — e.g., 'elevated', 'low', 'average'>",
                "pcr_signal": "<what the put/call ratio suggests>",
                "max_pain": <number — the max pain strike if identifiable, otherwise null>,
                "summary": "<3-4 sentences covering both price action and options analysis>",
                "options_insight": "<2-3 sentences specifically about what the options market is telling us>",
                "confidence": "high" | "medium" | "low"
            }""";

    /**
     * Analyze stock using price data only.
     * Equivalent to agent.py analyze_stock function.
     */
    public StockAnalysisResult analyzeStock(List<CandleData> candles, String symbol) {
        if (!anthropicConfig.isValid()) {
            throw new ApiException("ANTHROPIC_API_KEY is not set in configuration");
        }

        String dataTable = formatCandlesForPrompt(candles, symbol);
        String userMessage = String.format(
                "Here is the recent daily OHLCV data for %s:\n\n%s\n\n" +
                        "Analyze this data. What's the trend? Where are support and resistance? " +
                        "What's volume telling us? Give me your read.",
                symbol, dataTable);

        log.info("Sending data to Claude for analysis...");

        return callClaudeApi(BASIC_SYSTEM_PROMPT, userMessage);
    }

    /**
     * Analyze stock using both price data and options chain.
     * Equivalent to agent_with_options.py analyze_with_options function.
     */
    public StockAnalysisResult analyzeWithOptions(List<CandleData> candles,
                                                  OptionGreekResponse.StrikeInfo[] optionsData,
                                                  String symbol) {
        if (!anthropicConfig.isValid()) {
            throw new ApiException("ANTHROPIC_API_KEY is not set in configuration");
        }

        String priceTable = formatCandlesForPrompt(candles, symbol);
        String userMessage;

        if (optionsData != null && optionsData.length > 0) {
            String optionsTable = formatOptionsForPrompt(optionsData);
            userMessage = String.format(
                    "Here is the recent daily OHLCV data for %s:\n\n%s\n\n" +
                            "And here is the current option chain:\n\n%s\n\n" +
                            "Cross-reference the stock's price action with what the options market is pricing in. " +
                            "What's the trend? Do options confirm or contradict? Where's the smart money positioned?",
                    symbol, priceTable, optionsTable);
        } else {
            userMessage = String.format(
                    "Here is the recent daily OHLCV data for %s:\n\n%s\n\n" +
                            "Option chain data was not available for this stock. " +
                            "Analyze based on price action only. Set options-related fields to null or 'N/A'.\n\n" +
                            "What's the trend? Where are support and resistance?",
                    symbol, priceTable);
        }

        log.info("Sending data to Claude for analysis (with options)...");

        return callClaudeApi(OPTIONS_SYSTEM_PROMPT, userMessage);
    }

    /**
     * Calls Claude API and parses the response.
     */
    private StockAnalysisResult callClaudeApi(String systemPrompt, String userMessage) {
        WebClient client = webClientBuilder
                .baseUrl(ANTHROPIC_API_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("x-api-key", anthropicConfig.getApiKey())
                .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
                .build();

        ClaudeRequest request = new ClaudeRequest(
                anthropicConfig.getModel(),
                anthropicConfig.getMaxTokens(),
                systemPrompt,
                new ClaudeMessage[]{new ClaudeMessage("user", userMessage)}
        );

        try {
            String responseBody = client.post()
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseClaudeResponse(responseBody);

        } catch (WebClientResponseException.Unauthorized e) {
            log.error("Anthropic API key is invalid. Check ANTHROPIC_API_KEY in configuration");
            throw new ApiException("Anthropic API key is invalid", e);
        } catch (WebClientResponseException.TooManyRequests e) {
            log.error("Anthropic rate limit hit. Wait a moment and try again.");
            throw new ApiException("Anthropic rate limit hit", e);
        } catch (WebClientResponseException e) {
            log.error("Claude API call failed: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ApiException("Claude API call failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Claude API call failed: {}", e.getMessage());
            throw new ApiException("Claude API call failed: " + e.getMessage(), e);
        }
    }

    /**
     * Parses Claude's response and extracts the JSON analysis.
     */
    private StockAnalysisResult parseClaudeResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode content = root.path("content");

            if (content.isArray() && content.size() > 0) {
                String rawText = content.get(0).path("text").asText().trim();

                // Handle cases where Claude wraps JSON in code fences despite instructions
                if (rawText.startsWith("```")) {
                    int firstNewline = rawText.indexOf('\n');
                    if (firstNewline > 0) {
                        rawText = rawText.substring(firstNewline + 1);
                    }
                    int lastFence = rawText.lastIndexOf("```");
                    if (lastFence > 0) {
                        rawText = rawText.substring(0, lastFence);
                    }
                    rawText = rawText.trim();
                }

                return objectMapper.readValue(rawText, StockAnalysisResult.class);
            }

            throw new ApiException("Claude returned unexpected response format");

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse Claude response: {}", e.getMessage());
            throw new ApiException("Failed to parse Claude response: " + e.getMessage(), e);
        }
    }

    /**
     * Format candle data as a readable text table for Claude.
     */
    private String formatCandlesForPrompt(List<CandleData> candles, String symbol) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Stock: %s (NSE) — Last %d trading days\n\n", symbol, candles.size()));
        sb.append(String.format("%-12s %10s %10s %10s %10s %12s\n",
                "Date", "Open", "High", "Low", "Close", "Volume"));
        sb.append("─".repeat(68)).append("\n");

        for (CandleData candle : candles) {
            sb.append(String.format("%-12s %10.2f %10.2f %10.2f %10.2f %,12d\n",
                    candle.getDateString(),
                    candle.getOpen(),
                    candle.getHigh(),
                    candle.getLow(),
                    candle.getClose(),
                    candle.getVolume()));
        }

        return sb.toString();
    }

    /**
     * Format option chain data as a readable table for Claude.
     */
    private String formatOptionsForPrompt(OptionGreekResponse.StrikeInfo[] optionsData) {
        StringBuilder sb = new StringBuilder();
        sb.append("Option Chain (nearest expiry):\n\n");
        sb.append(String.format("%10s │ %10s %8s %9s │ %10s %8s %9s\n",
                "Strike", "CE OI", "CE IV", "CE Delta", "PE OI", "PE IV", "PE Delta"));
        sb.append("─".repeat(80)).append("\n");

        for (OptionGreekResponse.StrikeInfo strike : optionsData) {
            OptionGreekResponse.LegInfo call = strike.getCallLegSafe();
            OptionGreekResponse.LegInfo put = strike.getPutLegSafe();

            sb.append(String.format("%10.2f │ %,10d %8.2f %9.4f │ %,10d %8.2f %9.4f\n",
                    strike.getStrike(),
                    call.getOiValue(),
                    call.getIvValue(),
                    call.getDeltaValue(),
                    put.getOiValue(),
                    put.getIvValue(),
                    put.getDeltaValue()));
        }

        return sb.toString();
    }

    // Inner classes for Claude API request
    private record ClaudeRequest(String model, int max_tokens, String system, ClaudeMessage[] messages) {
    }

    private record ClaudeMessage(String role, String content) {
    }
}
