package org.uteq.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

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

    @Column(name = "ip_cliente")
    private String ipCliente; // si tu columna es inet, lo tratamos como String desde app

    @Column(name = "user_agent")
    private String userAgent;

    // getters/setters

    public Long getIdAud() { return idAud; }
    public void setIdAud(Long idAud) { this.idAud = idAud; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public String getUsuarioApp() { return usuarioApp; }
    public void setUsuarioApp(String usuarioApp) { this.usuarioApp = usuarioApp; }

    public String getUsuarioBd() { return usuarioBd; }
    public void setUsuarioBd(String usuarioBd) { this.usuarioBd = usuarioBd; }

    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public String getIpCliente() { return ipCliente; }
    public void setIpCliente(String ipCliente) { this.ipCliente = ipCliente; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
}