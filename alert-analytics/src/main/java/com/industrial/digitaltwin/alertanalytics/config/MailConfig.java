package com.industrial.digitaltwin.alertanalytics.config;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class MailConfig {

    @Bean
    @ConditionalOnProperty(name = "notification.channels.email.enabled", havingValue = "true", matchIfMissing = true)
    public JavaMailSender javaMailSender(
            @Value("${notification.channels.email.smtp-host:smtp.gmail.com}") String host,
            @Value("${notification.channels.email.smtp-port:587}") int port,
            @Value("${notification.channels.email.username:}") String username,
            @Value("${notification.channels.email.password:}") String password) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        props.put("mail.debug", "true");  // Enable debug to help troubleshoot issues
        
        return mailSender;
    }
}