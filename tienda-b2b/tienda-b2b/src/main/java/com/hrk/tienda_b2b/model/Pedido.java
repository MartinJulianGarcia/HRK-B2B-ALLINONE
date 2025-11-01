package com.hrk.tienda_b2b.model;
import java.util.List;
import java.util.ArrayList;
import jakarta.persistence.*;
import jakarta.persistence.Convert;
import lombok.*;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "pedidos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Pedido {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    private EstadoPedido estado;

    @Enumerated(EnumType.STRING)
    private TipoDocumento tipo;

    private Long pedidoOrigenId;
    private Double total;

    // ⭐ NUEVO: Método de pago
    @Column(name = "metodo_pago", nullable = true) // Nullable para compatibilidad con datos existentes
    @Convert(converter = com.hrk.tienda_b2b.model.converter.MetodoPagoConverter.class)
    private MetodoPago metodoPago;

    // ⭐ NUEVO: Tipo de aprobación para devoluciones (null si no es devolución o no está aprobada)
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_aprobacion_devolucion")
    private TipoAprobacionDevolucion tipoAprobacionDevolucion;

    // NUEVO: Referencia al Usuario
    @ManyToOne @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // MANTENER por compatibilidad
    @Column(name = "cliente_id") // @Deprecated
    private Long clienteId;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetallePedido> detalles = new ArrayList<>();

    // Constructor para compatibilidad
    public Pedido(Long cl, double tot, LocalDateTime f, EstadoPedido est) {
        this.clienteId = cl;
        this.total = tot;
        this.estado = est;
        this.fecha = f;
    }

    // Constructor nuevo con Usuario
    public Pedido(Usuario usuario, double tot, LocalDateTime f, EstadoPedido est) {
        this.usuario = usuario;
        this.clienteId = usuario.getId(); // Para compatibilidad
        this.total = tot;
        this.estado = est;
        this.fecha = f;
    }



    public void setTotal(Double total) {
        this.total = total;
    }
}