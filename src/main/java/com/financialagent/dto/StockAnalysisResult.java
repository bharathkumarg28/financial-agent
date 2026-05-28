package com.financialagent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Structured analysis result from Claude AI for financial-agent-india.
 * Contains trend analysis, support/resistance, volume info, and summary.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class StockAnalysisResult {

    @JsonProperty("trend")
    private String trendDirection;

    @JsonProperty("support")
    private BigDecimal supportLevel;

    @JsonProperty("resistance")
    private BigDecimal resistanceLevel;

    @JsonProperty("avg_volume")
    private Long averageVolume;

    @JsonProperty("volume_trend")
    private String volumeTrend;

    @JsonProperty("summary")
    private String analysisSummary;

    @JsonProperty("confidence")
    private String confidenceLevel;

    // Extended fields for options analysis
    @JsonProperty("options_sentiment")
    private String optionsSentiment;

    @JsonProperty("iv_percentile")
    private String ivPercentile;

    @JsonProperty("pcr_signal")
    private String pcrSignal;

    @JsonProperty("max_pain")
    private BigDecimal maxPainStrike;

    @JsonProperty("options_insight")
    private String optionsInsight;

    public String getTrendUpper() {
        return trendDirection != null ? trendDirection.toUpperCase() : "N/A";
    }

    public String getConfidenceCapitalized() {
        if (confidenceLevel == null) return "N/A";
        return confidenceLevel.substring(0, 1).toUpperCase() + confidenceLevel.substring(1).toLowerCase();
    }

    public String getVolumeTrendCapitalized() {
        if (volumeTrend == null) return "N/A";
        return volumeTrend.substring(0, 1).toUpperCase() + volumeTrend.substring(1).toLowerCase();
    }
}
