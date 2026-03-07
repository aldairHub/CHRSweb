package org.uteq.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.uteq.backend.entity.Notificacion;

import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    // Contar no leídas de un usuario
    long countByIdUsuarioAndLeidaFalse(Long idUsuario);

    // Listar las últimas 30 de un usuario (para el dropdown)
    List<Notificacion> findTop30ByIdUsuarioOrderByFechaCreacionDesc(Long idUsuario);

    // Solo las no leídas
    List<Notificacion> findByIdUsuarioAndLeidaFalseOrderByFechaCreacionDesc(Long idUsuario);

    // Marcar una como leída (seguro: solo si pertenece al usuario)
    @Modifying
    @Query("UPDATE Notificacion n SET n.leida = true, n.fechaLeida = CURRENT_TIMESTAMP " +
           "WHERE n.idNotificacion = :id AND n.idUsuario = :idUsuario AND n.leida = false")
    int marcarLeida(@Param("id") Long idNotificacion, @Param("idUsuario") Long idUsuario);

    // Marcar todas las no leídas de un usuario
    @Modifying
    @Query("UPDATE Notificacion n SET n.leida = true, n.fechaLeida = CURRENT_TIMESTAMP " +
           "WHERE n.idUsuario = :idUsuario AND n.leida = false")
    int marcarTodasLeidas(@Param("idUsuario") Long idUsuario);
}
