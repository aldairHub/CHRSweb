package org.uteq.backend.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${app.email.from}")
    private String emailFrom;

    @Value("${app.name}")
    private String appName;

    @Async
    public void enviarCredenciales(String destinatario, String usuarioApp, String claveApp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(emailFrom);
            helper.setTo(destinatario);
            helper.setSubject("Credenciales de acceso - " + appName);

            String contenidoHtml = construirEmailCredenciales(usuarioApp, claveApp);
            helper.setText(contenidoHtml, true);

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar el correo: " + e.getMessage());
        }
    }

    private String construirEmailCredenciales(String usuario, String clave) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            line-height: 1.6;
                            color: #333;
                            max-width: 600px;
                            margin: 0 auto;
                            padding: 20px;
                        }
                        .header {
                            background: linear-gradient(135deg, #059669 0%, #10b981 100%);
                            color: white;
                            padding: 30px;
                            text-align: center;
                            border-radius: 10px 10px 0 0;
                        }
                        .header h1 {
                            margin: 0;
                            font-size: 24px;
                        }
                        .content {
                            background: #f9f9f9;
                            padding: 30px;
                            border-radius: 0 0 10px 10px;
                        }
                        .credentials-box {
                            background: white;
                            border: 2px solid #059669;
                            border-radius: 8px;
                            padding: 20px;
                            margin: 20px 0;
                        }
                        .credential-item {
                            margin: 15px 0;
                        }
                        .credential-label {
                            font-weight: bold;
                            color: #059669;
                            display: block;
                            margin-bottom: 5px;
                        }
                        .credential-value {
                            background: #e8f5e9;
                            padding: 10px;
                            border-radius: 5px;
                            font-family: 'Courier New', monospace;
                            font-size: 16px;
                            word-break: break-all;
                        }
                        .warning {
                            background: #fff3cd;
                            border-left: 4px solid #ffc107;
                            padding: 15px;
                            margin: 20px 0;
                            border-radius: 4px;
                        }
                        .footer {
                            text-align: center;
                            margin-top: 30px;
                            padding-top: 20px;
                            border-top: 1px solid #ddd;
                            color: #666;
                            font-size: 14px;
                        }
                    </style>
                </head>
                <body>
                    <div class="header">
                        <h1>🎓 UTEQ</h1>
                        <p>Universidad Técnica Estatal de Quevedo</p>
                    </div>
                    
                    <div class="content">
                        <h2 style="color: #059669;">¡Bienvenido al Sistema de Selección Docente!</h2>
                        
                        <p>Tu registro se ha completado exitosamente. A continuación, encontrarás tus credenciales de acceso:</p>
                        
                        <div class="credentials-box">
                            <div class="credential-item">
                                <span class="credential-label">👤 Usuario:</span>
                                <div class="credential-value">%s</div>
                            </div>
                            
                            <div class="credential-item">
                                <span class="credential-label">🔐 Contraseña:</span>
                                <div class="credential-value">%s</div>
                            </div>
                        </div>
                        
                        <div class="warning">
                            <strong>⚠️ Importante:</strong>
                            <ul>
                                <li>Guarda estas credenciales en un lugar seguro</li>
                                <li>No compartas tu contraseña con nadie</li>
                                <li>Se recomienda cambiar tu contraseña después del primer inicio de sesión</li>
                            </ul>
                        </div>
                        
                        <p>Puedes iniciar sesión en: <strong>%s</strong></p>
                        
                        <p>Si tienes alguna pregunta o necesitas asistencia, no dudes en contactarnos.</p>
                    </div>
                    
                    <div class="footer">
                        <p>Este es un correo automático, por favor no responder.</p>
                        <p>&copy; 2025 Universidad Técnica Estatal de Quevedo - Todos los derechos reservados</p>
                    </div>
                </body>
                </html>
                """.formatted(usuario, clave, "http://localhost:4200/login");
    }
}
