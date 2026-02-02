package org.uteq.backend.Repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.uteq.backend.Entity.Usuario;
import java.util.List;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends CrudRepository<Usuario, Long> {
    Optional<Usuario> findByUsuarioApp(String usuarioApp);

    boolean existsByUsuarioApp(String usuarioApp);
    List<Usuario> findAll();
}