package org.uteq.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.uteq.backend.entity.RolAppBd;

import java.util.List;

@Repository
public interface RolAppBdRepository extends JpaRepository<RolAppBd, Integer> {

    List<RolAppBd> findByRolApp_IdRolApp(Integer idRolApp);

    @Modifying
    @Query("DELETE FROM RolAppBd r WHERE r.rolApp.idRolApp = :idRolApp")
    void deleteByRolAppId(Integer idRolApp);

    /**
     * Lee roles de BD disponibles desde pg_roles con prefijo role_* y NOLOGIN.
     * Equivale a: SELECT rolname FROM pg_roles WHERE rolname LIKE 'role_%'
     * AND rolcanlogin = false ORDER BY rolname
     */
    @Query(value = """
            SELECT rolname FROM pg_roles
            WHERE rolname LIKE 'role\\_%' AND rolcanlogin = false
            ORDER BY rolname
            """, nativeQuery = true)
    List<String> findRolesBdDisponibles();
}