package org.uteq.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
@Data
@Entity
@Table(name = "aud_login_app", schema = "public")
public class AudLoginApp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_aud")
    private Long idAud;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "usuario_app", nullable = false, length = 255)
    private String usuarioApp;

    @Column(name = "usuario_bd", length = 255)
    private String usuarioBd; // nullable

    @Column(name = "resultado", nullable = false, length = 20)
    private String resultado; // SUCCESS / FAIL

    @Column(name = "motivo", length = 50)
    private String motivo; // USER_NOT_FOUND / BAD_CREDENTIALS / USER_DISABLED / ERROR

    @Column(name = "ip_cliente", length = 100)
    private String ipCliente;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "id_usuario")
    private Long idUsuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", insertable = false, updatable = false)
    private Usuario usuario;
}