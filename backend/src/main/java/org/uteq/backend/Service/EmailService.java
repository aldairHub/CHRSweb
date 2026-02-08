package org.uteq.backend.Service;

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
            <body style="font-family: Arial;">
                <h2>Bienvenido al Sistema UTEQ</h2>
                <p>Estas son tus credenciales de acceso:</p>

                <p><strong>Usuario:</strong> %s</p>
                <p><strong>Contraseña:</strong> %s</p>

                <p>Ingresa en:</p>
                <p><strong>http://localhost:4200/login</strong></p>

                <p style="color:#777;">Correo automático, no responder.</p>
            </body>
            </html>
            """.formatted(usuario, clave);
    }
}