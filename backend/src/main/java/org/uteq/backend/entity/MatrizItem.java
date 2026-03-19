package org.uteq.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "matriz_item")
@Data
@NoArgsConstructor
public class MatrizItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_item")
    private Long idItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_seccion", nullable = false)
    private MatrizSeccion seccion;

    @Column(name = "codigo", unique = true, nullable = false, columnDefinition = "VARCHAR(20)")
    private String codigo;

    @Column(name = "label", nullable = false, columnDefinition = "VARCHAR(300)")
    private String label;

    @Column(name = "puntaje_maximo", nullable = false, columnDefinition = "NUMERIC(6,2)")
    private Double puntajeMaximo;

    @Column(name = "puntos_por", columnDefinition = "VARCHAR(100)")
    private String puntosPor;

    @Column(name = "orden", nullable = false)
    private Integer orden;

    @Column(name = "bloqueado", nullable = false)
    private Boolean bloqueado = false;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}
