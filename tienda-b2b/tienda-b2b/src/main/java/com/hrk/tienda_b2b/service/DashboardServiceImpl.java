package com.hrk.tienda_b2b.service;

import com.hrk.tienda_b2b.model.EstadoPedido;
import com.hrk.tienda_b2b.model.Pedido;
import com.hrk.tienda_b2b.model.TipoDocumento;
import com.hrk.tienda_b2b.model.TipoAprobacionDevolucion;
import com.hrk.tienda_b2b.model.DetallePedido;
import com.hrk.tienda_b2b.model.ProductoVariante;
import com.hrk.tienda_b2b.model.StockHistorico;
import com.hrk.tienda_b2b.model.Producto;
import com.hrk.tienda_b2b.repository.PedidoRepository;
import com.hrk.tienda_b2b.repository.DetallePedidoRepository;
import com.hrk.tienda_b2b.repository.ProductoVarianteRepository;
import com.hrk.tienda_b2b.repository.StockHistoricoRepository;
import com.hrk.tienda_b2b.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final PedidoRepository pedidoRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final ProductoVarianteRepository productoVarianteRepository;
    private final StockHistoricoRepository stockHistoricoRepository;
    private final ProductoRepository productoRepository;

    @Override
    public Long contarOrdenesCanceladas(LocalDateTime desde, LocalDateTime hasta) {
        List<Pedido> pedidos = pedidoRepository.findAll();
        
        return pedidos.stream()
                .filter(p -> p.getEstado() == EstadoPedido.CANCELADO)
                .filter(p -> p.getTipo() != TipoDocumento.DEVOLUCION) // Solo pedidos de compra, no devoluciones
                .filter(p -> {
                    if (desde != null && p.getFecha().isBefore(desde)) return false;
                    if (hasta != null && p.getFecha().isAfter(hasta)) return false;
                    return true;
                })
                .count();
    }

    @Override
    public Map<String, Object> obtenerMedioPagoMasUsado(LocalDateTime desde, LocalDateTime hasta) {
        List<Pedido> pedidos = pedidoRepository.findAll();
        
        Map<String, Long> conteoPorMetodo = pedidos.stream()
                .filter(p -> p.getEstado() == EstadoPedido.CONFIRMADO || p.getEstado() == EstadoPedido.ENTREGADO)
                .filter(p -> p.getTipo() != TipoDocumento.DEVOLUCION)
                .filter(p -> {
                    if (desde != null && p.getFecha().isBefore(desde)) return false;
                    if (hasta != null && p.getFecha().isAfter(hasta)) return false;
                    return true;
                })
                .filter(p -> p.getMetodoPago() != null && !p.getMetodoPago().trim().isEmpty())
                .collect(Collectors.groupingBy(
                        p -> p.getMetodoPago(),
                        Collectors.counting()
                ));
        
        // Encontrar el método más usado
        String metodoMasUsado = conteoPorMetodo.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
        
        Long cantidad = conteoPorMetodo.getOrDefault(metodoMasUsado, 0L);
        
        Map<String, Object> resultado = new HashMap<>();
        resultado.put("metodo", metodoMasUsado);
        resultado.put("cantidad", cantidad);
        
        return resultado;
    }

    @Override
    public Map<String, Long> contarDevoluciones(LocalDateTime desde, LocalDateTime hasta) {
        List<Pedido> devoluciones = pedidoRepository.findAll();
        
        long aptas = devoluciones.stream()
                .filter(p -> p.getTipo() == TipoDocumento.DEVOLUCION)
                .filter(p -> p.getTipoAprobacionDevolucion() == TipoAprobacionDevolucion.APTA)
                .filter(p -> {
                    if (desde != null && p.getFecha().isBefore(desde)) return false;
                    if (hasta != null && p.getFecha().isAfter(hasta)) return false;
                    return true;
                })
                .count();
        
        long scrap = devoluciones.stream()
                .filter(p -> p.getTipo() == TipoDocumento.DEVOLUCION)
                .filter(p -> p.getTipoAprobacionDevolucion() == TipoAprobacionDevolucion.SCRAP)
                .filter(p -> {
                    if (desde != null && p.getFecha().isBefore(desde)) return false;
                    if (hasta != null && p.getFecha().isAfter(hasta)) return false;
                    return true;
                })
                .count();
        
        // Incluir devoluciones pendientes (sin aprobación aún)
        long pendientes = devoluciones.stream()
                .filter(p -> p.getTipo() == TipoDocumento.DEVOLUCION)
                .filter(p -> p.getTipoAprobacionDevolucion() == null)
                .filter(p -> {
                    if (desde != null && p.getFecha().isBefore(desde)) return false;
                    if (hasta != null && p.getFecha().isAfter(hasta)) return false;
                    return true;
                })
                .count();
        
        Map<String, Long> resultado = new HashMap<>();
        resultado.put("aptas", aptas);
        resultado.put("scrap", scrap);
        resultado.put("pendientes", pendientes);
        resultado.put("total", aptas + scrap + pendientes);
        
        return resultado;
    }

    @Override
    public Map<String, Object> calcularPorcentajeVendidoStockHistorico(LocalDateTime desde, LocalDateTime hasta) {
        // Por defecto: últimos 3 meses si no se especifica
        final LocalDateTime fechaDesde = (desde != null) ? desde : LocalDateTime.now().minusMonths(3);
        final LocalDateTime fechaHasta = (hasta != null) ? hasta : LocalDateTime.now();
        
        // Obtener todos los pedidos CONFIRMADOS o ENTREGADOS en el período
        List<Pedido> pedidos = pedidoRepository.findAll().stream()
                .filter(p -> (p.getEstado() == EstadoPedido.CONFIRMADO || p.getEstado() == EstadoPedido.ENTREGADO))
                .filter(p -> p.getTipo() != TipoDocumento.DEVOLUCION)
                .filter(p -> {
                    if (fechaDesde != null && p.getFecha().isBefore(fechaDesde)) return false;
                    if (fechaHasta != null && p.getFecha().isAfter(fechaHasta)) return false;
                    return true;
                })
                .collect(Collectors.toList());
        
        // Sumar todas las ventas (cantidad vendida por variante)
        Map<Long, Integer> ventasPorVariante = new HashMap<>();
        for (Pedido pedido : pedidos) {
            List<DetallePedido> detalles = detallePedidoRepository.findByPedidoId(pedido.getId());
            for (DetallePedido detalle : detalles) {
                if (detalle.getVariante() != null) {
                    Long varianteId = detalle.getVariante().getId();
                    ventasPorVariante.put(varianteId, 
                        ventasPorVariante.getOrDefault(varianteId, 0) + detalle.getCantidad());
                }
            }
        }
        
        // Descontar devoluciones (aptas, scrap y pendientes) del período
        List<Pedido> devoluciones = pedidoRepository.findAll().stream()
                .filter(p -> p.getTipo() == TipoDocumento.DEVOLUCION)
                .filter(p -> {
                    if (fechaDesde != null && p.getFecha().isBefore(fechaDesde)) return false;
                    if (fechaHasta != null && p.getFecha().isAfter(fechaHasta)) return false;
                    return true;
                })
                .collect(Collectors.toList());
        
        // Descontar devoluciones de las ventas
        for (Pedido devolucion : devoluciones) {
            List<DetallePedido> detallesDev = detallePedidoRepository.findByPedidoId(devolucion.getId());
            for (DetallePedido detalle : detallesDev) {
                if (detalle.getVariante() != null) {
                    Long varianteId = detalle.getVariante().getId();
                    int cantidadDevuelta = detalle.getCantidad();
                    // Descontar de las ventas (puede quedar negativo si hay más devoluciones que ventas)
                    ventasPorVariante.put(varianteId, 
                        Math.max(0, ventasPorVariante.getOrDefault(varianteId, 0) - cantidadDevuelta));
                }
            }
        }
        
        // Calcular stock histórico total: sumar todas las entradas (stock inicial + ajustes positivos)
        int totalStockHistorico = 0;
        int totalVendido = 0;
        
        List<ProductoVariante> todasVariantes = productoVarianteRepository.findAll();
        for (ProductoVariante variante : todasVariantes) {
            // Obtener stock histórico: suma de ENTRADA_INICIAL + AJUSTE_SUMA - AJUSTE_RESTA
            List<StockHistorico> historicos = stockHistoricoRepository.findByVarianteOrderByFechaAsc(variante);
            int stockHistoricoVariante = 0;
            for (StockHistorico historico : historicos) {
                if (historico.getTipo() == StockHistorico.TipoMovimientoStock.ENTRADA_INICIAL || 
                    historico.getTipo() == StockHistorico.TipoMovimientoStock.AJUSTE_SUMA) {
                    stockHistoricoVariante += historico.getCantidad();
                } else if (historico.getTipo() == StockHistorico.TipoMovimientoStock.AJUSTE_RESTA) {
                    stockHistoricoVariante -= Math.abs(historico.getCantidad());
                }
            }
            
            // Si no hay histórico, usar el stock acumulado del último registro o el stock actual
            if (stockHistoricoVariante == 0 && !historicos.isEmpty()) {
                StockHistorico ultimo = historicos.get(historicos.size() - 1);
                stockHistoricoVariante = ultimo.getStockAcumulado();
            } else if (stockHistoricoVariante == 0) {
                // Si no hay registros históricos, usar stock actual como referencia
                stockHistoricoVariante = variante.getStockDisponible() != null ? variante.getStockDisponible() : 0;
            }
            
            totalStockHistorico += stockHistoricoVariante;
            totalVendido += ventasPorVariante.getOrDefault(variante.getId(), 0);
        }
        
        // Calcular porcentaje
        double porcentajeVendido = 0.0;
        if (totalStockHistorico > 0) {
            porcentajeVendido = (totalVendido * 100.0) / totalStockHistorico;
        }
        
        Map<String, Object> resultado = new HashMap<>();
        resultado.put("porcentajeVendido", Math.round(porcentajeVendido * 100.0) / 100.0); // Redondear a 2 decimales
        resultado.put("totalVendido", totalVendido);
        resultado.put("totalStockHistorico", totalStockHistorico);
        resultado.put("desde", fechaDesde.toString());
        resultado.put("hasta", fechaHasta.toString());
        
        return resultado;
    }

    @Override
    public List<Map<String, Object>> obtenerTopArticulosMasVendidos(int top, LocalDateTime desde, LocalDateTime hasta) {
        // Por defecto: últimos 3 meses si no se especifica
        final LocalDateTime fechaDesde = (desde != null) ? desde : LocalDateTime.now().minusMonths(3);
        final LocalDateTime fechaHasta = (hasta != null) ? hasta : LocalDateTime.now();
        
        // Obtener todos los pedidos CONFIRMADOS o ENTREGADOS en el período
        List<Pedido> pedidos = pedidoRepository.findAll().stream()
                .filter(p -> (p.getEstado() == EstadoPedido.CONFIRMADO || p.getEstado() == EstadoPedido.ENTREGADO))
                .filter(p -> p.getTipo() != TipoDocumento.DEVOLUCION)
                .filter(p -> {
                    if (fechaDesde != null && p.getFecha().isBefore(fechaDesde)) return false;
                    if (fechaHasta != null && p.getFecha().isAfter(fechaHasta)) return false;
                    return true;
                })
                .collect(Collectors.toList());
        
        // Sumar todas las ventas agrupadas por PRODUCTO (no por variante)
        Map<Long, Integer> ventasPorProducto = new HashMap<>();
        Map<Long, String> nombreProducto = new HashMap<>(); // Para almacenar nombres de productos
        
        for (Pedido pedido : pedidos) {
            List<DetallePedido> detalles = detallePedidoRepository.findByPedidoId(pedido.getId());
            for (DetallePedido detalle : detalles) {
                if (detalle.getVariante() != null && detalle.getVariante().getProducto() != null) {
                    Long productoId = detalle.getVariante().getProducto().getId();
                    // Sumar todas las variantes del mismo producto
                    ventasPorProducto.put(productoId, 
                        ventasPorProducto.getOrDefault(productoId, 0) + detalle.getCantidad());
                    
                    // Almacenar nombre del producto (solo una vez, es el mismo para todas las variantes)
                    if (!nombreProducto.containsKey(productoId)) {
                        nombreProducto.put(productoId, detalle.getVariante().getProducto().getNombre());
                    }
                }
            }
        }
        
        // Descontar devoluciones (aptas, scrap y pendientes) del período
        List<Pedido> devoluciones = pedidoRepository.findAll().stream()
                .filter(p -> p.getTipo() == TipoDocumento.DEVOLUCION)
                .filter(p -> {
                    if (fechaDesde != null && p.getFecha().isBefore(fechaDesde)) return false;
                    if (fechaHasta != null && p.getFecha().isAfter(fechaHasta)) return false;
                    return true;
                })
                .collect(Collectors.toList());
        
        // Descontar devoluciones de las ventas (agrupadas por producto)
        for (Pedido devolucion : devoluciones) {
            List<DetallePedido> detallesDev = detallePedidoRepository.findByPedidoId(devolucion.getId());
            for (DetallePedido detalle : detallesDev) {
                if (detalle.getVariante() != null && detalle.getVariante().getProducto() != null) {
                    Long productoId = detalle.getVariante().getProducto().getId();
                    int cantidadDevuelta = detalle.getCantidad();
                    // Descontar de las ventas del producto
                    ventasPorProducto.put(productoId, 
                        Math.max(0, ventasPorProducto.getOrDefault(productoId, 0) - cantidadDevuelta));
                }
            }
        }
        
        // Convertir a lista y ordenar por cantidad vendida (descendente)
        List<Map<String, Object>> topArticulos = ventasPorProducto.entrySet().stream()
                .filter(entry -> entry.getValue() > 0) // Solo incluir si tiene ventas netas positivas
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed()) // Ordenar descendente
                .limit(top) // Limitar al top solicitado
                .map(entry -> {
                    Map<String, Object> articulo = new HashMap<>();
                    articulo.put("productoId", entry.getKey());
                    articulo.put("cantidadVendida", entry.getValue());
                    articulo.put("nombre", nombreProducto.getOrDefault(entry.getKey(), "Producto desconocido"));
                    return articulo;
                })
                .collect(Collectors.toList());
        
        return topArticulos;
    }

    @Override
    public Map<String, Object> obtenerDetallesProducto(Long productoId, LocalDateTime desde, LocalDateTime hasta) {
        // Por defecto: últimos 3 meses si no se especifica
        final LocalDateTime fechaDesde = (desde != null) ? desde : LocalDateTime.now().minusMonths(3);
        final LocalDateTime fechaHasta = (hasta != null) ? hasta : LocalDateTime.now();
        
        // Obtener el producto
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        
        // Calcular stock histórico del producto (suma de todas sus variantes)
        int stockHistoricoTotal = 0;
        List<Map<String, Object>> variantesDetalles = new ArrayList<>();
        
        if (producto.getVariantes() != null) {
            for (ProductoVariante variante : producto.getVariantes()) {
                // Calcular stock histórico de esta variante
                List<StockHistorico> historicos = stockHistoricoRepository.findByVarianteOrderByFechaAsc(variante);
                int stockHistoricoVariante = 0;
                
                for (StockHistorico historico : historicos) {
                    if (historico.getTipo() == StockHistorico.TipoMovimientoStock.ENTRADA_INICIAL || 
                        historico.getTipo() == StockHistorico.TipoMovimientoStock.AJUSTE_SUMA) {
                        stockHistoricoVariante += historico.getCantidad();
                    } else if (historico.getTipo() == StockHistorico.TipoMovimientoStock.AJUSTE_RESTA) {
                        stockHistoricoVariante -= Math.abs(historico.getCantidad());
                    }
                }
                
                // Si no hay histórico, usar el stock acumulado del último registro o el stock actual
                if (stockHistoricoVariante == 0 && !historicos.isEmpty()) {
                    StockHistorico ultimo = historicos.get(historicos.size() - 1);
                    stockHistoricoVariante = ultimo.getStockAcumulado();
                } else if (stockHistoricoVariante == 0) {
                    stockHistoricoVariante = variante.getStockDisponible() != null ? variante.getStockDisponible() : 0;
                }
                
                stockHistoricoTotal += stockHistoricoVariante;
                
                // Calcular ventas de esta variante en el período
                List<Pedido> pedidosVariante = pedidoRepository.findAll().stream()
                        .filter(p -> (p.getEstado() == EstadoPedido.CONFIRMADO || p.getEstado() == EstadoPedido.ENTREGADO))
                        .filter(p -> p.getTipo() != TipoDocumento.DEVOLUCION)
                        .filter(p -> {
                            if (fechaDesde != null && p.getFecha().isBefore(fechaDesde)) return false;
                            if (fechaHasta != null && p.getFecha().isAfter(fechaHasta)) return false;
                            return true;
                        })
                        .collect(Collectors.toList());
                
                int ventasVariante = 0;
                for (Pedido pedido : pedidosVariante) {
                    List<DetallePedido> detalles = detallePedidoRepository.findByPedidoId(pedido.getId());
                    for (DetallePedido detalle : detalles) {
                        if (detalle.getVariante() != null && detalle.getVariante().getId().equals(variante.getId())) {
                            ventasVariante += detalle.getCantidad();
                        }
                    }
                }
                
                // Descontar devoluciones
                List<Pedido> devolucionesVariante = pedidoRepository.findAll().stream()
                        .filter(p -> p.getTipo() == TipoDocumento.DEVOLUCION)
                        .filter(p -> {
                            if (fechaDesde != null && p.getFecha().isBefore(fechaDesde)) return false;
                            if (fechaHasta != null && p.getFecha().isAfter(fechaHasta)) return false;
                            return true;
                        })
                        .collect(Collectors.toList());
                
                for (Pedido devolucion : devolucionesVariante) {
                    List<DetallePedido> detallesDev = detallePedidoRepository.findByPedidoId(devolucion.getId());
                    for (DetallePedido detalle : detallesDev) {
                        if (detalle.getVariante() != null && detalle.getVariante().getId().equals(variante.getId())) {
                            ventasVariante = Math.max(0, ventasVariante - detalle.getCantidad());
                        }
                    }
                }
                
                // Agregar detalles de la variante
                Map<String, Object> detalleVariante = new HashMap<>();
                detalleVariante.put("varianteId", variante.getId());
                detalleVariante.put("sku", variante.getSku());
                detalleVariante.put("color", variante.getColor());
                detalleVariante.put("talle", variante.getTalle());
                detalleVariante.put("stockHistorico", stockHistoricoVariante);
                detalleVariante.put("cantidadVendida", ventasVariante);
                variantesDetalles.add(detalleVariante);
            }
        }
        
        // Calcular ventas totales del producto (suma de todas las variantes)
        int ventasTotales = variantesDetalles.stream()
                .mapToInt(v -> (Integer) v.get("cantidadVendida"))
                .sum();
        
        // Calcular porcentaje vendido
        double porcentajeVendido = 0.0;
        if (stockHistoricoTotal > 0) {
            porcentajeVendido = (ventasTotales * 100.0) / stockHistoricoTotal;
        }
        
        Map<String, Object> resultado = new HashMap<>();
        resultado.put("productoId", producto.getId());
        resultado.put("nombreProducto", producto.getNombre());
        resultado.put("stockHistoricoTotal", stockHistoricoTotal);
        resultado.put("ventasTotales", ventasTotales);
        resultado.put("porcentajeVendido", Math.round(porcentajeVendido * 100.0) / 100.0);
        resultado.put("variantes", variantesDetalles);
        resultado.put("desde", fechaDesde.toString());
        resultado.put("hasta", fechaHasta.toString());
        
        return resultado;
    }
}

