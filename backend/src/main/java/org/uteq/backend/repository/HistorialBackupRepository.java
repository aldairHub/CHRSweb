package org.uteq.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.entity.HistorialBackup;

import java.util.List;
import java.util.Optional;

public interface HistorialBackupRepository extends JpaRepository<HistorialBackup, Long> {

    List<HistorialBackup> findTop50ByOrderByFechaInicioDesc();

    /** Para lógica INCREMENTAL: último backup exitoso (cualquier tipo) */
    Optional<HistorialBackup> findTop1ByEstadoOrderByFechaInicioDesc(String estado);

    /** Para lógica DIFERENCIAL: último FULL exitoso */
    Optional<HistorialBackup> findTop1ByTipoBackupExtAndEstadoOrderByFechaInicioDesc(
            String tipoBackupExt, String estado);
}
