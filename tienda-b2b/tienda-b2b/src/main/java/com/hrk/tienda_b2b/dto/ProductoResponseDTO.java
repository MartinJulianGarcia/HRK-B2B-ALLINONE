package com.hrk.tienda_b2b.dto;

import com.hrk.tienda_b2b.model.Categoria;
import com.hrk.tienda_b2b.model.TipoProducto;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProductoResponseDTO {
    private Long id;
    private String nombre;
    private String descripcion;
    private TipoProducto tipo;
    private String imagenUrl;
    private Categoria categoria;
    private List<ProductoVarianteResponseDTO> variantes;
    
    @Data
    @Builder
    public static class ProductoVarianteResponseDTO {
        private Long id;
        private String sku;
        private String color;
        private String talle;
        private Double precio;
        private Integer stockDisponible;
    }
}
