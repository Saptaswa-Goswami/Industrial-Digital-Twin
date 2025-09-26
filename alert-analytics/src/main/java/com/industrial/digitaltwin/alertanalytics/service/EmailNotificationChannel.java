package com.industrial.digitaltwin.alertanalytics.service;

import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.industrial.digitaltwin.alertanalytics.model.AlertEvent;
import com.industrial.digitaltwin.alertanalytics.model.NotificationChannel;
import com.industrial.digitaltwin.alertanalytics.model.NotificationType;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

@Component("email")
@Slf4j
public class EmailNotificationChannel implements NotificationChannel {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${notification.channels.email.enabled:true}")
    private boolean enabled;

    @Value("${notification.channels.email.from:alerts@digitaltwin.com}")
    private String fromEmail;

    @Value("${notification.channels.email.to:saptaswa.goswamii.2002@gmail.com}")
    private String toEmail;

    @Override
    public void sendNotification(AlertEvent alert) {
        if (!enabled || mailSender == null) {
            log.warn("Email notifications are disabled or mail sender is not configured");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(buildSubject(alert));

            String htmlContent = buildHtmlContent(alert);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email notification sent for alert: {}", alert.getAlertId());
        } catch (MessagingException e) {
            log.error("Failed to send email notification for alert: {}", alert.getAlertId(), e);
            throw new RuntimeException("Failed to send email notification", e);
        }
    }

    @Override
    public boolean isAvailable() {
        return enabled && mailSender != null;
    }

    @Override
    public NotificationType getType() {
        return NotificationType.EMAIL;
    }

    private String buildSubject(AlertEvent alert) {
        return String.format("[%s] Alert: %s - Machine %s",
                alert.getSeverity(), alert.getType(), alert.getMachineId());
    }

    private String buildHtmlContent(AlertEvent alert) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        StringBuilder html = new StringBuilder();
        html.append("<html><head><style>");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }");
        html.append(
                ".container { max-width: 800px; margin: 0 auto; background-color: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }");
        html.append("h2 { color: #d32f2f; border-bottom: 2px solid #d32f2f; padding-bottom: 10px; }");
        html.append(".alert-table { width: 100%; border-collapse: collapse; margin: 20px 0; }");
        html.append(".alert-table td { padding: 12px; border: 1px solid #ddd; }");
        html.append(".alert-table tr:nth-child(even) { background-color: #f9f9f9; }");
        html.append(".label { font-weight: bold; color: #333; width: 30%; }");
        html.append(".value { color: #555; }");
        html.append(".severity-critical { color: #d32f2f; font-weight: bold; }");
        html.append(".severity-warning { color: #f57c00; font-weight: bold; }");
        html.append(".severity-info { color: #1976d2; font-weight: bold; }");
        html.append(".details-list { margin: 10px 0; padding-left: 20px; }");
        html.append(".details-list li { margin: 5px 0; }");
        html.append(
                ".footer { margin-top: 30px; padding-top: 15px; border-top: 1px solid #eee; color: #777; font-size: 0.9em; }");
        html.append("</style></head><body>");
        html.append("<div class='container'>");
        html.append("<h2>Machine Alert Notification</h2>");
        html.append("<table class='alert-table'>");

        html.append("<tr><td class='label'>Alert ID:</td><td class='value'>").append(alert.getAlertId())
                .append("</td></tr>");
        html.append("<tr><td class='label'>Machine ID:</td><td class='value'>").append(alert.getMachineId())
                .append("</td></tr>");
        html.append("<tr><td class='label'>Severity:</td><td class='value severity-")
                .append(alert.getSeverity().toString().toLowerCase()).append("'>")
                .append(alert.getSeverity()).append("</td></tr>");
        html.append("<tr><td class='label'>Alert Type:</td><td class='value'>").append(alert.getType())
                .append("</td></tr>");
        html.append("<tr><td class='label'>Status:</td><td class='value'>").append(alert.getStatus())
                .append("</td></tr>");
        html.append("<tr><td class='label'>Timestamp:</td><td class='value'>")
                .append(formatter.format(alert.getTimestamp().atOffset(java.time.ZoneOffset.UTC))).append("</td></tr>");

        if (alert.getDescription() != null && !alert.getDescription().isEmpty()) {
            html.append("<tr><td class='label'>Description:</td><td class='value'>").append(alert.getDescription())
                    .append("</td></tr>");
        }

        if (alert.getDetails() != null && !alert.getDetails().isEmpty()) {
            html.append("<tr><td class='label'>Details:</td><td class='value'>")
                    .append(formatDetails(alert.getDetails())).append("</td></tr>");
        }

        html.append("</table>");
        html.append("<div class='footer'>");
        html.append(
                "<p><em>This is an automated alert notification from the Industrial Equipment Digital Twin system.</em></p>");
        html.append(
                "<p><strong>Action Required:</strong> Please investigate this alert and take appropriate action if necessary.</p>");
        html.append("</div>");
        html.append("</div></body></html>");

        return html.toString();
    }

    private String formatDetails(java.util.Map<String, Object> details) {
        StringBuilder formatted = new StringBuilder();
        formatted.append("<ul class='details-list'>");
        for (java.util.Map.Entry<String, Object> entry : details.entrySet()) {
            formatted.append("<li><strong>").append(entry.getKey()).append(":</strong> ")
                    .append(formatValue(entry.getValue())).append("</li>");
        }
        formatted.append("</ul>");
        return formatted.toString();
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "N/A";
        }

        // Handle collections (like Lists, Sets, etc.)
        if (value instanceof java.util.Collection) {
            java.util.Collection<?> collection = (java.util.Collection<?>) value;
            if (collection.isEmpty()) {
                return "[]";
            }

            StringBuilder formatted = new StringBuilder();
            formatted.append("<div style='margin: 5px 0;'><ul style='margin: 0; padding-left: 20px;'>");
            for (Object item : collection) {
                formatted.append("<li style='margin: 3px 0;'>").append(formatCollectionItem(item)).append("</li>");
            }
            formatted.append("</ul></div>");
            return formatted.toString();
        }

        // Handle maps (nested objects)
        if (value instanceof java.util.Map) {
            java.util.Map<?, ?> map = (java.util.Map<?, ?>) value;
            if (map.isEmpty()) {
                return "{}";
            }

            StringBuilder formatted = new StringBuilder();
            formatted.append("<div style='margin: 5px 0;'><ul style='margin: 0; padding-left: 20px;'>");
            for (java.util.Map.Entry<?, ?> entry : map.entrySet()) {
                formatted.append("<li style='margin: 3px 0;'><strong>").append(entry.getKey()).append(":</strong> ")
                        .append(formatValue(entry.getValue())).append("</li>");
            }
            formatted.append("</ul></div>");
            return formatted.toString();
        }

        // Handle arrays
        if (value.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(value);
            if (length == 0) {
                return "[]";
            }

            StringBuilder formatted = new StringBuilder();
            formatted.append("<div style='margin: 5px 0;'><ul style='margin: 0; padding-left: 20px;'>");
            for (int i = 0; i < length; i++) {
                Object item = java.lang.reflect.Array.get(value, i);
                formatted.append("<li style='margin: 3px 0;'>").append(formatCollectionItem(item)).append("</li>");
            }
            formatted.append("</ul></div>");
            return formatted.toString();
        }

        // For simple values, just convert to string
        return escapeHtml(value.toString());
    }

    private String formatCollectionItem(Object item) {
        if (item == null) {
            return "null";
        }

        // Handle maps (objects in collections)
        if (item instanceof java.util.Map) {
            java.util.Map<?, ?> map = (java.util.Map<?, ?>) item;
            StringBuilder formatted = new StringBuilder();
            formatted.append(
                    "<div style='margin: 2px 0; padding: 5px; background-color: #f0f0f0; border-radius: 3px;'>");
            for (java.util.Map.Entry<?, ?> entry : map.entrySet()) {
                formatted.append("<div><strong>").append(entry.getKey()).append(":</strong> ")
                        .append(formatValue(entry.getValue())).append("</div>");
            }
            formatted.append("</div>");
            return formatted.toString();
        }

        // For simple values in collections
        return escapeHtml(item.toString());
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(text.length() * 2);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&':
                    escaped.append("&amp;");
                    break;
                case '<':
                    escaped.append("&lt;");
                    break;
                case '>':
                    escaped.append("&gt;");
                    break;
                case '"':
                    escaped.append("&quot;");
                    break;
                case '\'':
                    escaped.append("&#x27;");
                    break;
                default:
                    escaped.append(c);
                    break;
            }
        }
        return escaped.toString();
    }

}