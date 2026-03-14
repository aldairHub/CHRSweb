package org.uteq.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.entity.AudCambio;

public interface AudCambioRepository extends JpaRepository<AudCambio, Long> {

    @Query(
            value = """
            SELECT *
            FROM public.aud_cambio
            WHERE (:tabla      IS NULL OR tabla      = :tabla)
              AND (:operacion  IS NULL OR operacion  = :operacion)
              AND (:campo      IS NULL OR LOWER(campo)       LIKE LOWER(CONCAT('%', :campo,      '%')))
              AND (:usuarioApp IS NULL OR LOWER(usuario_app) LIKE LOWER(CONCAT('%', :usuarioApp, '%')))
              AND (:desde      IS NULL OR fecha >= CAST(:desde AS TIMESTAMPTZ))
              AND (:hasta      IS NULL OR fecha <  CAST(:hasta AS TIMESTAMPTZ))
            ORDER BY fecha DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM public.aud_cambio
            WHERE (:tabla      IS NULL OR tabla      = :tabla)
              AND (:operacion  IS NULL OR operacion  = :operacion)
              AND (:campo      IS NULL OR LOWER(campo)       LIKE LOWER(CONCAT('%', :campo,      '%')))
              AND (:usuarioApp IS NULL OR LOWER(usuario_app) LIKE LOWER(CONCAT('%', :usuarioApp, '%')))
              AND (:desde      IS NULL OR fecha >= CAST(:desde AS TIMESTAMPTZ))
              AND (:hasta      IS NULL OR fecha <  CAST(:hasta AS TIMESTAMPTZ))
            """,
            nativeQuery = true
    )
    Page<AudCambio> buscar(
            @Param("tabla")      String tabla,
            @Param("operacion")  String operacion,
            @Param("campo")      String campo,
            @Param("usuarioApp") String usuarioApp,
            @Param("desde")      String desde,
            @Param("hasta")      String hasta,
            Pageable pageable
    );
}