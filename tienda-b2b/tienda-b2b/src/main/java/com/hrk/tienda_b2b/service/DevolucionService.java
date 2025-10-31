package com.hrk.tienda_b2b.service;
import com.hrk.tienda_b2b.model.Pedido;

import java.util.List;
import java.util.Map;

public interface DevolucionService {
    Pedido crearDevolucion(Long clienteId, Long pedidoOrigenId); // tipo=DEVOLUCION
    Pedido agregarItem(Long devolucionId, Long varianteId, int cantidad, String motivo);
    Pedido aprobarApta(Long devolucionId);     // suma stock (DEVOLUCION_ENTRADA)
    Pedido aprobarScrap(Long devolucionId);    // registra scrap (DESPERFECTO_SCRAP)

    Pedido findById(Long devolucionId); // <-- nuevo
    
    // Método para consultar disponibilidad de devolución por variante
    Map<String, Integer> consultarDisponibilidadDevolucion(Long clienteId, Long varianteId);
}