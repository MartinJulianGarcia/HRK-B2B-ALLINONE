package com.hrk.tienda_b2b.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "producto_variantes")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProductoVariante {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private String color;
    
    @Column(nullable = false)
    private String talle;

    @Column(nullable = false)
    private Double precio;

    @Version
    private Long version; // para optimistic locking

    @Column(nullable = false)
    private Integer stockDisponible;
}
