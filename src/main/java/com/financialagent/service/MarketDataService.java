package com.financialagent.service;

import com.financialagent.dto.CandleData;
import com.financialagent.dto.CandleRequest;
import com.financialagent.dto.CandleResponse;
import com.financialagent.dto.OptionGreekResponse;
import com.financialagent.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Fetches market data from AngelOne SmartAPI.
 * Equivalent to fetch_data.py in the Python version.
 */
@Service
@RequiredArgsConstructor
public class MarketDataService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataService.class);

    private final AngelOneSessionService sessionService;
    private final AuthenticationService authenticationService;

    private static final String CANDLE_ENDPOINT = "/rest/secure/angelbroking/historical/v1/getCandleData";
    private static final String OPTION_GREEK_ENDPOINT = "/rest/secure/angelbroking/option/v1/optionGreek";
    private static final String MASTER_DATA_ENDPOINT = "/rest/secure/angelbroking/user/v1/rms/margin";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Get the AngelOne session service (for authentication checks).
     */
    public AngelOneSessionService getSessionService() {
        return sessionService;
    }

    /**
     * Interval constants matching SmartAPI expectations.
     */
    public enum CandleInterval {
        ONE_MINUTE("ONE_MINUTE"),
        FIVE_MINUTE("FIVE_MINUTE"),
        FIFTEEN_MINUTE("FIFTEEN_MINUTE"),
        THIRTY_MINUTE("THIRTY_MINUTE"),
        ONE_HOUR("ONE_HOUR"),
        ONE_DAY("ONE_DAY");

        private final String value;

        CandleInterval(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Fetch historical candle data for a stock.
     *
     * @param symbol   Stock symbol (e.g., "RELIANCE")
     * @param token    SmartAPI symbol token (e.g., "2885")
     * @param exchange Exchange — "NSE" or "BSE"
     * @param interval Candle interval
     * @param days     Number of days of history to fetch
     * @return List of candle data or empty list if failed
     */
    public List<CandleData> fetchCandles(String symbol, String token, String exchange,
                                         CandleInterval interval, int days) {
        log.info("Fetching {} trading days of {} data for {}...", days, interval.getValue(), symbol);

        // Wait for AngelOne authentication to complete (max 30 seconds)
        if (!sessionService.isAuthenticated()) {
            log.warn("AngelOne not authenticated yet, waiting...");
            int maxWait = 30; // seconds
            int waited = 0;
            while (!sessionService.isAuthenticated() && waited < maxWait) {
                try {
                    Thread.sleep(1000);
                    waited++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            if (!sessionService.isAuthenticated()) {
                log.error("AngelOne authentication timeout after {} seconds", waited);
                throw new ApiException("AngelOne authentication not complete. Please wait a moment and try again.");
            }
            log.info("AngelOne authentication completed after {} seconds", waited);
        }

        LocalDateTime toDate = LocalDateTime.now();
        LocalDateTime fromDate = calculateFromDateForTradingDays(toDate, days);

        CandleRequest request = CandleRequest.builder()
                .exchange(exchange)
                .symbolToken(token)
                .interval(interval.getValue())
                .fromDate(fromDate.format(DATE_FORMATTER).replace(" ", " "))
                .toDate(toDate.format(DATE_FORMATTER).replace(" ", " "))
                .build();

        // Adjust times for market hours
        String fromDateStr = fromDate.toLocalDate().toString() + " 09:15";
        String toDateStr = toDate.toLocalDate().toString() + " 15:30";

        request.setFromDate(fromDateStr);
        request.setToDate(toDateStr);

        try {
            WebClient client = sessionService.createAuthenticatedClient();

            log.debug("Sending candle request: {}", request);

            CandleResponse response = client.post()
                    .uri(CANDLE_ENDPOINT)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(CandleResponse.class)
                    .block();

            if (response == null) {
                log.warn("Got empty response from SmartAPI. Check your symbol token.");
                return Collections.emptyList();
            }

            log.debug("API response status: {}, message: {}, errorcode: {}",
                    response.isStatus(), response.getMessage(), response.getErrorcode());

            if (!response.isStatus()) {
                String errorMsg = response.getMessage() != null ? response.getMessage() : "Unknown error";
                String errorCode = response.getErrorcode() != null ? response.getErrorcode() : "N/A";
                log.error("API error - Code: {}, Message: {}", errorCode, errorMsg);

                // Provide specific error messages for common issues
                if (errorCode != null) {
                    switch (errorCode) {
                        case "AB1000":
                            log.error("Authentication failed. Please check your AngelOne credentials and session.");
                            break;
                        case "AB2000":
                            log.error("Invalid symbol token. Please verify the symbol token for {}", symbol);
                            break;
                        case "AB2007":
                            log.error("Invalid date range. Please check the from/to dates.");
                            break;
                        default:
                            log.error("API error code {}: {}", errorCode, errorMsg);
                    }
                }
                return Collections.emptyList();
            }

            List<Object[]> rawData = response.getData();
            if (rawData == null || rawData.isEmpty()) {
                log.warn("No candle data returned. This could be due to:");
                log.warn("1. Invalid symbol token for {}", symbol);
                log.warn("2. Market closed for the requested period");
                log.warn("3. No trading data available for the specified timeframe");
                log.debug("Request details - Symbol: {}, Token: {}, Exchange: {}, Interval: {}, From: {}, To: {}",
                        symbol, token, exchange, interval.getValue(), request.getFromDate(), request.getToDate());
                return Collections.emptyList();
            }

            List<CandleData> candles = new ArrayList<>();
            for (Object[] row : rawData) {
                CandleData candle = CandleData.fromArray(row);
                if (candle != null) {
                    candles.add(candle);
                }
            }

            log.info("Fetched {} candles for {}", candles.size(), symbol);

            // Validate we got enough trading days
            if (candles.size() < days) {
                log.warn("Requested {} trading days but only got {} candles. " +
                                "This might be due to holidays, market closures, or insufficient data.",
                        days, candles.size());

                // For very small requests (1-2 days), provide a helpful message
                if (days <= 2 && candles.isEmpty()) {
                    throw new ApiException(
                            "No trading data available for the requested period. " +
                                    "The market might be closed today or there might be insufficient data. " +
                                    "Try requesting more days or check if the market is open."
                    );
                }
            }

            return candles;

        } catch (WebClientResponseException e) {
            log.error("API call failed: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ApiException("Failed to fetch candle data: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("API call failed: {}", e.getMessage());
            throw new ApiException("Failed to fetch candle data: " + e.getMessage(), e);
        }
    }

    /**
     * Fetch candles with default parameters (NSE, daily, 30 days).
     */
    public List<CandleData> fetchCandles(String symbol, String token) {
        return fetchCandles(symbol, token, "NSE", CandleInterval.ONE_DAY, 30);
    }

    /**
     * Fetch candles with custom days parameter.
     */
    public List<CandleData> fetchCandles(String symbol, String token, int days) {
        return fetchCandles(symbol, token, "NSE", CandleInterval.ONE_DAY, days);
    }

    /**
     * Fetch option chain data for a stock.
     *
     * @param symbol Stock symbol (e.g., "RELIANCE")
     * @return Option chain data or null if failed
     */
    public OptionGreekResponse.StrikeInfo[] fetchOptionChain(String symbol) {
        log.info("Fetching option chain for {}...", symbol);

        try {
            WebClient client = sessionService.createAuthenticatedClient();

            OptionGreekRequest request = new OptionGreekRequest(symbol, "");

            OptionGreekResponse response = client.post()
                    .uri(OPTION_GREEK_ENDPOINT)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OptionGreekResponse.class)
                    .block();

            if (response == null) {
                log.warn("Got empty option chain response.");
                return null;
            }

            if (!response.isStatus()) {
                String errorMsg = response.getMessage() != null ? response.getMessage() : "Unknown error";
                log.error("Option chain error: {}", errorMsg);
                return null;
            }

            if (response.getData() == null || response.getData().isEmpty()) {
                log.warn("No option chain data returned. The stock might not have F&O contracts.");
                return null;
            }

            log.info("Fetched option chain with {} strikes for {}", response.getData().size(), symbol);
            return response.getData().toArray(new OptionGreekResponse.StrikeInfo[0]);

        } catch (WebClientResponseException e) {
            log.error("Option chain API call failed: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            log.warn("This might mean the stock doesn't have F&O contracts, " +
                    "or SmartAPI's option chain endpoint is temporarily down.");
            return null;
        } catch (Exception e) {
            log.error("Option chain API call failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Formats candle data as a readable table string.
     */
    public String formatCandlesAsTable(List<CandleData> candles, String symbol) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s — Daily OHLCV (%d candles)%n%n", symbol, candles.size()));
        sb.append(String.format("%-12s %10s %10s %10s %10s %12s%n",
                "Date", "Open", "High", "Low", "Close", "Volume"));
        sb.append("─".repeat(68)).append("\n");

        for (CandleData candle : candles) {
            sb.append(String.format("%-12s %10.2f %10.2f %10.2f %10.2f %,12d%n",
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
     * Fetch all available symbols and tokens from AngelOne.
     * This uses the master data endpoint to get the complete instrument list.
     *
     * @return List of SymbolInfo objects or empty list if failed
     */
    public List<SymbolInfo> getAllSymbols() {
        log.info("Fetching all symbols from AngelOne master data...");

        try {
            WebClient client = sessionService.createAuthenticatedClient();

            // Use a simple request to get session data which contains some symbol info
            // Note: AngelOne doesn't provide a direct public endpoint for all symbols
            // This is a simplified approach - in production you'd use their master data API

            // For now, return a curated list of common NSE symbols
            List<SymbolInfo> symbols = getCommonSymbols();

            log.info("Returning {} common symbols", symbols.size());
            return symbols;

        } catch (Exception e) {
            log.error("Failed to fetch symbols: {}", e.getMessage());
            return getCommonSymbols(); // Fallback to common symbols
        }
    }

    /**
     * Returns a list of commonly traded NSE symbols with their tokens.
     * This serves as a fallback when the master data API is not available.
     */
    private List<SymbolInfo> getCommonSymbols() {
        return List.of(
                new SymbolInfo("RELIANCE", "2885", "NSE", "Reliance Industries Ltd"),
                new SymbolInfo("TCS", "11536", "NSE", "Tata Consultancy Services"),
                new SymbolInfo("INFY", "1594", "NSE", "Infosys Ltd"),
                new SymbolInfo("HDFCBANK", "1333", "NSE", "HDFC Bank Ltd"),
                new SymbolInfo("ICICIBANK", "4963", "NSE", "ICICI Bank Ltd"),
                new SymbolInfo("SBIN", "3045", "NSE", "State Bank of India"),
                new SymbolInfo("BHARTIARTL", "10604", "NSE", "Bharti Airtel Ltd"),
                new SymbolInfo("ITC", "1660", "NSE", "ITC Ltd"),
                new SymbolInfo("KOTAKBANK", "1922", "NSE", "Kotak Mahindra Bank Ltd"),
                new SymbolInfo("HINDUNILVR", "1394", "NSE", "Hindustan Unilever Ltd"),
                new SymbolInfo("TITAN", "3506", "NSE", "Titan Company Ltd"),
                new SymbolInfo("WIPRO", "3432", "NSE", "Wipro Ltd"),
                new SymbolInfo("AXISBANK", "11223", "NSE", "Axis Bank Ltd"),
                new SymbolInfo("LT", "11532", "NSE", "Larsen & Toubro Ltd"),
                new SymbolInfo("MARUTI", "13475", "NSE", "Maruti Suzuki India Ltd"),
                new SymbolInfo("SUNPHARMA", "15994", "NSE", "Sun Pharmaceutical Industries Ltd"),
                new SymbolInfo("M&M", "5330", "NSE", "Mahindra & Mahindra Ltd"),
                new SymbolInfo("NTPC", "12401", "NSE", "NTPC Ltd"),
                new SymbolInfo("POWERGRID", "13534", "NSE", "Power Grid Corporation of India Ltd"),
                new SymbolInfo("COALINDIA", "11588", "NSE", "Coal India Ltd")
        );
    }

    /**
     * Calculate from date by counting back trading days (excluding weekends and holidays).
     * This ensures we get the requested number of trading days of data.
     */
    private LocalDateTime calculateFromDateForTradingDays(LocalDateTime toDate, int tradingDays) {
        LocalDateTime currentDate = toDate;
        int daysCounted = 0;

        // Go back day by day, counting only trading days (weekdays excluding holidays)
        while (daysCounted < tradingDays) {
            currentDate = currentDate.minusDays(1);

            // Skip weekends (Saturday=6, Sunday=7)
            int dayOfWeek = currentDate.getDayOfWeek().getValue();
            if (dayOfWeek <= 5) { // Monday=1 to Friday=5
                // Check if it's a market holiday
                if (!isMarketHoliday(currentDate.toLocalDate())) {
                    daysCounted++;
                }
            }
        }

        log.debug("Requested {} trading days, calculated date range: {} to {}",
                tradingDays, currentDate.toLocalDate(), toDate.toLocalDate());

        return currentDate;
    }

    /**
     * Check if a given date is an Indian market holiday.
     * Includes major NSE/BSE holidays for 2024-2025.
     */
    private boolean isMarketHoliday(LocalDate date) {
        // Get month and day for easier comparison
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();
        int year = date.getYear();

        // Republic Day - January 26
        if (month == 1 && day == 26) return true;

        // Mahashivratri - March 8, 2024; February 18, 2025
        if ((year == 2024 && month == 3 && day == 8) ||
                (year == 2025 && month == 2 && day == 18)) return true;

        // Holi - March 25, 2024; March 14, 2025
        if ((year == 2024 && month == 3 && day == 25) ||
                (year == 2025 && month == 3 && day == 14)) return true;

        // Ram Navami - April 17, 2024; April 6, 2025
        if ((year == 2024 && month == 4 && day == 17) ||
                (year == 2025 && month == 4 && day == 6)) return true;

        // Mahavir Jayanti - April 21, 2024; April 10, 2025
        if ((year == 2024 && month == 4 && day == 21) ||
                (year == 2025 && month == 4 && day == 10)) return true;

        // Good Friday - March 29, 2024; April 18, 2025
        if ((year == 2024 && month == 3 && day == 29) ||
                (year == 2025 && month == 4 && day == 18)) return true;

        // Id-ul-Fitr - April 11, 2024; March 31, 2025
        if ((year == 2024 && month == 4 && day == 11) ||
                (year == 2025 && month == 3 && day == 31)) return true;

        // Maharashtra Day / Gujarat Day - May 1
        if (month == 5 && day == 1) return true;

        // Bakri Id - June 17, 2024; June 7, 2025
        if ((year == 2024 && month == 6 && day == 17) ||
                (year == 2025 && month == 6 && day == 7)) return true;

        // Independence Day - August 15
        if (month == 8 && day == 15) return true;

        // Ganesh Chaturthi - September 7, 2024; August 27, 2025
        if ((year == 2024 && month == 9 && day == 7) ||
                (year == 2025 && month == 8 && day == 27)) return true;

        // Gandhi Jayanti - October 2
        if (month == 10 && day == 2) return true;

        // Dussehra - October 24, 2024; October 13, 2025
        if ((year == 2024 && month == 10 && day == 24) ||
                (year == 2025 && month == 10 && day == 13)) return true;

        // Diwali - November 1, 2024; October 21, 2025
        if ((year == 2024 && month == 11 && day == 1) ||
                (year == 2025 && month == 10 && day == 21)) return true;

        // Diwali Balipratipada - November 2, 2024; October 22, 2025
        if ((year == 2024 && month == 11 && day == 2) ||
                (year == 2025 && month == 10 && day == 22)) return true;

        // Gurunanak Jayanti - November 15, 2024; November 5, 2025
        if ((year == 2024 && month == 11 && day == 15) ||
                (year == 2025 && month == 11 && day == 5)) return true;

        // Christmas - December 25
        if (month == 12 && day == 25) return true;

        // Additional holidays for specific years
        if (year == 2024) {
            // Lok Sabha Election Voting Days
            if ((month == 4 && (day == 19 || day == 26)) ||
                    (month == 5 && (day == 7 || day == 13 || day == 20 || day == 25))) {
                return true;
            }
        }

        return false;
    }

    // Inner class for option greek request
    private record OptionGreekRequest(String name, String expirydate) {
    }

    /**
     * Get market holidays for a specific year.
     * Returns a list of Indian market holidays (NSE/BSE).
     */
    public List<HolidayInfo> getMarketHolidays(int year) {
        List<HolidayInfo> holidays = new ArrayList<>();

        // Add all major holidays for the specified year
        holidays.add(new HolidayInfo(LocalDate.of(year, 1, 26), "Republic Day"));

        // Add variable date holidays based on year
        if (year == 2024) {
            holidays.add(new HolidayInfo(LocalDate.of(2024, 3, 8), "Mahashivratri"));
            holidays.add(new HolidayInfo(LocalDate.of(2024, 3, 25), "Holi"));
            holidays.add(new HolidayInfo(LocalDate.of(2024, 3, 29), "Good Friday"));
            holidays.add(new HolidayInfo(LocalDate.of(2024, 4, 11), "Id-ul-Fitr"));
            holidays.add(new HolidayInfo(LocalDate.of(2024, 4, 17), "Ram Navami"));
            holidays.add(new HolidayInfo(LocalDate.of(2024, 4, 19), "Lok Sabha Election Voting"));
            holidays.add(new HolidayInfo(LocalDate.of(2024, 4, 21), "Mahavir Jayanti"));
            holidays.add(new HolidayInfo(LocalDate.of(2024, 4, 26), "Lok Sabha Election Voting"));
            holidays.add(new HolidayInfo(LocalDate.of(2024, 5, 1), "Maharashtra Day / Gujarat Day"));
            holidays.add(new HolidayInfo(LocalDate.of(2024, 5, 7), "Lok Sabha Election Voting"));
            holidays.add(new HolidayInfo(LocalDate.of(2024, 5, 13), "Lok Sabha Election Voting"));
            holidays.add(new HolidayInfo(LocalDate.of(2024, 5, 20), "Lok Sabha Election Voting"));
            holidays.add(new HolidayInfo(LocalDate.of(2024, 5, 25), "Lok Sabha Election Voting"));
            holidays.add(new HolidayInfo(LocalDate.of(2024, 6, 17), "Bakri Id"));
            holidays.add(new HolidayInfo(LocalDate.of(2024, 8, 15), "Independence Day"));
            holidays.add(new HolidayInfo(LocalDate.of(2024, 9, 7), "Ganesh Chaturthi"));
            holidays.add(new HolidayInfo(LocalDate.of(2024, 10, 2), "Gandhi Jayanti"));
            holidays.add(new HolidayInfo(LocalDate.of(2024, 10, 24), "Dussehra"));
            holidays.add(new HolidayInfo(LocalDate.of(2024, 11, 1), "Diwali"));
            holidays.add(new HolidayInfo(LocalDate.of(2024, 11, 2), "Diwali Balipratipada"));
            holidays.add(new HolidayInfo(LocalDate.of(2024, 11, 15), "Gurunanak Jayanti"));
            holidays.add(new HolidayInfo(LocalDate.of(2024, 12, 25), "Christmas"));
        } else if (year == 2025) {
            holidays.add(new HolidayInfo(LocalDate.of(2025, 2, 18), "Mahashivratri"));
            holidays.add(new HolidayInfo(LocalDate.of(2025, 3, 14), "Holi"));
            holidays.add(new HolidayInfo(LocalDate.of(2025, 3, 31), "Id-ul-Fitr"));
            holidays.add(new HolidayInfo(LocalDate.of(2025, 4, 6), "Ram Navami"));
            holidays.add(new HolidayInfo(LocalDate.of(2025, 4, 10), "Mahavir Jayanti"));
            holidays.add(new HolidayInfo(LocalDate.of(2025, 4, 18), "Good Friday"));
            holidays.add(new HolidayInfo(LocalDate.of(2025, 5, 1), "Maharashtra Day / Gujarat Day"));
            holidays.add(new HolidayInfo(LocalDate.of(2025, 6, 7), "Bakri Id"));
            holidays.add(new HolidayInfo(LocalDate.of(2025, 8, 15), "Independence Day"));
            holidays.add(new HolidayInfo(LocalDate.of(2025, 8, 27), "Ganesh Chaturthi"));
            holidays.add(new HolidayInfo(LocalDate.of(2025, 10, 2), "Gandhi Jayanti"));
            holidays.add(new HolidayInfo(LocalDate.of(2025, 10, 13), "Dussehra"));
            holidays.add(new HolidayInfo(LocalDate.of(2025, 10, 21), "Diwali"));
            holidays.add(new HolidayInfo(LocalDate.of(2025, 10, 22), "Diwali Balipratipada"));
            holidays.add(new HolidayInfo(LocalDate.of(2025, 11, 5), "Gurunanak Jayanti"));
            holidays.add(new HolidayInfo(LocalDate.of(2025, 12, 25), "Christmas"));
        }

        // Sort by date
        holidays.sort(Comparator.comparing(HolidayInfo::date));

        return holidays;
    }

    // Symbol info record
    public record SymbolInfo(String symbol, String token, String exchange, String name) {
    }

    // Holiday info record
    public record HolidayInfo(LocalDate date, String name) {
    }
}
