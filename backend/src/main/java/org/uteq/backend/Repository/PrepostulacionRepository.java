package org.uteq.backend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.uteq.backend.Entity.Prepostulacion;

import java.util.Optional;

@Repository
public interface PrepostulacionRepository extends JpaRepository<Prepostulacion, Long> {
    Optional<Prepostulacion> findByIdentificacion(String identificacion);
    Optional<Prepostulacion> findByCorreo(String correo);
    boolean existsByIdentificacion(String identificacion);
    boolean existsByCorreo(String correo);
}