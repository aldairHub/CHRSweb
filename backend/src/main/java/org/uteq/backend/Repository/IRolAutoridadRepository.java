package org.uteq.backend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.Entity.RolAutoridad;
import org.uteq.backend.dto.RolUsuarioResponseDTO;

import java.util.List;

public interface IRolAutoridadRepository extends JpaRepository<RolAutoridad, Long> {
    @Query("""
        SELECT DISTINCT new org.uteq.backend.dto.RolUsuarioResponseDTO(ru.idRolUsuario, ru.nombre)
        FROM RolAutoridad ra
        JOIN ra.rolesUsuario ru
        WHERE ra.idRolAutoridad IN :ids
        ORDER BY ru.nombre
    """)
    List<RolUsuarioResponseDTO> findRolesUsuarioByIdsRolAutoridad(@Param("ids") List<Long> ids);
}