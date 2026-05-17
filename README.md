# Financial Agent India - Java/Spring Boot Version

A Java 21 / Spring Boot implementation of the AI-powered financial analysis agent for Indian equities.
Uses [AngelOne SmartAPI](https://smartapi.angelbroking.com/) for market data and [Claude](https://docs.anthropic.com/) (
Anthropic's API) for AI-powered analysis.

This is a complete port of the Python version with additional features:

- **REST API** for all functionality
- **Modern Web UI** with dark theme
- **Real-time connection management**
- **Order placement with dry-run safety**

---

## Features

| Feature              | Description                                          |
|----------------------|------------------------------------------------------|
| **Stock Analysis**   | AI-powered technical analysis using Claude           |
| **Options Analysis** | Cross-reference price action with options chain data |
| **Market Data**      | Fetch OHLCV candles and option chains                |
| **Order Placement**  | Buy, sell, and stop-loss orders (dry-run by default) |
| **Web UI**           | Modern dark-themed dashboard                         |
| **REST API**         | Full API for programmatic access                     |

---

## Prerequisites

- **Java 21** or higher
- **Maven 3.8+** or use the included Maven wrapper
- **AngelOne trading account** with SmartAPI access
- **Anthropic API key** for Claude

---

## Quick Start

### 1. Clone and navigate to the Java project

```bash
cd financial-agent
```

### 2. Configure credentials

```bash
cp .env.example .env
```

Edit `.env` with your actual credentials:

```properties
ANGELONE_API_KEY=your_smartapi_key_here
ANGELONE_CLIENT_ID=A12345678
ANGELONE_PASSWORD=1234
ANGELONE_TOTP_SECRET=your_totp_base32_secret
ANTHROPIC_API_KEY=sk-ant-...
ORDER_DRY_RUN=true
```

### 3. Build and run

**Using Maven:**

```bash
./mvnw spring-boot:run
```

**Or build a JAR:**

```bash
./mvnw clean package
java -jar target/financial-agent-india-1.0.0.jar
```

### 4. Open the UI

Navigate to [http://localhost:8080](http://localhost:8080)

---

## APIs Used

### 1. AngelOne SmartAPI (External APIs)

#### Base URL: `https://apiconnect.angelbroking.com`

**Authentication & Session Management**

- **Endpoint**: `/rest/auth/angelbroking/user/v1/loginByPassword`
- **Method**: POST
- **Purpose**: Authenticate with AngelOne using client ID, password, and TOTP
- **Headers Required**:
    - `X-PrivateKey`: API Key
    - `X-UserType`: USER
    - `X-SourceID`: WEB
    - `X-ClientLocalIP`: 127.0.0.1
    - `X-ClientPublicIP`: 127.0.0.1
    - `X-MACAddress`: 00:00:00:00:00:00

**Market Data APIs**

- **Endpoint**: `/rest/secure/angelbroking/historical/v1/getCandleData`
- **Method**: POST
- **Purpose**: Fetch historical OHLCV candle data for stocks
- **Parameters**: exchange, symbolToken, interval, fromDate, toDate
- **Supported Intervals**: ONE_MINUTE, FIVE_MINUTE, FIFTEEN_MINUTE, THIRTY_MINUTE, ONE_HOUR, ONE_DAY

- **Endpoint**: `/rest/secure/angelbroking/option/v1/optionGreek`
- **Method**: POST
- **Purpose**: Fetch options chain data with Greeks
- **Parameters**: name (symbol), expirydate

**Order Management APIs**

- **Endpoint**: `/rest/secure/angelbroking/order/v1/placeOrder`
- **Method**: POST
- **Purpose**: Place buy/sell orders (market, limit, stop-loss)
- **Features**: Dry-run mode for testing without real trades

### 2. Anthropic Claude API (External AI)

**Base URL**: `https://api.anthropic.com/v1/messages`

- **Method**: POST
- **Purpose**: AI-powered stock analysis using Claude
- **Headers Required**:
    - `x-api-key`: Anthropic API Key
    - `anthropic-version`: 2023-06-01
- **Model**: claude-sonnet-4-20250514
- **Max Tokens**: 1500
- **Use Cases**:
    - Technical analysis of OHLCV data
    - Options chain analysis with sentiment
    - Trend prediction and support/resistance levels

### 3. Internal REST APIs

**Connection Management (`/api/connection`)**

- **POST** `/connect` - Authenticate with AngelOne
- **GET** `/status` - Check connection status
- **POST** `/disconnect` - Clear session

**Market Data (`/api/market`)**

- **GET** `/candles` - Fetch historical candle data
- **GET** `/candles/formatted` - Get candles as formatted table
- **GET** `/options` - Fetch options chain

**Stock Analysis (`/api/analysis`)**

- **POST** `/basic` - AI analysis using price data only
- **POST** `/with-options` - AI analysis with options data
- **GET** `/quick` - Quick analysis endpoint

**Order Management (`/api/orders`)**

- **GET** `/mode` - Check dry-run mode
- **POST** `/buy/market` - Market buy order
- **POST** `/buy/limit` - Limit buy order
- **POST** `/sell/market` - Market sell order
- **POST** `/sell/limit` - Limit sell order
- **POST** `/stoploss` - Stop-loss order
- **POST** `/place` - Generic order placement
- **GET** `/{orderId}/status` - Check order status
- **DELETE** `/{orderId}` - Cancel order

**Web Pages (`/`)**

- **GET** `/` - Main dashboard
- **GET** `/analysis` - Stock analysis page
- **GET** `/market-data` - Market data page
- **GET** `/orders` - Order placement page
- **GET** `/settings` - Settings page

---

## API Endpoints Summary

### Connection

| Endpoint                     | Method | Description                |
|------------------------------|--------|----------------------------|
| `/api/connection/connect`    | POST   | Authenticate with AngelOne |
| `/api/connection/status`     | GET    | Check connection status    |
| `/api/connection/disconnect` | POST   | Clear session              |

### Market Data

| Endpoint                                                 | Method | Description                |
|----------------------------------------------------------|--------|----------------------------|
| `/api/market/candles?symbol=RELIANCE&token=2885&days=30` | GET    | Fetch OHLCV data           |
| `/api/market/candles/formatted`                          | GET    | Get formatted candle table |
| `/api/market/options?symbol=RELIANCE`                    | GET    | Fetch option chain         |

### Analysis

| Endpoint                                                | Method | Description              |
|---------------------------------------------------------|--------|--------------------------|
| `/api/analysis/basic?symbol=RELIANCE&token=2885`        | POST   | AI analysis (price only) |
| `/api/analysis/with-options?symbol=RELIANCE&token=2885` | POST   | AI analysis with options |
| `/api/analysis/quick?symbol=RELIANCE&token=2885`        | GET    | Quick analysis           |

### Orders

| Endpoint                                                            | Method | Description             |
|---------------------------------------------------------------------|--------|-------------------------|
| `/api/orders/mode`                                                  | GET    | Check dry-run mode      |
| `/api/orders/buy/market?symbol=RELIANCE-EQ&token=2885&quantity=1`   | POST   | Market buy              |
| `/api/orders/buy/limit?symbol=...&price=1300`                       | POST   | Limit buy               |
| `/api/orders/sell/market?symbol=...`                                | POST   | Market sell             |
| `/api/orders/sell/limit?symbol=...&price=1350`                      | POST   | Limit sell              |
| `/api/orders/stoploss?symbol=...&limitPrice=1245&triggerPrice=1250` | POST   | Stop-loss               |
| `/api/orders/place`                                                 | POST   | Generic order placement |
| `/api/orders/{orderId}/status`                                      | GET    | Check order status      |
| `/api/orders/{orderId}?variety=NORMAL`                              | DELETE | Cancel order            |

---

## Project Structure

```
financial-agent/
├── src/main/java/com/financialagent/
│   ├── FinancialAgentApplication.java    # Main application
│   ├── config/
│   │   ├── AngelOneConfig.java           # AngelOne credentials
│   │   ├── AnthropicConfig.java          # Claude API config
│   │   ├── OrderConfig.java              # Order safety settings
│   │   └── WebClientConfig.java          # HTTP client config
│   ├── controller/
│   │   ├── ConnectionController.java     # /api/connection/*
│   │   ├── MarketDataController.java     # /api/market/*
│   │   ├── AnalysisController.java       # /api/analysis/*
│   │   ├── OrderController.java          # /api/orders/*
│   │   └── WebController.java            # UI pages
│   ├── dto/
│   │   ├── ApiResponse.java              # Generic API response
│   │   ├── CandleData.java               # OHLCV candle
│   │   ├── StockAnalysisResult.java      # Claude analysis result
│   │   ├── OrderRequest.java             # Order placement
│   │   └── ...
│   ├── exception/
│   │   ├── ApiException.java
│   │   ├── AuthenticationException.java
│   │   └── GlobalExceptionHandler.java
│   └── service/
│       ├── AngelOneSessionService.java   # Authentication (connect.py)
│       ├── MarketDataService.java        # Data fetching (fetch_data.py)
│       ├── ClaudeAnalysisService.java    # AI analysis (agent.py)
│       └── OrderService.java             # Order placement (place_order.py)
├── src/main/resources/
│   ├── application.yml                   # Configuration
│   └── templates/                        # Thymeleaf UI templates
│       ├── index.html                    # Dashboard
│       ├── analysis.html                 # Stock analysis
│       ├── market-data.html              # Market data viewer
│       ├── orders.html                   # Order placement
│       └── settings.html                 # Settings page
├── pom.xml                               # Maven dependencies
├── .env.example                          # Environment template
└── README.md
```

---

## Configuration

All configuration is in `application.yml` and can be overridden with environment variables:

| Property               | Environment Variable   | Description                 |
|------------------------|------------------------|-----------------------------|
| `angelone.api-key`     | `ANGELONE_API_KEY`     | SmartAPI key                |
| `angelone.client-id`   | `ANGELONE_CLIENT_ID`   | Trading client ID           |
| `angelone.password`    | `ANGELONE_PASSWORD`    | Trading PIN                 |
| `angelone.totp-secret` | `ANGELONE_TOTP_SECRET` | TOTP secret (base32)        |
| `anthropic.api-key`    | `ANTHROPIC_API_KEY`    | Claude API key              |
| `order.dry-run`        | `ORDER_DRY_RUN`        | Safety mode (default: true) |

---

## Common Symbol Tokens

| Stock      | Token |
|------------|-------|
| RELIANCE   | 2885  |
| TCS        | 11536 |
| INFY       | 1594  |
| HDFCBANK   | 1333  |
| ICICIBANK  | 4963  |
| SBIN       | 3045  |
| BHARTIARTL | 10604 |
| ITC        | 1660  |
| KOTAKBANK  | 1922  |
| HINDUNILVR | 1394  |

---

## Comparison with Python Version

| Python File             | Java Equivalent                               |
|-------------------------|-----------------------------------------------|
| `config.py`             | `AngelOneConfig.java`, `AnthropicConfig.java` |
| `connect.py`            | `AngelOneSessionService.java`                 |
| `fetch_data.py`         | `MarketDataService.java`                      |
| `agent.py`              | `ClaudeAnalysisService.java`                  |
| `agent_with_options.py` | `ClaudeAnalysisService.analyzeWithOptions()`  |
| `place_order.py`        | `OrderService.java`                           |

---

## Safety Features

1. **DRY_RUN mode** is enabled by default - no real orders are placed
2. **TOTP authentication** - secure 2FA login
3. **Credentials in environment** - not hardcoded
4. **Clear warnings** in UI when in live mode

---

## Disclaimer

This project is for **educational purposes only**. It is not financial advice.

- The analysis generated by Claude is based on historical price data and should not be used as the sole basis for
  trading decisions
- Always do your own research before making investment decisions
- The authors are not responsible for any financial losses incurred from using this code
- If you enable real order placement, you do so entirely at your own risk

---

## License

MIT — do whatever you want with it.
"# stock-market" 
