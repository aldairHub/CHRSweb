package org.uteq.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "matriz_seccion")
@Data
@NoArgsConstructor
public class MatrizSeccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_seccion")
    private Long idSeccion;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "codigo", unique = true, nullable = false, columnDefinition = "VARCHAR(10)")
    private String codigo;

    @Column(name = "titulo", nullable = false, columnDefinition = "VARCHAR(200)")
    private String titulo;

    @Column(name = "tipo", nullable = false, columnDefinition = "VARCHAR(20)")
    private String tipo = "meritos";

    @Column(name = "puntaje_maximo", nullable = false, columnDefinition = "NUMERIC(6,2)")
    private Double puntajeMaximo;

    @Column(name = "orden", nullable = false)
    private Integer orden;

    @Column(name = "bloqueado", nullable = false)
    private Boolean bloqueado = false;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @OneToMany(mappedBy = "seccion", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("orden ASC")
    private List<MatrizItem> items = new ArrayList<>();
}
