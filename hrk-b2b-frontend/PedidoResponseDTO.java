package com.hrk.tienda_b2b.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoResponseDTO {
    
    private Long id;
    private Long clienteId;
    private String fecha;
    private Double total;
    private String estado;
    private UsuarioDTO usuario;
    private List<DetalleDTO> detalles;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetalleDTO {
        private Long id;
        private Integer cantidad;
        private Double precioUnitario;
        private VarianteDTO variante;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VarianteDTO {
        private Long id;
        private String sku;
        private String color;
        private String talle;
        private Double precio;
        private Integer stockDisponible;
        private ProductoDTO producto;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductoDTO {
        private Long id;
        private String nombre;
    }
}
