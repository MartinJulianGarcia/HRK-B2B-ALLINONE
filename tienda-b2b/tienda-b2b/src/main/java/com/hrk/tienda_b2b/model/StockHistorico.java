package com.hrk.tienda_b2b.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_historico")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockHistorico {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variante_id", nullable = false)
    private ProductoVariante variante;

    @Column(nullable = false)
    private Integer cantidad; // Puede ser positivo (suma) o negativo (resta)

    @Column(nullable = false)
    private Integer stockAcumulado; // Stock acumulado después de este movimiento

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(length = 100)
    private String motivo; // "Creación inicial", "Ajuste manual", etc.

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private TipoMovimientoStock tipo; // ENTRADA_INICIAL, AJUSTE_SUMA, AJUSTE_RESTA

    public enum TipoMovimientoStock {
        ENTRADA_INICIAL,  // Stock inicial cuando se crea la variante
        AJUSTE_SUMA,      // Suma de stock al modificar
        AJUSTE_RESTA      // Resta de stock al modificar
    }
}

