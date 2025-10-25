package com.hrk.tienda_b2b.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "carrito_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CarritoItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(name = "carrito_id", nullable = false)
    private Carrito carrito;

    @ManyToOne @JoinColumn(name = "variante_id", nullable = false)
    private ProductoVariante variante;

    @Column(nullable = false)
    private Integer cantidad;

    // opcional: mostrar subtotal estimado en UI; el precio FIRMADO va en DetallePedido
    @Column(name = "precio_unitario_sugerido")
    private Double precioUnitarioSugerido;

    public Double getPrecioUnitarioSugerido()
    {
        return precioUnitarioSugerido;
    }

    public Integer getCantidad(){return cantidad;};

    public ProductoVariante getVariante() {return variante;}

    public CarritoItem(Carrito c, ProductoVariante v, Integer can) {
        this.carrito = c;
        this.variante=v;
        this.cantidad=can;
    }
}