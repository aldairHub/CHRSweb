package org.uteq.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "matriz_accion_afirmativa")
@Data
@NoArgsConstructor
public class MatrizAccionAfirmativa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_accion")
    private Long idAccion;

    @Column(name = "codigo", unique = true, nullable = false, columnDefinition = "VARCHAR(20)")
    private String codigo;

    @Column(name = "label", nullable = false, columnDefinition = "VARCHAR(300)")
    private String label;

    @Column(name = "puntos", nullable = false, columnDefinition = "NUMERIC(4,2)")
    private Double puntos = 2.0;

    @Column(name = "orden", nullable = false)
    private Integer orden;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}
