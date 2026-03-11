package org.uteq.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.uteq.backend.entity.Carrera;

import java.util.List;

@Repository
public interface CarreraRepository extends JpaRepository<Carrera, Long> {
    // Usado en el formulario de solicitud docente para la cascada
    @Query("SELECT c FROM Carrera c WHERE c.facultad.idFacultad = :idFacultad AND c.estado = true")
    List<Carrera> findByFacultadIdAndEstadoTrue(@Param("idFacultad") Long idFacultad);
}