package org.uteq.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.entity.DriveConfig;

import java.util.Optional;

public interface DriveConfigRepository extends JpaRepository<DriveConfig, Long> {

    /** Siempre habrá máximo 1 registro de config (singleton) */
    Optional<DriveConfig> findFirstByActivoTrueOrderByIdAsc();
}
