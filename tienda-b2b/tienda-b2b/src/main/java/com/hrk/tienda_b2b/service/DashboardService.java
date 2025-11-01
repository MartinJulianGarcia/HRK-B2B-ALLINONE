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
    
}

