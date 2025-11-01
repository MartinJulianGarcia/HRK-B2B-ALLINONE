package com.hrk.tienda_b2b.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface DashboardService {
    
    // Métricas simples
    Long contarOrdenesCanceladas(LocalDateTime desde, LocalDateTime hasta);
    
    Map<String, Object> obtenerMedioPagoMasUsado(LocalDateTime desde, LocalDateTime hasta);
    
    Map<String, Long> contarDevoluciones(LocalDateTime desde, LocalDateTime hasta);
    
    // Métricas con stock histórico
    Map<String, Object> calcularPorcentajeVendidoStockHistorico(LocalDateTime desde, LocalDateTime hasta);
    
    // Top artículos más vendidos
    List<Map<String, Object>> obtenerTopArticulosMasVendidos(int top, LocalDateTime desde, LocalDateTime hasta);
    
    // Detalles de un producto específico (stock histórico y ventas por variante)
    Map<String, Object> obtenerDetallesProducto(Long productoId, LocalDateTime desde, LocalDateTime hasta);
    
    // Total facturado en un período
    Map<String, Object> calcularTotalFacturado(LocalDateTime desde, LocalDateTime hasta);
    
    // Top clientes por monto facturado
    List<Map<String, Object>> obtenerTopClientesPorMonto(int top, LocalDateTime desde, LocalDateTime hasta);
    
    // Clientes que no compraron en un período
    Map<String, Object> obtenerClientesSinCompras(LocalDateTime desde, LocalDateTime hasta);
    
    // Obtener última compra de un cliente
    Map<String, Object> obtenerUltimaCompraCliente(Long clienteId);
    
}

