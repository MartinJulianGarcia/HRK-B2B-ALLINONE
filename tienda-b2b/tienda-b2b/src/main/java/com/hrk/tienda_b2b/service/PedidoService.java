package com.hrk.tienda_b2b.service;
import com.hrk.tienda_b2b.model.Pedido;

import java.util.List;

public interface PedidoService {
    Pedido crearPedido(Long clienteId, String metodoPago); // ⭐ NUEVO: Agregar método de pago
    Pedido agregarItem(Long pedidoId, Long varianteId, int cantidad);
    Pedido confirmar(Long pedidoId);
    Pedido cancelar(Long pedidoId);
    Pedido confirmarPedido(Long pedidoId); // Para marcar como ENTREGADO
    Pedido cancelarPedido(Long pedidoId); // Para marcar como CANCELADO
    List<Pedido> obtenerPedidosPorCliente(Long clienteId);
}