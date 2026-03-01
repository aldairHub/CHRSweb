package org.uteq.backend.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uteq.backend.entity.Usuario;
import org.uteq.backend.repository.UsuarioRepository;
import org.uteq.backend.service.AesCipherService;

import java.security.SecureRandom;
import java.util.List;

@RestController
public class DemoController {

    private final JdbcTemplate jdbcTemplate;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private AesCipherService aesCipherService;
    public DemoController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/api/demo/whoami")
    public String whoami() {
        return jdbcTemplate.queryForObject("SELECT current_user", String.class);
    }

//    @GetMapping("/resetear-claves-bd")
//    public ResponseEntity<String> resetearClavesBd() {
//        List<Usuario> usuarios = usuarioRepository.findAll();
//        int arreglados = 0;
//        int errores = 0;
//
//        for (Usuario u : usuarios) {
//            if (u.getUsuarioBd() == null || u.getUsuarioBd().isBlank()) continue;
//
//            try {
//                String claveRealNueva = generarClaveTemporal(16);
//                String claveCifrada = aesCipherService.cifrar(claveRealNueva);
//
//                u.setClaveBd(claveCifrada);
//                usuarioRepository.saveAndFlush(u);
//
//                jdbcTemplate.execute(
//                        String.format("ALTER USER \"%s\" WITH PASSWORD '%s'",
//                                u.getUsuarioBd(), claveRealNueva)
//                );
//
//                arreglados++;
//
//            } catch (Exception e) {
//                errores++;
//                System.err.println("Error en " + u.getUsuarioApp() + ": " + e.getMessage());
//            }
//        }
//
//        return ResponseEntity.ok("Arreglados: " + arreglados + " | Errores: " + errores);
//    }
//
//    private String generarClaveTemporal(int length) {
//        final String ABC = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#$%";
//        SecureRandom r = new SecureRandom();
//        StringBuilder sb = new StringBuilder(length);
//        for (int i = 0; i < length; i++) sb.append(ABC.charAt(r.nextInt(ABC.length())));
//        return sb.toString();
//    }
}