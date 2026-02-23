package org.uteq.backend.entity;


import jakarta.persistence.*;

// IMPORTANTE: Tu BD real usa "activo" (boolean) en vez de "estado"
// y tiene columna "descripcion". Esta entity reemplaza la que tenías
// si usabas "estado".

@Entity
@Table(name = "tipo_documento")
public class TipoDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_documento")
    private Long idTipoDocumento;

    @Column(name = "nombre", nullable = false, unique = true)
    private String nombre;

    // Tu BD real tiene "descripcion" (la BD final no la tenía)
    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "obligatorio", nullable = false)
    private Boolean obligatorio;

    // DIFERENCIA CLAVE: tu BD usa "activo", no "estado"
    @Column(name = "activo", nullable = false)
    private Boolean activo;

    // Getters y Setters
    public Long getIdTipoDocumento() { return idTipoDocumento; }
    public void setIdTipoDocumento(Long v) { this.idTipoDocumento = v; }

    public String getNombre() { return nombre; }
    public void setNombre(String v) { this.nombre = v; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String v) { this.descripcion = v; }

    public Boolean getObligatorio() { return obligatorio; }
    public void setObligatorio(Boolean v) { this.obligatorio = v; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean v) { this.activo = v; }
}