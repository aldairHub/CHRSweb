package org.uteq.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class MailConfig {

    @Bean
    @ConditionalOnProperty(
            name = "app.mail.fallback-enabled",
            havingValue = "true",
            matchIfMissing = false
    )
    public JavaMailSender fallbackMailSender(
            @Value("${spring.mail.host:smtp.gmail.com}") String host,
            @Value("${spring.mail.port:587}")           int    port,
            @Value("${spring.mail.username:}")          String username,
            @Value("${spring.mail.password:}")          String password
    ) {
        JavaMailSenderImpl s = new JavaMailSenderImpl();
        s.setHost(host); s.setPort(port);
        s.setUsername(username); s.setPassword(password);
        java.util.Properties props = s.getJavaMailProperties();
        props.put("mail.transport.protocol",     "smtp");
        props.put("mail.smtp.auth",              "true");
        props.put("mail.smtp.starttls.enable",   "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout",           "5000");
        return s;
    }
}
