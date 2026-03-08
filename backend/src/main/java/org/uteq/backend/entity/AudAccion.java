package org.uteq.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Tabla de auditoría extendida de acciones del sistema.
 *
 * Vinculada a la tabla usuario mediante id_usuario (FK nullable).
 * - Cuando viene desde la app Java: id_usuario, usuario_app y usuario_bd
 *   tienen valor real extraído del JWT.
 * - Cuando viene desde un trigger SQL directo: id_usuario = NULL,
 *   usuario_app = 'db_directo', usuario_bd = current_user de PostgreSQL.
 */
@Entity
@Table(name = "aud_accion", schema = "public",
        indexes = {
                @Index(name = "idx_aud_accion_usuario",     columnList = "id_usuario"),
                @Index(name = "idx_aud_accion_usuario_app", columnList = "usuario_app"),
                @Index(name = "idx_aud_accion_entidad",     columnList = "entidad, id_entidad"),
                @Index(name = "idx_aud_accion_fecha",       columnList = "fecha")
        })
@Data
@NoArgsConstructor
public class AudAccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_aud_accion")
    private Long idAudAccion;

    /** FK a usuario — NULL cuando el cambio viene directo por SQL */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = true)
    private Usuario usuario;

    /** Correo / login de la app (JWT). 'db_directo' si viene de SQL */
    @Column(name = "usuario_app", nullable = false, length = 255)
    private String usuarioApp;

    /** Usuario de base de datos PostgreSQL (usuario_bd del JWT o current_user) */
    @Column(name = "usuario_bd", nullable = false, length = 255)
    private String usuarioBd;

    /**
     * Tipo de acción.
     * Valores: CREAR, ACTUALIZAR, ELIMINAR, CAMBIAR_ESTADO,
     *          SUBIR_DOCUMENTO, GENERAR_REPORTE
     */
    @Column(name = "accion", nullable = false, length = 50)
    private String accion;

    /** Nombre lógico de la entidad afectada */
    @Column(name = "entidad", nullable = false, length = 100)
    private String entidad;

    /** PK del registro afectado */
    @Column(name = "id_entidad")
    private Long idEntidad;

    /** Descripción legible del evento */
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    /** IP real del cliente HTTP (null si viene de SQL directo) */
    @Column(name = "ip_cliente", length = 100)
    private String ipCliente;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();
}