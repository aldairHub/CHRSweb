package org.uteq.backend.dto;

public class RolUsuarioDTO {
    private Long idRolUsuario;
    private String nombre;
    private Boolean estado;

    public RolUsuarioDTO(Long idRolUsuario, String nombre, Boolean estado) {
        this.idRolUsuario = idRolUsuario;
        this.nombre = nombre;
        this.estado = estado;
    }
    public Long getIdRolUsuario() { return idRolUsuario; }
    public String getNombre() { return nombre; }
    public Boolean getEstado() { return estado; }
}
