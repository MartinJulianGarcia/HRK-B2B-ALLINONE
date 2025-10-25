package com.hrk.tienda_b2b.model;
import java.util.List;
import java.util.ArrayList;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name = "pedido_detalles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DetallePedido {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public  DetallePedido(Pedido p, ProductoVariante v, Integer cant, Double pre)
    {
        this.pedido=p;
        this.variante=v;
        this.cantidad=cant;
        this.precioUnitario=pre;
    }

//DetallePedido dp = DetallePedido.builder()
        //    .pedido(p)
          //  .variante(ci.getVariante())
          //  .cantidad(ci.getCantidad())
          //  .precioUnitario(precio) // se “congela” el precio
          //  .build();

    @ManyToOne @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne @JoinColumn(name = "variante_id", nullable = false)
    private ProductoVariante variante;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false)
    private Double precioUnitario; // precio capturado al momento
}