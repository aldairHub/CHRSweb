package org.uteq.backend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.uteq.backend.Entity.Prepostulacion;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrepostulacionRepository extends JpaRepository<Prepostulacion, Long> {

    // Verificar si ya existe una solicitud con esta identificación (cédula/pasaporte)
    boolean existsByIdentificacion(String identificacion);

    // Buscar por identificación
    Optional<Prepostulacion> findByIdentificacion(String identificacion);

    // Buscar por correo
    Optional<Prepostulacion> findByCorreo(String correo);

    // Buscar por estado de revisión
    List<Prepostulacion> findByEstadoRevision(String estadoRevision);

    // Listar todas ordenadas por fecha de envío (más recientes primero)
    List<Prepostulacion> findAllByOrderByFechaEnvioDesc();
}