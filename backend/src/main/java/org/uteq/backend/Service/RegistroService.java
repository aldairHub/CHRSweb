package org.uteq.backend.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.Entity.Usuario;
import org.uteq.backend.Repository.UsuarioRepository;
import org.uteq.backend.dto.RegistroResponseDTO;
import org.uteq.backend.dto.RegistroUsuarioDTO;
import org.uteq.backend.util.CredencialesGenerator;

@Service
@RequiredArgsConstructor
public class RegistroService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional
    public RegistroResponseDTO registrarUsuario(RegistroUsuarioDTO dto) {
        // Validar que el correo no esté registrado
        if (usuarioRepository.existsByCorreo(dto.getCorreo())) {
            throw new RuntimeException("El correo ya está registrado en el sistema");
        }

        // Generar credenciales automáticas
        String usuarioApp = CredencialesGenerator.generarUsuario(dto.getCedula());
        String claveAppPlain = CredencialesGenerator.generarClaveApp();
        String claveBd = CredencialesGenerator.generarClaveBd();
        String usuarioBd = "usuario" + dto.getCedula().substring(dto.getCedula().length() - 6);

        // Verificar si el usuario generado ya existe (muy raro, pero por si acaso)
        int contador = 1;
        String usuarioAppOriginal = usuarioApp;
        while (usuarioRepository.existsByUsuarioApp(usuarioApp)) {
            usuarioApp = usuarioAppOriginal + contador;
            contador++;
        }

        // Crear entidad Usuario
        Usuario usuario = new Usuario();
        usuario.setUsuarioBd(usuarioBd);
        usuario.setClaveBd(claveBd);
        usuario.setUsuarioApp(usuarioApp);
        usuario.setClaveApp(passwordEncoder.encode(claveAppPlain)); // Encriptar con BCrypt
        usuario.setCorreo(dto.getCorreo());
        usuario.setActivo(true);

        // Guardar en base de datos
        usuarioRepository.save(usuario);

        // Enviar email con credenciales (asíncrono)
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
}
