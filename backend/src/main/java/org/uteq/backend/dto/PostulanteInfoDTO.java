package org.uteq.backend.dto;

// ============================================================
// DTO — Info del postulante (SP 5)
// ============================================================
public class PostulanteInfoDTO {

    private Long idPostulante;
    private String nombres;
    private String apellidos;
    private String identificacion;
    private String correo;
    private Long idPostulacion;
    private String estadoPostulacion;
    private String nombreMateria;
    private String nombreCarrera;
    private String nombreArea;

    // Getters y Setters
    public Long getIdPostulante() { return idPostulante; }
    public void setIdPostulante(Long v) { this.idPostulante = v; }

    public String getNombres() { return nombres; }
    public void setNombres(String v) { this.nombres = v; }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String v) { this.apellidos = v; }

    public String getIdentificacion() { return identificacion; }
    public void setIdentificacion(String v) { this.identificacion = v; }

    public String getCorreo() { return correo; }
    public void setCorreo(String v) { this.correo = v; }

    public Long getIdPostulacion() { return idPostulacion; }
    public void setIdPostulacion(Long v) { this.idPostulacion = v; }

    public String getEstadoPostulacion() { return estadoPostulacion; }
    public void setEstadoPostulacion(String v) { this.estadoPostulacion = v; }

    public String getNombreMateria() { return nombreMateria; }
    public void setNombreMateria(String v) { this.nombreMateria = v; }

    public String getNombreCarrera() { return nombreCarrera; }
    public void setNombreCarrera(String v) { this.nombreCarrera = v; }

    public String getNombreArea() { return nombreArea; }
    public void setNombreArea(String v) { this.nombreArea = v; }
}


// ============================================================
// DTO — Respuesta genérica (para SP 2, 3, 4)
// ============================================================
class OperacionResultDTO {

    private Boolean exitoso;
    private String mensaje;
    private Long idGenerado;  // para SP 2 (id_documento creado)

    public OperacionResultDTO() {}

    public OperacionResultDTO(Boolean exitoso, String mensaje) {
        this.exitoso = exitoso;
        this.mensaje = mensaje;
    }

    public Boolean getExitoso() { return exitoso; }
    public void setExitoso(Boolean v) { this.exitoso = v; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String v) { this.mensaje = v; }

    public Long getIdGenerado() { return idGenerado; }
    public void setIdGenerado(Long v) { this.idGenerado = v; }
}