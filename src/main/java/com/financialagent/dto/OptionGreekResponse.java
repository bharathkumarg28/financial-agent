package com.financialagent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * AngelOne SmartAPI option greek response for financial-agent-india project.
 * Maps the option chain data including strikes, OI, IV, and Greeks.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OptionGreekResponse {

    private boolean status;
    private String message;
    private String errorcode;
    private List<StrikeInfo> data;

    // Getter methods as workaround for Lombok annotation processing issue
    public boolean isStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public List<StrikeInfo> getData() {
        return data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StrikeInfo {

        @JsonProperty("strikePrice")
        private BigDecimal strike;

        @JsonProperty("CE")
        private LegInfo callLeg;

        @JsonProperty("PE")
        private LegInfo putLeg;

        // Getter method as workaround for Lombok annotation processing issue
        public BigDecimal getStrike() {
            return strike;
        }

        public LegInfo getCallLegSafe() {
            return callLeg != null ? callLeg : new LegInfo();
        }

        public LegInfo getPutLegSafe() {
            return putLeg != null ? putLeg : new LegInfo();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LegInfo {

        @JsonProperty("openInterest")
        private Long oi;

        @JsonProperty("impliedVolatility")
        private BigDecimal iv;

        @JsonProperty("delta")
        private BigDecimal deltaVal;

        @JsonProperty("gamma")
        private BigDecimal gammaVal;

        @JsonProperty("theta")
        private BigDecimal thetaVal;

        @JsonProperty("vega")
        private BigDecimal vegaVal;

        @JsonProperty("ltp")
        private BigDecimal lastPrice;

        public long getOiValue() {
            return oi != null ? oi : 0L;
        }

        public BigDecimal getIvValue() {
            return iv != null ? iv : BigDecimal.ZERO;
        }

        public BigDecimal getDeltaValue() {
            return deltaVal != null ? deltaVal : BigDecimal.ZERO;
        }
    }
}
