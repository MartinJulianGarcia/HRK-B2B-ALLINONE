package com.hrk.tienda_b2b.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActualizacionProductoResponse {
    private ProductoResponseDTO producto;
    private boolean soloAgregarVariantes;
    private int cantidadVariantesNuevas;
    private List<VarianteNuevaInfo> variantesNuevas;
    private String mensaje;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VarianteNuevaInfo {
        private String sku;
        private String color;
        private String talle;
        private Integer stock;
    }
}

