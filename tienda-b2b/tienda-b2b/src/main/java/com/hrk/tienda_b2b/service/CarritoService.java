package com.hrk.tienda_b2b.service;



import com.hrk.tienda_b2b.model.Pedido;

public interface CarritoService {
    Long crearCarrito(Long clienteId);
    void agregarItem(Long carritoId, Long varianteId, int cantidad);
    void quitarItem(Long carritoItemId);
    void vaciar(Long carritoId);
    Pedido convertirACreatePedido(Long carritoId); // crea Pedido DOCUMENTADO
}