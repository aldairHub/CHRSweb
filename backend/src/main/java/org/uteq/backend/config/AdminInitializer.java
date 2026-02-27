package org.uteq.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.uteq.backend.service.AesCipherService;
import org.uteq.backend.util.CredencialesGenerator;

@Component
public class AdminInitializer implements ApplicationRunner {

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired private AesCipherService aesCipherService; // el tuyo
    @Autowired private JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM usuario WHERE usuario_app = ?",
                Integer.class, "admin"
        );
        if (count != null && count > 0) {
            System.out.println(">>> admin ya existe, skip");
            return;
        }
        String claveAppPlain = CredencialesGenerator.generarClaveApp();
        String claveAppHash  = passwordEncoder.encode(claveAppPlain);

        String claveBdReal   = generarClaveTemporal(16);
        String claveBdCifrada = aesCipherService.cifrar(claveBdReal);   // aes

        jdbc.query(
                "SELECT * FROM sp_registrar_usuario_simple(?,?,?,?,?,?,?::varchar[])",
                (rs, rowNum) -> null,
                "admin",
                claveAppHash,
                "admin@uteq.edu.ec",
                "admin",
                claveBdCifrada,
                claveBdReal,
                "{ADMIN}"
        );

        jdbc.update(
                "UPDATE usuario SET primer_login = false WHERE usuario_app = 'admin'"
        );

        System.out.println(">>> admin creado correctamente");
        System.out.println("contraseña app: " + claveAppPlain);

        System.out.println("contraseña bd: " + claveBdReal);
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