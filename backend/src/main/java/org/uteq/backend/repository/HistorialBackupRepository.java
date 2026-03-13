package org.uteq.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.entity.HistorialBackup;
import java.util.List;

public interface HistorialBackupRepository extends JpaRepository<HistorialBackup, Long> {
    List<HistorialBackup> findTop50ByOrderByFechaInicioDesc();
}
