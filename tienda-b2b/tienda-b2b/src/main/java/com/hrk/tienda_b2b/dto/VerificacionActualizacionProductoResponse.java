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
public class VerificacionActualizacionProductoResponse {
    private boolean tieneVariantesConPedidos;
    private int cantidadVariantesConPedidos;
    private List<VarianteConPedidosInfo> variantesConPedidos;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VarianteConPedidosInfo {
        private Long varianteId;
        private String sku;
        private String color;
        private String talle;
        private Integer stockActual;
        private long cantidadPedidos;
    }
}

