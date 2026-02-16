package org.uteq.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.uteq.backend.entity.RolApp;

import java.util.Optional;

/**
 * Repository para gestionar roles de aplicación
 * Reemplaza a IRolAutoridadRepository
 */
@Repository
public interface RolAppRepository extends JpaRepository<RolApp, Integer> {

    Optional<RolApp> findByNombre(String nombre);

    boolean existsByNombre(String nombre);
}
