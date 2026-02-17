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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap; // Import necesario para el mapa

@Service
@RequiredArgsConstructor
@Data
public class EmailService {

    private final JavaMailSender mailSender;

    // 1. AÑADIMOS ESTO: Memoria temporal para guardar los códigos (Correo -> Código)
    private final Map<String, String> codigoStorage = new ConcurrentHashMap<>();

    @Value("${app.email.from}")
    private String emailFrom;

    @Value("${app.name}")
    private String appName;

    @Async
    public void enviarCodigoVerificacion(String destinatario, String codigo) {
        // 2. GUARDAMOS EL CÓDIGO ANTES DE ENVIARLO
        // Así el sistema recuerda que 'juan@uteq.edu.ec' tiene el código '123456'
        codigoStorage.put(destinatario, codigo);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(emailFrom);
            helper.setTo(destinatario);
            helper.setSubject("Código de verificación - " + appName);
            helper.setText(construirEmailCodigo(codigo), true);

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar el código de verificación", e);
        }
    }

    // 3. AÑADIMOS EL MÉTODO QUE TE FALTABA (Validar)
    public boolean validarCodigo(String correo, String codigoUsuario) {
        String codigoReal = codigoStorage.get(correo);

        // --- CHISMOSO PARA VER EL ERROR EN CONSOLA ---
        System.out.println("--- INTENTO DE VALIDACIÓN ---");
        System.out.println("Correo buscado: " + correo);
        System.out.println("Código que envió el usuario: '" + codigoUsuario + "'");
        System.out.println("Código que tengo guardado: '" + codigoReal + "'");
        // ---------------------------------------------

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
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(emailFrom);
            helper.setTo(destinatario);
            helper.setSubject("Credenciales de acceso - " + appName);
            helper.setText(construirEmailCredenciales(usuarioApp, claveApp), true);

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar credenciales", e);
        }
    }

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

    /**
     * Envía correo notificando el rechazo de la prepostulación
     */
    public void enviarCorreoRechazo(String destinatario, String nombreCompleto, String motivo) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setTo(destinatario);
            helper.setSubject("Prepostulación Rechazada - UTEQ");

            String contenidoHtml = String.format("""
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
                    <div class="header">
                        <h1>Prepostulación Rechazada</h1>
                    </div>
                    <div class="content">
                        <p>Estimado/a <strong>%s</strong>,</p>
                        
                        <p>Lamentamos informarle que su prepostulación ha sido rechazada.</p>
                        
                        <div class="motivo">
                            <strong>Motivo del rechazo:</strong><br>
                            %s
                        </div>
                        
                        <p>Si tiene alguna consulta, por favor comuníquese con nosotros.</p>
                        
                        <p>Atentamente,<br>
                        <strong>Universidad Técnica Estatal de Quevedo</strong></p>
                    </div>
                    <div class="footer">
                        <p>Este es un correo automático, por favor no responder.</p>
                    </div>
                </div>
            </body>
            </html>
            """,
                    nombreCompleto,
                    motivo
            );

            helper.setText(contenidoHtml, true);
            mailSender.send(mensaje);

            System.out.println("✅ Correo de rechazo enviado exitosamente a: " + destinatario);

        } catch (Exception e) {
            System.err.println("❌ Error al enviar correo de rechazo: " + e.getMessage());
            throw new RuntimeException("Error al enviar correo de rechazo", e);
        }
    }
}