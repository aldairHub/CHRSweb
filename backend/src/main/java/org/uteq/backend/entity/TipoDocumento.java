package org.uteq.backend.entity;


import jakarta.persistence.*;
import lombok.Data;

@Data
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

    @Column(name = "activo", nullable = false)
    private Boolean activo;

}