package com.hrk.tienda_b2b.dto;

import com.hrk.tienda_b2b.model.Categoria;
import com.hrk.tienda_b2b.model.TipoProducto;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CreateProductoRequest {
    private String nombre;
    private TipoProducto tipo;
    private Categoria categoria;
    private String sku;
    private List<String> colores;
    private List<String> talles;
    private Double precio;
    private Integer stock;
    private String descripcion;
    private String imagenUrl; // URL de la imagen subida
    private Boolean oculto; // Si está oculto en el catálogo
    
    // Nuevo: Stock por variante (color-talle -> stock)
    private Map<String, Integer> stockPorVariante;
}




