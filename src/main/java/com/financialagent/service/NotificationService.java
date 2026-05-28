package com.financialagent.service;

import com.financialagent.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Service for sending notifications via multiple channels (Email, SMS, Telegram).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;
    private final UserService userService;
    private final SmsService smsService;
    private final TelegramService telegramService;

    /**
     * Send a price alert to a user via all enabled channels.
     *
     * @param userId       User ID to send alert to
     * @param symbol       Stock symbol
     * @param alertType    Type of alert (TARGET_PRICE or STOP_LOSS)
     * @param currentPrice Current price
     * @param targetPrice  Target or stop-loss price
     */
    public void sendPriceAlert(Long userId, String symbol, String alertType,
                              Double currentPrice, Double targetPrice) {
        try {
            User user = userService.getUserById(userId);

            if (user == null) {
                log.warn("Cannot send alert to user {}: user not found", userId);
                return;
            }

            if (!user.getNotificationsEnabled()) {
                log.info("Notifications disabled for user {}, skipping alert", userId);
                return;
            }

            String alertText = alertType.equals("TARGET_PRICE") ? "Target Price Reached" : "Stop Loss Triggered";
            String message = String.format(
                    "%s for %s\nCurrent Price: ₹%.2f\n%s: ₹%.2f",
                    alertText,
                    symbol,
                    currentPrice,
                    alertType.equals("TARGET_PRICE") ? "Target Price" : "Stop Loss",
                    targetPrice
            );

            // Send email
            sendEmailAlert(user, symbol, alertType, currentPrice, targetPrice);

            // Send SMS
            sendSmsAlert(user, message);

            // Send Telegram message
            sendTelegramAlert(user, message);

        } catch (Exception e) {
            log.error("Failed to send price alert to user {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Send price alert via email.
     */
    private void sendEmailAlert(User user, String symbol, String alertType,
                               Double currentPrice, Double targetPrice) {
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            log.warn("Cannot send email alert to user {}: email not found", user.getUsername());
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setSubject("Price Alert: " + symbol);

            String alertText = alertType.equals("TARGET_PRICE") ? "Target Price Reached" : "Stop Loss Triggered";
            String body = String.format(
                    "Hello %s,\n\n" +
                    "%s for %s\n\n" +
                    "Current Price: ₹%.2f\n" +
                    "%s: ₹%.2f\n\n" +
                    "Please check your trading account for further action.\n\n" +
                    "Regards,\n" +
                    "Financial Agent",
                    user.getUsername(),
                    alertText,
                    symbol,
                    currentPrice,
                    alertType.equals("TARGET_PRICE") ? "Target Price" : "Stop Loss",
                    targetPrice
            );

            message.setText(body);

            mailSender.send(message);
            log.info("Price alert email sent to {} for {}", user.getEmail(), symbol);

        } catch (Exception e) {
            log.error("Failed to send price alert email to user {}: {}", user.getUsername(), e.getMessage(), e);
        }
    }

    /**
     * Send price alert via SMS.
     */
    private void sendSmsAlert(User user, String message) {
        if (!smsService.isEnabled()) {
            return;
        }

        if (user.getPhoneNumber() == null || user.getPhoneNumber().isEmpty()) {
            log.warn("Cannot send SMS alert to user {}: phone number not found", user.getUsername());
            return;
        }

        try {
            smsService.sendSms(user.getPhoneNumber(), message);
            log.info("Price alert SMS sent to {}", user.getPhoneNumber());
        } catch (Exception e) {
            log.error("Failed to send price alert SMS to user {}: {}", user.getUsername(), e.getMessage(), e);
        }
    }

    /**
     * Send price alert via Telegram.
     */
    private void sendTelegramAlert(User user, String message) {
        if (!telegramService.isEnabled()) {
            return;
        }

        if (user.getTelegramChatId() == null || user.getTelegramChatId().isEmpty()) {
            log.warn("Cannot send Telegram alert to user {}: chat ID not found", user.getUsername());
            return;
        }

        try {
            telegramService.sendMessage(user.getTelegramChatId(), message);
            log.info("Price alert Telegram message sent to chat {}", user.getTelegramChatId());
        } catch (Exception e) {
            log.error("Failed to send price alert Telegram message to user {}: {}", user.getUsername(), e.getMessage(), e);
        }
    }

    /**
     * Send a generic notification email.
     *
     * @param to      Recipient email
     * @param subject Email subject
     * @param body    Email body
     */
    public void sendNotification(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("Notification email sent to {}", to);

        } catch (Exception e) {
            log.error("Failed to send notification email to {}: {}", to, e.getMessage(), e);
        }
    }
}
