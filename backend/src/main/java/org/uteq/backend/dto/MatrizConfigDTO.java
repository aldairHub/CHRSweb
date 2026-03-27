package org.uteq.backend.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class MatrizConfigDTO {

    private List<SeccionDTO> secciones;
    private List<AccionAfirmativaDTO> accionesAfirmativas;

    @Data
    @Builder
    public static class SeccionDTO {
        private Long idSeccion;
        private String codigo;
        private String titulo;
        private String descripcion;
        private Double puntajeMaximo;
        private Integer orden;
        private String tipo;
        private Boolean bloqueado;
        private List<ItemDTO> items;
    }

    @Data
    @Builder
    public static class ItemDTO {
        private Long idItem;
        private String codigo;
        private String label;
        private Double puntajeMaximo;
        private String puntosPor;
        private Integer orden;
        private Boolean bloqueado;
        private String tipoInput;      // "checkbox" | "cantidad" | "bloqueado"
        private Double valorUnitario;  // para tipo "cantidad": puntaje por unidad
    }

    @Data
    @Builder
    public static class AccionAfirmativaDTO {
        private Long idAccion;
        private String codigo;
        private String label;
        private Double puntos;
    }
}
