package com.hrk.tienda_b2b.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "producto_variantes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoVariante {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;
    
    @Column(name = "sku", unique = true, nullable = false)
    private String sku;
    
    @Column(name = "color")
    private String color;
    
    @Column(name = "talle")
    private String talle;
    
    @Column(name = "precio", nullable = false)
    private Double precio;
    
    @Column(name = "stock_disponible")
    private Integer stockDisponible;
}
