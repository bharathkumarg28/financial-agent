package com.financialagent.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.security.cert.X509Certificate;

/**
 * Service for sending Telegram bot notifications.
 */
@Service
@Slf4j
public class TelegramService {

    @Value("${telegram.bot-token:}")
    private String botToken;

    @Value("${telegram.enabled:false}")
    private boolean telegramEnabled;

    private final RestTemplate restTemplate;

    public TelegramService() {
        this.restTemplate = createRestTemplate();
    }

    private RestTemplate createRestTemplate() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new javax.net.ssl.TrustManager[] {
                new javax.net.ssl.X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
            }, new java.security.SecureRandom());

            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext);
            CloseableHttpClient httpClient = HttpClients.custom()
                    .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                            .setSSLSocketFactory(sslSocketFactory)
                            .build())
                    .build();

            HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
            factory.setConnectTimeout(java.time.Duration.ofMillis(10000));
            factory.setConnectionRequestTimeout(java.time.Duration.ofMillis(10000));

            return new RestTemplate(factory);
        } catch (Exception e) {
            log.error("Failed to create RestTemplate with SSL configuration, using default", e);
            return new RestTemplate();
        }
    }

    /**
     * Send a message to a Telegram chat.
     *
     * @param chatId  Telegram chat ID
     * @param message Message content
     */
    public void sendMessage(String chatId, String message) {
        if (!telegramEnabled) {
            log.info("Telegram is disabled, skipping message to chat: {}", chatId);
            return;
        }

        if (chatId == null || chatId.isEmpty()) {
            log.warn("Cannot send Telegram message: chat ID is null or empty");
            return;
        }

        try {
            String url = String.format(
                    "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s",
                    botToken,
                    chatId,
                    java.net.URLEncoder.encode(message, "UTF-8")
            );

            String response = restTemplate.getForObject(url, String.class);
            log.info("Telegram message sent to chat {}: {}", chatId, response);

        } catch (Exception e) {
            log.error("Failed to send Telegram message to chat {}: {}", chatId, e.getMessage(), e);
        }
    }

    /**
     * Check if Telegram is enabled.
     */
    public boolean isEnabled() {
        return telegramEnabled;
    }
}
