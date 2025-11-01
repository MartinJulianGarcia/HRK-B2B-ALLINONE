package com.hrk.tienda_b2b.controller;

import com.hrk.tienda_b2b.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboards")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/ordenes-canceladas")
    public ResponseEntity<Map<String, Long>> contarOrdenesCanceladas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        
        Long cantidad = dashboardService.contarOrdenesCanceladas(desde, hasta);
        
        return ResponseEntity.ok(Map.of("cantidad", cantidad));
    }

    @GetMapping("/medio-pago-mas-usado")
    public ResponseEntity<Map<String, Object>> obtenerMedioPagoMasUsado(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        
        Map<String, Object> resultado = dashboardService.obtenerMedioPagoMasUsado(desde, hasta);
        
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/devoluciones")
    public ResponseEntity<Map<String, Long>> contarDevoluciones(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        
        Map<String, Long> resultado = dashboardService.contarDevoluciones(desde, hasta);
        
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/porcentaje-vendido-stock-historico")
    public ResponseEntity<Map<String, Object>> calcularPorcentajeVendidoStockHistorico(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        
        Map<String, Object> resultado = dashboardService.calcularPorcentajeVendidoStockHistorico(desde, hasta);
        
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/top-articulos-vendidos")
    public ResponseEntity<List<Map<String, Object>>> obtenerTopArticulosMasVendidos(
            @RequestParam(defaultValue = "10") int top,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        
        List<Map<String, Object>> resultado = dashboardService.obtenerTopArticulosMasVendidos(top, desde, hasta);
        
        return ResponseEntity.ok(resultado);
    }
}

