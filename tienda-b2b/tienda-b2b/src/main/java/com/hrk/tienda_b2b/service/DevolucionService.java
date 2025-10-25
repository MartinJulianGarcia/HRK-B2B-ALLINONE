package com.hrk.tienda_b2b.service;
import com.hrk.tienda_b2b.model.Pedido;

import java.util.List;

public interface DevolucionService {
    Pedido crearDevolucion(Long clienteId, Long pedidoOrigenId); // tipo=DEVOLUCION
    Pedido agregarItem(Long devolucionId, Long varianteId, int cantidad, String motivo);
    Pedido aprobarApta(Long devolucionId);     // suma stock (DEVOLUCION_ENTRADA)
    Pedido aprobarScrap(Long devolucionId);    // registra scrap (DESPERFECTO_SCRAP)

    Pedido findById(Long devolucionId); // <-- nuevo
}