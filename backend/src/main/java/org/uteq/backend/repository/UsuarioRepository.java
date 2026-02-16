package org.uteq.backend.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.uteq.backend.entity.Usuario;
import java.util.List;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends CrudRepository<Usuario, Long> {
    Optional<Usuario> findByUsuarioApp(String usuarioApp);
    Optional<Usuario> findByCorreo(String correo);
    boolean existsByUsuarioBd(String usuarioBd);

    boolean existsByUsuarioApp(String usuarioApp);
    boolean existsByCorreo(String correo);
    List<Usuario> findAll();
}