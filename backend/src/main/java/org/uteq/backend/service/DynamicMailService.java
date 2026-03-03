package org.uteq.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.uteq.backend.entity.Institucion;
import org.uteq.backend.repository.InstitucionRepository;
import java.util.Properties;

@Service
public class DynamicMailService {

    private static final Logger log =
            LoggerFactory.getLogger(DynamicMailService.class);

    private final InstitucionRepository institucionRepository;
    private final AesCipherService aesCipherService;

    public DynamicMailService(InstitucionRepository r, AesCipherService aes) {
        this.institucionRepository = r;
        this.aesCipherService = aes;
    }

    public JavaMailSender getMailSender() {
        return buildFromInstitucion(obtenerInstitucionConEmail());
    }

    public String getEmailFrom() {
        return obtenerInstitucionConEmail().getEmailSmtp();
    }

    public String getAppName() {
        return institucionRepository.findByActivoTrue()
                .map(i -> i.getAppName() != null ? i.getAppName() : "Sistema")
                .orElseThrow(() -> new IllegalStateException(
                        "No hay institucion activa."));
    }

    private Institucion obtenerInstitucionConEmail() {
        Institucion inst = institucionRepository.findByActivoTrue()
                .orElseThrow(() -> new IllegalStateException(
                        "No hay institucion activa."));
        if (inst.getEmailSmtp() == null || inst.getEmailSmtp().isBlank())
            throw new IllegalStateException(
                    "Falta emailSmtp en la institucion activa.");
        if (inst.getEmailPassword() == null || inst.getEmailPassword().isBlank())
            throw new IllegalStateException(
                    "Falta clave de email en la institucion activa.");
        return inst;
    }

    private JavaMailSender buildFromInstitucion(Institucion inst) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        String  host = inst.getEmailHost() != null
                ? inst.getEmailHost() : "smtp.gmail.com";
        int     port = inst.getEmailPort() != null
                ? inst.getEmailPort() : 587;
        boolean ssl  = Boolean.TRUE.equals(inst.getEmailSsl());

        sender.setHost(host); sender.setPort(port);
        sender.setUsername(inst.getEmailSmtp());
        sender.setPassword(aesCipherService.descifrar(inst.getEmailPassword()));

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol",     "smtp");
        props.put("mail.smtp.auth",              "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout",           "5000");
        props.put("mail.smtp.writetimeout",      "5000");

        if (ssl) {
            sender.setProtocol("smtps");
            props.put("mail.smtp.ssl.enable",      "true");
            props.put("mail.smtp.starttls.enable", "false");
            log.debug("SMTP SSL puro activo -> {}:{}", host, port);
        } else {
            props.put("mail.smtp.ssl.enable",      "false");
            props.put("mail.smtp.starttls.enable", "true");
            log.debug("SMTP STARTTLS activo -> {}:{}", host, port);
        }
        return sender;
    }
}
