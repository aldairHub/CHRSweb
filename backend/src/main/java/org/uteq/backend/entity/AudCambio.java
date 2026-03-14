package org.uteq.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "aud_cambio",
        schema = "public",
        indexes = {
                @Index(name = "idx_aud_cambio_tabla_registro", columnList = "tabla, id_registro"),
                @Index(name = "idx_aud_cambio_usuario_app",    columnList = "usuario_app"),
                @Index(name = "idx_aud_cambio_fecha",          columnList = "fecha"),
                @Index(name = "idx_aud_cambio_operacion",      columnList = "operacion")
        }
)
@Data
@NoArgsConstructor
public class AudCambio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_aud_cambio")
    private Long idAudCambio;

    /** Nombre de la tabla afectada (ej: 'convocatoria', 'usuario') */
    @Column(name = "tabla", nullable = false, length = 100)
    private String tabla;

    /** PK del registro afectado en esa tabla */
    @Column(name = "id_registro", nullable = false)
    private Long idRegistro;

    /** Tipo de operación: INSERT, UPDATE o DELETE */
    @Column(name = "operacion", nullable = false, length = 10)
    private String operacion;

    /** Nombre de la columna que cambió */
    @Column(name = "campo", nullable = false, length = 100)
    private String campo;

    /** Valor anterior del campo (NULL en INSERT) */
    @Column(name = "valor_antes", columnDefinition = "TEXT")
    private String valorAntes;

    /** Valor nuevo del campo (NULL en DELETE) */
    @Column(name = "valor_despues", columnDefinition = "TEXT")
    private String valorDespues;

    /** Usuario de PostgreSQL que ejecutó la operación (current_user) */
    @Column(name = "usuario_bd", nullable = false, length = 100)
    private String usuarioBd;

    /** Usuario de la app (JWT) — NULL si el cambio no viene de un request HTTP */
    @Column(name = "usuario_app", length = 255)
    private String usuarioApp;

    /** IP del cliente HTTP — NULL si el cambio no viene de un request HTTP */
    @Column(name = "ip_cliente", length = 100)
    private String ipCliente;

    /** Timestamp con zona horaria del momento exacto del cambio */
    @Column(name = "fecha", nullable = false)
    private OffsetDateTime fecha;
}