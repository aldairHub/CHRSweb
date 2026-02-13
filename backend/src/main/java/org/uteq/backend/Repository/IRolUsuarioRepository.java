package org.uteq.backend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.Entity.RolUsuario;

import java.util.List;

public interface IRolUsuarioRepository extends JpaRepository<RolUsuario, Long> {
    List<RolUsuario> findAllByOrderByNombreAsc();
}
