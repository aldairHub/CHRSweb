package org.uteq.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.entity.AudAccion;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public interface AudAccionRepository extends JpaRepository<AudAccion, Long> {

    /**
     * Las columnas usuario_app y usuario_bd en aud_accion son bytea en PostgreSQL
     * (almacenadas así por el trigger de auditoría). JPQL no puede aplicar lower()
     * sobre bytea. Se usa query nativa con CAST explícito en las columnas.
     *
     * Las fechas se reciben como String (ISO "yyyy-MM-dd HH:mm:ss") para evitar
     * problemas de binding de LocalDateTime con nativeQuery=true en algunos drivers.
     *
     * countQuery separado requerido por Spring Data para paginación con nativeQuery=true.
     */
    @Query(
            value = """
            SELECT *
            FROM public.aud_accion
            WHERE (:usuarioApp IS NULL OR LOWER(CAST(usuario_app AS TEXT)) LIKE LOWER(CONCAT('%', :usuarioApp, '%')))
              AND (:usuarioBd  IS NULL OR LOWER(CAST(usuario_bd  AS TEXT)) LIKE LOWER(CONCAT('%', :usuarioBd,  '%')))
              AND (:accion     IS NULL OR accion  = :accion)
              AND (:entidad    IS NULL OR entidad = :entidad)
              AND (:desde      IS NULL OR fecha  >= CAST(:desde AS TIMESTAMP))
              AND (:hasta      IS NULL OR fecha  <  CAST(:hasta AS TIMESTAMP))
            ORDER BY fecha DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM public.aud_accion
            WHERE (:usuarioApp IS NULL OR LOWER(CAST(usuario_app AS TEXT)) LIKE LOWER(CONCAT('%', :usuarioApp, '%')))
              AND (:usuarioBd  IS NULL OR LOWER(CAST(usuario_bd  AS TEXT)) LIKE LOWER(CONCAT('%', :usuarioBd,  '%')))
              AND (:accion     IS NULL OR accion  = :accion)
              AND (:entidad    IS NULL OR entidad = :entidad)
              AND (:desde      IS NULL OR fecha  >= CAST(:desde AS TIMESTAMP))
              AND (:hasta      IS NULL OR fecha  <  CAST(:hasta AS TIMESTAMP))
            """,
            nativeQuery = true
    )
    Page<AudAccion> buscar(
            @Param("usuarioApp") String usuarioApp,
            @Param("usuarioBd")  String usuarioBd,
            @Param("accion")     String accion,
            @Param("entidad")    String entidad,
            @Param("desde")      String desde,
            @Param("hasta")      String hasta,
            Pageable pageable
    );
}