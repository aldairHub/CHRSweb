package org.uteq.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.uteq.backend.entity.AudLoginApp;

import java.util.List;

@Repository
public interface AudLoginAppJpaRepository extends JpaRepository<AudLoginApp, Long> {

    @Query(value = """
        SELECT
            DATE(fecha)           AS dia,
            COUNT(*)              AS total,
            SUM(CASE WHEN resultado = 'SUCCESS' THEN 1 ELSE 0 END) AS exitosos,
            SUM(CASE WHEN resultado = 'FAIL'    THEN 1 ELSE 0 END) AS fallidos
        FROM public.aud_login_app
        WHERE fecha >= NOW() - INTERVAL '7 days'
        GROUP BY DATE(fecha)
        ORDER BY dia
    """, nativeQuery = true)
    List<Object[]> statsUltimos7Dias();
}