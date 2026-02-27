package org.uteq.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.entity.AutoridadAcademica;

import java.util.Optional;

public interface AutoridadAcademicaRepository extends JpaRepository<AutoridadAcademica, Long> {

    // buscar por usuario_app
    Optional<AutoridadAcademica> findByUsuario_UsuarioApp(String usuarioApp);
}
