package org.uteq.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.uteq.backend.entity.Usuario;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsuarioApp(String usuarioApp);
    Optional<Usuario> findByCorreo(String correo);
    boolean existsByUsuarioBd(String usuarioBd);
    List<Usuario> findByRolesApp_Nombre(String nombreRol);
    boolean existsByUsuarioApp(String usuarioApp);
    boolean existsByCorreo(String correo);
    List<Usuario> findAll();

    /**
     * Usuarios que NO están en autoridad_academica NI en postulante,
     * disponibles para ser vinculados como nueva autoridad.
     */
    @Query("""
        SELECT u FROM Usuario u
        WHERE NOT EXISTS (SELECT 1 FROM AutoridadAcademica a WHERE a.usuario = u)
          AND NOT EXISTS (SELECT 1 FROM Postulante p WHERE p.usuario = u)
          AND NOT EXISTS (SELECT 1 FROM u.rolesApp r WHERE LOWER(r.nombre) = 'postulante')
        ORDER BY u.correo
        """)
    List<Usuario> findUsuariosDisponiblesParaAutoridad();
}