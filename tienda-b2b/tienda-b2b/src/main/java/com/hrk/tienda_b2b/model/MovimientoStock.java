package com.hrk.tienda_b2b.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "movimientos_stock")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MovimientoStock {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(name = "variante_id", nullable = false)
    private ProductoVariante variante;

    @ManyToOne @JoinColumn(name = "pedido_id")
    private Pedido pedido; // opcional

    @ManyToOne @JoinColumn(name = "detalle_id")
    private DetallePedido detalle; // opcional

    @Enumerated(EnumType.STRING)
    @Column(length = 50)  // Aumentar el tamaño para permitir valores largos como AJUSTE_INVENTARIO_POSITIVO (25 caracteres)
    private TipoMovimiento tipo;

    private Integer cantidad;
    private LocalDateTime fecha;
}