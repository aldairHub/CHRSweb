package org.uteq.backend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.uteq.backend.Entity.RolUsuario;

import java.util.List;
import java.util.Optional;

@Repository
public interface IRolUsuarioRepository extends JpaRepository<RolUsuario, Long> {
    List<RolUsuario> findAllByOrderByNombreAsc();
    Optional<RolUsuario> findByNombre(String nombre);
}
