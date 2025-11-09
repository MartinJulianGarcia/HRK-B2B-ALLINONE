package com.hrk.tienda_b2b.dto;

import com.hrk.tienda_b2b.model.EstadoPedido;
import com.hrk.tienda_b2b.model.TipoDocumento;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DevolucionResponseDTO {
    private Long id;
    private LocalDateTime fecha;
    private EstadoPedido estado;
    private TipoDocumento tipo;
    private Long pedidoOrigenId;
    private Double total;
    private String metodoPago;
    private Long clienteId;
    
    // Constructor para mapear desde Pedido
    public DevolucionResponseDTO(Long id, LocalDateTime fecha, EstadoPedido estado, 
                                TipoDocumento tipo, Long pedidoOrigenId, Double total, 
                                String metodoPago, Long clienteId) {
        this.id = id;
        this.fecha = fecha;
        this.estado = estado;
        this.tipo = tipo;
        this.pedidoOrigenId = pedidoOrigenId;
        this.total = total;
        this.metodoPago = metodoPago;
        this.clienteId = clienteId;
    }
}






