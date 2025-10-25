package com.hrk.tienda_b2b.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Entity
@Table(name = "carritos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Carrito {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // NUEVO: Referencia al Usuario
    @ManyToOne @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // MANTENER por compatibilidad durante migración
    @Column(name = "cliente_id") // @Deprecated
    private Long clienteId;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @OneToMany(mappedBy = "carrito", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CarritoItem> items = new ArrayList<>();

    // Constructor para compatibilidad
    public Carrito(Long clienteId, LocalDateTime fechaCreacion) {
        this.clienteId = clienteId;
        this.fechaCreacion = fechaCreacion;
    }

    // Constructor nuevo con Usuario
    public Carrito(Usuario usuario, LocalDateTime fechaCreacion) {
        this.usuario = usuario;
        this.clienteId = usuario.getId(); // Para compatibilidad
        this.fechaCreacion = fechaCreacion;
    }

    public Long getId() {
        return id;
    }

    public List<CarritoItem> getItems() {
        return items;
    }
}