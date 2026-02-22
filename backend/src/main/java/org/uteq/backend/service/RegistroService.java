package org.uteq.backend.service;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.entity.Usuario;
import org.uteq.backend.repository.PostgresProcedureRepository;
import org.uteq.backend.repository.UsuarioRepository;
import org.uteq.backend.dto.RegistroResponseDTO;
import org.uteq.backend.dto.RegistroUsuarioDTO;
import org.uteq.backend.util.CredencialesGenerator;

@Service
@RequiredArgsConstructor
public class RegistroService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AesCipherService aesCipherService;
    private final PostgresProcedureRepository postgresProcedureRepository;

    @Transactional
    public RegistroResponseDTO registrarUsuario(RegistroUsuarioDTO dto) {
        // Validar que el correo no esté registrado
        if (usuarioRepository.existsByCorreo(dto.getCorreo())) {
            throw new RuntimeException("El correo ya está registrado en el sistema");
        }

        // Generar credenciales automáticas
        String usuarioApp = CredencialesGenerator.generarUsuario(dto.getCedula());
        String claveAppPlain = CredencialesGenerator.generarClaveApp();
        String claveAppHash  = passwordEncoder.encode(claveAppPlain);
        //String claveBd = CredencialesGenerator.generarClaveBd();
        String usuarioBd = "usuario" + dto.getCedula().substring(dto.getCedula().length() - 6);
        String claveBdReal   = generarClaveTemporal(16);
        String claveBdCifrada = aesCipherService.cifrar(claveBdReal); // ✅ AES

        // Verificar si el usuario generado ya existe (muy raro, pero por si acaso)
        int contador = 1;
        String usuarioAppOriginal = usuarioApp;
        while (usuarioRepository.existsByUsuarioApp(usuarioApp)) {
            usuarioApp = usuarioAppOriginal + contador;
            contador++;
        }

        //  SP hace todo: INSERT usuario + CREATE USER + GRANT roles
        postgresProcedureRepository.registrarUsuarioSimple(
                usuarioApp, claveAppHash, dto.getCorreo(),
                usuarioBd, claveBdCifrada, claveBdReal,
                dto.getRolesApp()
        );

        // Enviar email con credenciales
        try {
            emailService.enviarCredenciales(dto.getCorreo(), usuarioApp, claveAppPlain);
        } catch (Exception e) {
            // Log el error pero no fallar el registro
            System.err.println("Error al enviar email: " + e.getMessage());
        }

        return new RegistroResponseDTO(
                "Registro exitoso. Se han enviado las credenciales a tu correo electrónico.",
                dto.getCorreo(),
                true
        );
    }
    private String generarClaveTemporal(int length) {
        final String ABC = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#$%";
        java.security.SecureRandom r = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ABC.charAt(r.nextInt(ABC.length())));
        }
        return sb.toString();
    }
}
