package org.uteq.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.uteq.backend.entity.Institucion;
import org.uteq.backend.repository.InstitucionRepository;

import java.util.Properties;
@Service
public class DynamicMailService {

    private final InstitucionRepository institucionRepository;
    private final AesCipherService aesCipherService;
    private final JavaMailSender defaultMailSender;

    // Fallback si la institución no tiene email configurado
    @Value("${app.email.from}")
    private String emailFromFallback;

    @Value("${app.name}")
    private String appNameFallback;

    public DynamicMailService(InstitucionRepository r,
                              AesCipherService aes,
                              JavaMailSender defaultMailSender) {
        this.institucionRepository = r;
        this.aesCipherService = aes;
        this.defaultMailSender = defaultMailSender;
    }

    public JavaMailSender getMailSender() {
        return institucionRepository.findByActivoTrue()
                .filter(i -> i.getEmailSmtp() != null && i.getEmailPassword() != null)
                .map(this::buildFromInstitucion)
                .orElse(defaultMailSender);
    }

    public String getEmailFrom() {
        return institucionRepository.findByActivoTrue()
                .map(i -> i.getEmailSmtp() != null ? i.getEmailSmtp() : emailFromFallback)
                .orElse(emailFromFallback);
    }

    public String getAppName() {
        return institucionRepository.findByActivoTrue()
                .map(i -> i.getAppName() != null ? i.getAppName() : appNameFallback)
                .orElse(appNameFallback);
    }

    private JavaMailSender buildFromInstitucion(Institucion inst) {
        JavaMailSenderImpl s = new JavaMailSenderImpl();
        s.setHost(inst.getEmailHost() != null ? inst.getEmailHost() : "smtp.gmail.com");
        s.setPort(inst.getEmailPort() != null ? inst.getEmailPort() : 587);
        s.setUsername(inst.getEmailSmtp());
        s.setPassword(aesCipherService.descifrar(inst.getEmailPassword()));

        Properties props = s.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        return s;
    }
}
