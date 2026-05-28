package com.financialagent.controller;

import com.financialagent.dto.ApiResponse;
import com.financialagent.dto.CandleData;
import com.financialagent.dto.OptionGreekResponse;
import com.financialagent.dto.StockAnalysisResult;
import com.financialagent.service.ClaudeAnalysisService;
import com.financialagent.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for AI-powered stock analysis.
 * Equivalent to agent.py and agent_with_options.py functionality.
 */
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final MarketDataService marketDataService;
    private final ClaudeAnalysisService claudeAnalysisService;

    /**
     * Analyze a stock using price data only.
     * Equivalent to running agent.py
     *
     * @param symbol Stock symbol (default: RELIANCE)
     * @param token  SmartAPI symbol token (default: 2885)
     */
    @PostMapping("/basic")
    public ResponseEntity<ApiResponse<AnalysisResponse>> analyzeBasic(
            @RequestParam(defaultValue = "RELIANCE") String symbol,
            @RequestParam(defaultValue = "2885") String token) {

        try {
            // Fetch candle data
            List<CandleData> candles = marketDataService.fetchCandles(symbol, token);

            if (candles.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.fail("No data to analyze. " +
                        "Check your symbol token or try again later."));
            }

            // Analyze with Claude
            StockAnalysisResult analysis = claudeAnalysisService.analyzeStock(candles, symbol);

            AnalysisResponse response = new AnalysisResponse(
                    symbol,
                    token,
                    candles.size(),
                    false,
                    analysis
            );

            return ResponseEntity.ok(ApiResponse.ok(response,
                    String.format("Analysis complete for %s", symbol)));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("Analysis failed", e.getMessage()));
        }
    }

    /**
     * Analyze a stock using both price data and options chain.
     * Equivalent to running agent_with_options.py
     *
     * @param symbol Stock symbol (default: RELIANCE)
     * @param token  SmartAPI symbol token (default: 2885)
     */
    @PostMapping("/with-options")
    public ResponseEntity<ApiResponse<AnalysisResponse>> analyzeWithOptions(
            @RequestParam(defaultValue = "RELIANCE") String symbol,
            @RequestParam(defaultValue = "2885") String token) {

        try {
            // Fetch candle data
            List<CandleData> candles = marketDataService.fetchCandles(symbol, token);

            if (candles.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.fail("No price data to analyze."));
            }

            // Fetch option chain (may be null if stock doesn't have F&O)
            OptionGreekResponse.StrikeInfo[] optionsData = marketDataService.fetchOptionChain(symbol);

            // Analyze with Claude
            StockAnalysisResult analysis = claudeAnalysisService.analyzeWithOptions(
                    candles, optionsData, symbol);

            AnalysisResponse response = new AnalysisResponse(
                    symbol,
                    token,
                    candles.size(),
                    optionsData != null && optionsData.length > 0,
                    analysis
            );

            return ResponseEntity.ok(ApiResponse.ok(response,
                    String.format("Analysis complete for %s (with options: %s)",
                            symbol, optionsData != null)));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("Analysis failed", e.getMessage()));
        }
    }

    /**
     * Quick analysis endpoint - same as basic but with GET for easy testing.
     */
    @GetMapping("/quick")
    public ResponseEntity<ApiResponse<AnalysisResponse>> quickAnalysis(
            @RequestParam(defaultValue = "RELIANCE") String symbol,
            @RequestParam(defaultValue = "2885") String token,
            @RequestParam(defaultValue = "false") boolean includeOptions) {

        if (includeOptions) {
            return analyzeWithOptions(symbol, token);
        }
        return analyzeBasic(symbol, token);
    }

    // Response record
    public record AnalysisResponse(
            String symbol,
            String token,
            int candlesAnalyzed,
            boolean optionsIncluded,
            StockAnalysisResult analysis
    ) {
    }
}
