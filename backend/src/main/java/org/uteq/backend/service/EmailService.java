package org.uteq.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap; // Import necesario para el mapa

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final DynamicMailService dynamicMailService; // ← REEMPLAZA mailSender
    private final Map<String, String> codigoStorage = new ConcurrentHashMap<>();

    // emailFrom y appName ahora vienen de la institución, no de properties
    // Si la institución no tiene correo configurado, usa un fallback
    private static final String APP_NAME_FALLBACK = "Sistema UTEQ";

    public EmailService(DynamicMailService dynamicMailService) {
        this.dynamicMailService = dynamicMailService;
    }

    // ─── Helper interno ──────────────────────────────────────
    // Obtiene el "from" dinámicamente desde la institución activa
    private String getEmailFrom() {
        return dynamicMailService.getEmailFrom();
    }

    private String getAppName() {
        return dynamicMailService.getAppName();
    }

    // ─── Métodos de envío — EXACTAMENTE iguales, solo cambia sender ──────────

    @Async
    public void enviarCodigoVerificacion(String destinatario, String codigo) {
        codigoStorage.put(destinatario, codigo);
        try {
            JavaMailSender sender = dynamicMailService.getMailSender(); // ← ÚNICO CAMBIO
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(getEmailFrom());
            helper.setTo(destinatario);
            helper.setSubject("Código de verificación - " + getAppName());
            helper.setText(construirEmailCodigo(codigo), true);

            sender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar el código de verificación", e);
        }
    }

    public boolean validarCodigo(String correo, String codigoUsuario) {
        String codigoReal = codigoStorage.get(correo);

        System.out.println("--- INTENTO DE VALIDACIÓN ---");
        System.out.println("Correo buscado: " + correo);
        System.out.println("Código que envió el usuario: '" + codigoUsuario + "'");
        System.out.println("Código que tengo guardado: '" + codigoReal + "'");

        if (codigoReal == null) {
            System.out.println("ERROR: No encontré código para este correo (¿Reiniciaste el server?)");
            return false;
        }

        boolean resultado = codigoReal.trim().equals(codigoUsuario.trim());
        System.out.println("Resultado de la comparación: " + resultado);
        return resultado;
    }

    @Async
    public void enviarCredenciales(String destinatario, String usuarioApp, String claveApp) {
        try {
            JavaMailSender sender = dynamicMailService.getMailSender(); // ← ÚNICO CAMBIO
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(getEmailFrom());
            helper.setTo(destinatario);
            helper.setSubject("Credenciales de acceso - " + getAppName());
            helper.setText(construirEmailCredenciales(usuarioApp, claveApp), true);

            sender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar credenciales", e);
        }
    }
    @Async
    public void enviarCorreoRechazo(String destinatario, String nombreCompleto, String motivo) {
        try {
            JavaMailSender sender = dynamicMailService.getMailSender(); // ← ÚNICO CAMBIO
            MimeMessage mensaje = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setTo(destinatario);
            helper.setSubject("Prepostulación Rechazada - " + getAppName());
            helper.setText(construirEmailRechazo(nombreCompleto, motivo), true);
            sender.send(mensaje);

//            System.out.println("✅ Correo de rechazo enviado exitosamente a: " + destinatario);
            log.info("Correo de rechazo enviado exitosamente a {}", destinatario);
        } catch (Exception e) {
//            System.err.println("❌ Error al enviar correo de rechazo: " + e.getMessage());
            log.error("Error al enviar correo a {}: {}", destinatario, e.getMessage(), e);
            throw new RuntimeException("Error al enviar correo de rechazo", e);
        }
    }

    // ─── Builders HTML — sin ningún cambio ───────────────────

    private String construirEmailCodigo(String codigo) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family: Arial; text-align: center;">
                <h2>🔐 Código de verificación</h2>
                <p>Usa este código para continuar tu registro:</p>
                <div style="
                    font-size: 28px;
                    font-weight: bold;
                    background: #e8f5e9;
                    padding: 15px;
                    border-radius: 8px;
                    display: inline-block;
                    margin: 20px 0;
                ">
                    %s
                </div>
                <p>Este código expira en 10 minutos.</p>
                <p style="color:#777;">No respondas este correo.</p>
            </body>
            </html>
            """.formatted(codigo);
    }

    private String construirEmailCredenciales(String usuario, String clave) {
        return """
                <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset="UTF-8">
                            <link href="https://fonts.googleapis.com/css2?family=Sora:wght@600;700&display=swap" rel="stylesheet">
                        </head>
                        <body style="font-family: Arial; margin: 0; padding: 0; background: #f0f2f5;">
                            <div style="max-width: 480px; margin: 40px auto; border-radius: 16px; overflow: hidden; box-shadow: 0 8px 30px rgba(0,0,0,0.12);">
                
                                <div style="background: linear-gradient(135deg, #1a1a2e 0%%, #16213e 100%%); padding: 28px 32px 24px; text-align: center;">
                                    <div style="font-size: 32px; margin-bottom: 8px;">🎓</div>
                                    <h2 style="color: #fff; font-family: 'Sora', Arial, sans-serif; font-size: 18px; margin: 0; font-weight: 700;">
                                        Bienvenido al Sistema UTEQ
                                    </h2>
                                </div>
                
                                <div style="background: #ffffff; padding: 32px;">
                                    <p style="color: #555; font-size: 14px; text-align: center; margin: 0 0 20px;">
                                        Estas son tus credenciales de acceso:
                                    </p>
                
                                    <div style="background: #f2faf4; border: 1px solid #c8e6c9; border-radius: 10px; overflow: hidden;">
                                        <div style="display: flex; align-items: center; padding: 14px 20px; gap: 14px;">
                                            <span style="font-size: 20px;">👤</span>
                                            <div>
                                                <div style="font-size: 11px; color: #5a9e6f; font-weight: 600; text-transform: uppercase; letter-spacing: 1px;">Usuario</div>
                                                <div style="font-family: 'Sora', Arial, sans-serif; font-size: 15px; font-weight: 600; color: #1a1a2e;">%s</div>
                                            </div>
                                        </div>
                                        <div style="border-top: 1px solid #c8e6c9; display: flex; align-items: center; padding: 14px 20px; gap: 14px;">
                                            <span style="font-size: 20px;">🔑</span>
                                            <div>
                                                <div style="font-size: 11px; color: #5a9e6f; font-weight: 600; text-transform: uppercase; letter-spacing: 1px;">Contraseña</div>
                                                <div style="font-family: 'Sora', Arial, sans-serif; font-size: 15px; font-weight: 600; color: #1a1a2e;">%s</div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                
                                <div style="background: #ffffff; padding: 0 32px 24px; text-align: center;">
                                    <p style="color: #aaa; font-size: 12px; margin: 0;">Correo automático, no responder.</p>
                                </div>
                
                            </div>
                        </body>
                        </html>
            """.formatted(usuario, clave);
    }

    private String construirEmailRechazo(String nombreCompleto, String motivo) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #dc2626; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { background-color: #f9fafb; padding: 30px; border-radius: 0 0 8px 8px; }
                    .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #666; }
                    .motivo { background-color: #fee2e2; border-left: 4px solid #dc2626; padding: 15px; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header"><h1>Prepostulación Rechazada</h1></div>
                    <div class="content">
                        <p>Estimado/a <strong>%s</strong>,</p>
                        <p>Lamentamos informarle que su prepostulación ha sido rechazada.</p>
                        <div class="motivo">
                            <strong>Motivo del rechazo:</strong><br>%s
                        </div>
                        <p>Si tiene alguna consulta, por favor comuníquese con nosotros.</p>
                        <p>Atentamente,<br><strong>Universidad Técnica Estatal de Quevedo</strong></p>
                    </div>
                    <div class="footer"><p>Este es un correo automático, por favor no responder.</p></div>
                </div>
            </body>
            </html>
            """, nombreCompleto, motivo);
    }
    // ════════════════════════════════════════════════════════════════════════
// MÉTODO NUEVO — agregar a EmailService.java
// ════════════════════════════════════════════════════════════════════════

    // ════════════════════════════════════════════════════════════════════════
// MÉTODOS NUEVOS — agregar a EmailService.java
// ════════════════════════════════════════════════════════════════════════

    @Async
    public void enviarResultadoSeleccion(String destinatario, String nombreCompleto,
                                         String materia, String carrera,
                                         boolean seleccionado, String puntaje) {
        try {
            JavaMailSender sender = dynamicMailService.getMailSender();
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(getEmailFrom());
            helper.setTo(destinatario);
            helper.setSubject((seleccionado ? "¡Seleccionado! " : "Resultado: ") +
                    "Proceso de Selección Docente — " + getAppName());
            helper.setText(construirEmailResultado(nombreCompleto, materia, carrera,
                    seleccionado, puntaje), true);
            sender.send(message);
            log.info("Correo resultado enviado a {}", destinatario);
        } catch (Exception e) {
            log.error("Error enviando correo resultado a {}: {}", destinatario, e.getMessage());
        }
    }

    @Async
    public void enviarInformeFinalConPdf(String destinatario, String nombreAutoridad,
                                         String materia, String carrera,
                                         String nombreGanador, byte[] pdfBytes) {
        try {
            JavaMailSender sender = dynamicMailService.getMailSender();
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(getEmailFrom());
            helper.setTo(destinatario);
            helper.setSubject("Informe Final de Selección Docente — " + materia + " — " + getAppName());
            helper.setText(construirEmailInformeFinal(nombreAutoridad, materia, carrera, nombreGanador), true);
            helper.addAttachment(
                    "Informe-Seleccion-" + materia.replaceAll("\\s+", "-") + ".pdf",
                    new org.springframework.core.io.ByteArrayResource(pdfBytes),
                    "application/pdf"
            );
            sender.send(message);
            log.info("Informe final enviado a {}", destinatario);
        } catch (Exception e) {
            log.error("Error enviando informe final a {}: {}", destinatario, e.getMessage());
        }
    }

    private String construirEmailInformeFinal(String nombre, String materia,
                                              String carrera, String ganador) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: Arial, sans-serif; margin: 0; padding: 0; background: #f0f2f5;">
              <div style="max-width: 520px; margin: 40px auto; border-radius: 16px;
                          overflow: hidden; box-shadow: 0 8px 30px rgba(0,0,0,0.12);">
                <div style="background: #016630; padding: 28px 32px 24px; text-align: center;">
                  <div style="font-size: 36px; margin-bottom: 8px;">📄</div>
                  <h2 style="color: #fff; font-size: 18px; margin: 0; font-weight: 700;">
                    Informe Final de Selección Docente
                  </h2>
                </div>
                <div style="background: #ffffff; padding: 32px;">
                  <p style="color: #333; font-size: 14px;">Estimado/a <strong>%s</strong>,</p>
                  <p style="color: #333; font-size: 14px; line-height: 1.6;">
                    Se adjunta el Informe Final de Selección para la materia
                    <strong>%s</strong> de la carrera <strong>%s</strong>.
                  </p>
                  <div style="background:#f2faf4;border-radius:8px;padding:14px 20px;margin:16px 0;">
                    <span style="font-size:12px;color:#5a9e6f;">Candidato seleccionado</span><br>
                    <span style="font-size:16px;font-weight:700;color:#016630;">%s</span>
                  </div>
                  <p style="color:#555;font-size:13px;">
                    El documento adjunto contiene el detalle completo del proceso para
                    ser presentado al Consejo Directivo.
                  </p>
                </div>
                <div style="background:#f9f9f9;padding:16px 32px;text-align:center;">
                  <p style="color:#aaa;font-size:12px;margin:0;">
                    Correo automático — no responder.<br>Universidad Técnica Estatal de Quevedo
                  </p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(nombre, materia, carrera, ganador);
    }

    private String construirEmailResultado(String nombre, String materia, String carrera,
                                           boolean seleccionado, String puntaje) {
        String colorHeader = seleccionado ? "#00A63E" : "#536b50";
        String icono       = seleccionado ? "🎉" : "📋";
        String tituloMsg   = seleccionado
                ? "¡Felicitaciones! Has sido seleccionado"
                : "Resultado del proceso de selección";
        String cuerpo = seleccionado
                ? "Nos complace informarte que has sido <strong>seleccionado/a</strong> como docente para la materia <strong>"
                + materia + "</strong> de la carrera de <strong>" + carrera + "</strong>."
                : "El proceso de selección para la materia <strong>" + materia
                + "</strong> de la carrera de <strong>" + carrera + "</strong> ha finalizado. "
                + "Agradecemos tu participación en este proceso.";

        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: Arial, sans-serif; margin: 0; padding: 0; background: #f0f2f5;">
              <div style="max-width: 520px; margin: 40px auto; border-radius: 16px;
                          overflow: hidden; box-shadow: 0 8px 30px rgba(0,0,0,0.12);">
                <div style="background: %s; padding: 28px 32px 24px; text-align: center;">
                  <div style="font-size: 36px; margin-bottom: 8px;">%s</div>
                  <h2 style="color: #fff; font-size: 18px; margin: 0; font-weight: 700;">%s</h2>
                </div>
                <div style="background: #ffffff; padding: 32px;">
                  <p style="color: #333; font-size: 14px;">Estimado/a <strong>%s</strong>,</p>
                  <p style="color: #333; font-size: 14px; line-height: 1.6;">%s</p>
                  %s
                  <p style="color: #555; font-size: 13px; margin-top: 20px;">
                    Si tienes alguna consulta, comunícate con la institución.
                  </p>
                </div>
                <div style="background: #f9f9f9; padding: 16px 32px; text-align: center;">
                  <p style="color: #aaa; font-size: 12px; margin: 0;">
                    Correo automático — no responder.<br>Universidad Técnica Estatal de Quevedo
                  </p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                colorHeader, icono, tituloMsg, nombre, cuerpo,
                !puntaje.equals("—")
                        ? "<div style=\"background:#f2faf4;border-radius:8px;padding:12px 20px;margin:16px 0;text-align:center;\">"
                        + "<span style=\"font-size:13px;color:#5a9e6f;\">Puntaje obtenido</span><br>"
                        + "<span style=\"font-size:24px;font-weight:700;color:#00A63E;\">" + puntaje + " pts</span></div>"
                        : ""
        );
    }

}