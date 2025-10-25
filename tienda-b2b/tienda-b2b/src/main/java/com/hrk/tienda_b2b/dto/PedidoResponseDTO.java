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
public class PedidoResponseDTO {
    private Long id;
    private Long clienteId;
    private String fecha;
    private String estado;
    private Double total;
    private String metodoPago; // ⭐ NUEVO: Método de pago
    private UsuarioDTO usuario;
    private List<PedidoDetalleResponseDTO> detalles;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PedidoDetalleResponseDTO {
        private Long id;
        private Integer cantidad;
        private Double precioUnitario;
        private VarianteResponseDTO variante;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VarianteResponseDTO {
        private Long id;
        private String sku;
        private String color;
        private String talle;
        private Double precio;
        private Integer stockDisponible;
        private ProductoResponseDTO producto;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductoResponseDTO {
        private Long id;
        private String nombre;
    }
}