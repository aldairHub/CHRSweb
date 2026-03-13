package org.uteq.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.entity.ConfigBackup;
import java.util.Optional;

public interface ConfigBackupRepository extends JpaRepository<ConfigBackup, Long> {
    Optional<ConfigBackup> findFirstByOrderByIdConfigAsc();
}
