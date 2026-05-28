package com.financialagent.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for sending SMS notifications using Twilio.
 */
@Service
@Slf4j
public class SmsService {

    @Value("${twilio.account.sid:}")
    private String accountSid;

    @Value("${twilio.auth.token:}")
    private String authToken;

    @Value("${twilio.phone.number:}")
    private String twilioPhoneNumber;

    @Value("${twilio.enabled:false}")
    private boolean twilioEnabled;

    /**
     * Send an SMS message.
     *
     * @param to      Recipient phone number (with country code, e.g., +919876543210)
     * @param message SMS message content
     */
    public void sendSms(String to, String message) {
        if (!twilioEnabled) {
            log.info("Twilio SMS is disabled, skipping SMS to: {}", to);
            return;
        }

        if (to == null || to.isEmpty()) {
            log.warn("Cannot send SMS: recipient phone number is null or empty");
            return;
        }

        try {
            Twilio.init(accountSid, authToken);

            Message sms = Message.creator(
                    new PhoneNumber(to),
                    new PhoneNumber(twilioPhoneNumber),
                    message
            ).create();

            log.info("SMS sent successfully. SID: {}, To: {}", sms.getSid(), to);

        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", to, e.getMessage(), e);
        }
    }

    /**
     * Check if Twilio SMS is enabled.
     */
    public boolean isEnabled() {
        return twilioEnabled;
    }
}
