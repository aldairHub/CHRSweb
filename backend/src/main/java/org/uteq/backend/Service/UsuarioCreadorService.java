package org.uteq.backend.Service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.uteq.backend.Entity.Usuario;
import org.uteq.backend.Repository.UsuarioRepository;

import java.security.SecureRandom;

/**
 * Servicio para crear usuarios - Usado por PrepostulacionService y AutoridadAcademicaServiceImpl
 */
@Service
public class UsuarioCreadorService {

  private final UsuarioRepository usuarioRepository;
  private final PasswordEncoder passwordEncoder;
  private final EmailService emailService;

  public UsuarioCreadorService(
    UsuarioRepository usuarioRepository,
    PasswordEncoder passwordEncoder,
    EmailService emailService
  ) {
    this.usuarioRepository = usuarioRepository;
    this.passwordEncoder = passwordEncoder;
    this.emailService = emailService;
  }

  /**
   * Crea un usuario con credenciales generadas automáticamente y envía el correo
   */
  public Usuario crearUsuarioYEnviarCredenciales(String correo, String nombres, String apellidos) {
    // generar usuarioApp (a partir del correo)
    String usuarioApp = generarUsuarioAppDesdeCorreo(correo);

    // generar usuarioBd (a partir de nombres+apellidos, único)
    String baseBd = generarUsuarioBdBase(nombres, apellidos);
    String usuarioBd = generarUsuarioBdUnico(baseBd);

    // clave temporal (solo email), hash en BD
    String claveTemporal = generarClaveTemporal(12);
    String claveHash = passwordEncoder.encode(claveTemporal);

    Usuario usuario = new Usuario();
    usuario.setUsuarioApp(usuarioApp);
    usuario.setClaveApp(claveHash);
    usuario.setCorreo(correo);
    usuario.setUsuarioBd(usuarioBd);
    usuario.setClaveBd("MTIzNA==");
    usuario.setActivo(true);

    Usuario usuarioGuardado = usuarioRepository.save(usuario);

    // correo con credenciales
    try {
      emailService.enviarCredenciales(correo, usuarioApp, claveTemporal);
      System.out.println("✅ Usuario creado y correo enviado: " + usuarioApp);
    } catch (Exception ex) {
      System.err.println("⚠️ Usuario creado pero error al enviar correo: " + ex.getMessage());
    }

    return usuarioGuardado;
  }

  // -------------------------
  // Métodos copiados de AutoridadAcademicaServiceImpl
  // -------------------------

  private String generarUsuarioAppDesdeCorreo(String correo) {
    if (correo == null || !correo.contains("@")) {
      throw new RuntimeException("Correo inválido para generar usuarioApp");
    }
    String base = correo.split("@")[0].trim().toLowerCase();
    base = base.replaceAll("\\s+", "").replaceAll("[^a-z0-9._-]", "");
    if (base.isBlank()) throw new RuntimeException("No se pudo generar usuarioApp");

    String candidato = base;
    int n = 1;
    while (usuarioRepository.existsByUsuarioApp(candidato)) {
      n++;
      candidato = base + n;
    }
    return candidato;
  }

  private String normalizar(String s) {
    if (s == null) return "";
    String t = s.toLowerCase();
    t = t.replaceAll("\\s+", "");
    t = t.replace("á","a").replace("é","e").replace("í","i")
      .replace("ó","o").replace("ú","u").replace("ü","u")
      .replace("ñ","n");
    return t.replaceAll("[^a-z0-9]", "");
  }

  private String generarUsuarioBdBase(String nombres, String apellidos) {
    return normalizar(nombres) + normalizar(apellidos);
  }

  private String generarUsuarioBdUnico(String base) {
    if (base == null || base.isBlank()) throw new RuntimeException("No se pudo generar usuarioBd");
    String candidato = base;
    int n = 1;
    while (usuarioRepository.existsByUsuarioBd(candidato)) {
      n++;
      candidato = base + n;
    }
    return candidato;
  }

  private String generarClaveTemporal(int length) {
    final String ABC = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#$%";
    SecureRandom r = new SecureRandom();
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) sb.append(ABC.charAt(r.nextInt(ABC.length())));
    return sb.toString();
  }
}
