package com.industrial.digitaltwin.alertanalytics.service;

import com.industrial.digitaltwin.alertanalytics.model.AlertEvent;
import com.industrial.digitaltwin.alertanalytics.model.NotificationChannel;
import com.industrial.digitaltwin.alertanalytics.model.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import jakarta.annotation.PostConstruct;

@Component("sms")
@Slf4j
public class SmsNotificationChannel implements NotificationChannel {

    @Value("${notification.channels.sms.enabled:true}")
    private boolean enabled;

    @Value("${notification.channels.sms.provider:twilio}")
    private String provider;

    @Value("${notification.channels.sms.to:+1234567890}")
    private String toPhoneNumber;

    @Value("${notification.channels.sms.from-number:+0987654321}")
    private String fromPhoneNumber;

    @Value("${notification.channels.sms.account-sid:}")
    private String accountSid;

    @Value("${notification.channels.sms.auth-token:}")
    private String authToken;

    @Autowired
    private Environment env;

    @PostConstruct
    public void initializeTwilio() {
        String sid = env.getProperty("TWILIO_ACCOUNT_SID", accountSid);
        String token = env.getProperty("TWILIO_AUTH_TOKEN", authToken);
        // Don't initialize here with phone number, just credentials
        if (sid != null && !sid.isEmpty() && token != null && !token.isEmpty()) {
            Twilio.init(sid, token);
            log.info("Twilio initialized successfully");
        } else {
            log.warn("Twilio credentials not properly configured. SMS notifications will not work properly.");
        }
    }

    @Override
    public void sendNotification(AlertEvent alert) {
        if (!enabled) {
            log.warn("SMS notifications are disabled");
            return;
        }

        try {
            String message = buildSmsMessage(alert);
            
            // In a real implementation, we would integrate with an SMS provider like Twilio
            // For now, just log the notification that would be sent
            log.info("SMS notification prepared for alert: {}", alert.getAlertId());
            log.info("To: {}, From: {}, Message: {}", toPhoneNumber, fromPhoneNumber, message);
            
            // Simulate sending SMS
            sendSmsViaProvider(toPhoneNumber, fromPhoneNumber, message);
            
            log.info("SMS notification sent for alert: {} to maintenance team", alert.getAlertId());
        } catch (Exception e) {
            log.error("Failed to send SMS notification for alert: {}", alert.getAlertId(), e);
            throw new RuntimeException("Failed to send SMS notification", e);
        }
    }

    @Override
    public boolean isAvailable() {
        return enabled;
    }

    @Override
    public NotificationType getType() {
        return NotificationType.SMS;
    }

    private String buildSmsMessage(AlertEvent alert) {
        return String.format("[ALERT] %s - Machine %s (%s): %s. Time: %s",
            alert.getSeverity(),
            alert.getMachineId(),
            alert.getType(),
            alert.getDescription() != null ? alert.getDescription() : "No description",
            alert.getTimestamp());
    }

    private void sendSmsViaProvider(String to, String from, String message) {
        // In a real implementation, this would integrate with an SMS provider
        // For example, with Twilio:
        /*
        Twilio.init(accountSid, authToken);
        Message.creator(
            new PhoneNumber(to),
            new PhoneNumber(from),
            message)
            .create();
        */
        
        // Send actual SMS via Twilio
        try {
            String sid = env.getProperty("TWILIO_ACCOUNT_SID", accountSid);
            String token = env.getProperty("TWILIO_AUTH_TOKEN", authToken);
            String fromNumber = env.getProperty("TWILIO_PHONE_NUMBER", fromPhoneNumber);
            
            // Ensure phone numbers are properly formatted with +
            String formattedToNumber = ensurePhoneNumberFormat(to);
            String formattedFromNumber = ensurePhoneNumberFormat(fromNumber);
            
            if (sid != null && !sid.isEmpty() && token != null && !token.isEmpty()) {
                // Initialize Twilio if not already done (for cases where PostConstruct didn't run)
                // We'll try to initialize here if needed, but normally it should be done in PostConstruct
                try {
                    // Attempt to send the message - Twilio.init() should have been called already
                    Message.creator(
                        new PhoneNumber(formattedToNumber),
                        new PhoneNumber(formattedFromNumber),
                        message
                    ).create();
                    
                    log.info("SMS sent successfully via Twilio to: {}", formattedToNumber);
                } catch (Exception initException) {
                    // If there's an initialization error, try to initialize and send again
                    Twilio.init(sid, token);
                    Message.creator(
                        new PhoneNumber(formattedToNumber),
                        new PhoneNumber(formattedFromNumber),
                        message
                    ).create();
                    
                    log.info("SMS sent successfully via Twilio to: {}", formattedToNumber);
                }
            } else {
                log.error("Twilio credentials not configured. Cannot send SMS to: {}", formattedToNumber);
            }
        } catch (Exception e) {
            log.error("Failed to send SMS via Twilio to: {}", to, e);
            throw e;
        }
    }

    private String ensurePhoneNumberFormat(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        // Remove any existing + and then add it back to ensure proper format
        phoneNumber = phoneNumber.replaceAll("[^0-9+]", "");
        if (!phoneNumber.startsWith("+")) {
            phoneNumber = "+" + phoneNumber;
        }
        return phoneNumber;
    }
}