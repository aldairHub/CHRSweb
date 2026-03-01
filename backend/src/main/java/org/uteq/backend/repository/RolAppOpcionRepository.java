package org.uteq.backend.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.uteq.backend.entity.RolAppOpcion;
import org.uteq.backend.entity.RolAppOpcionId;

@Repository
public interface RolAppOpcionRepository
        extends JpaRepository<RolAppOpcion, RolAppOpcionId> {

    boolean existsById_IdRolAppAndId_IdOpcion(
            Integer idRolApp, Integer idOpcion);

    @Modifying
    @Query("DELETE FROM RolAppOpcion r WHERE r.id.idRolApp = :idRolApp")
    void deleteByIdRolApp(Integer idRolApp);
}

