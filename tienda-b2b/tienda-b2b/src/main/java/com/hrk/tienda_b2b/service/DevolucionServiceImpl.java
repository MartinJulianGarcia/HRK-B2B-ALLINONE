package com.hrk.tienda_b2b.service;

import com.hrk.tienda_b2b.model.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.hrk.tienda_b2b.repository.PedidoRepository;
import com.hrk.tienda_b2b.repository.ProductoVarianteRepository;
import com.hrk.tienda_b2b.repository.DetallePedidoRepository;
import com.hrk.tienda_b2b.repository.MovimientoStockRepository;
import com.hrk.tienda_b2b.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class DevolucionServiceImpl implements DevolucionService {

    private final PedidoRepository pedidoRepo;
    private final ProductoVarianteRepository varianteRepo;
    private final DetallePedidoRepository detalleRepo;
    private final MovimientoStockRepository movRepo;
    private final UsuarioRepository usuarioRepo;

    @Override @Transactional
    public Pedido crearDevolucion(Long clienteId, Long pedidoOrigenId) {
        System.out.println("🔵 [DEVOLUCION SERVICE] Creando devolución para cliente: " + clienteId + ", pedido origen: " + pedidoOrigenId);
        
        // Buscar el usuario para establecer la relación
        Usuario usuario = usuarioRepo.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        
        // ⭐ Validar que el pedido origen esté ENTREGADO
        if (pedidoOrigenId != null) {
            Pedido pedidoOrigen = pedidoRepo.findById(pedidoOrigenId)
                    .orElseThrow(() -> new IllegalArgumentException("Pedido origen no encontrado con ID: " + pedidoOrigenId));
            
            if (pedidoOrigen.getEstado() != EstadoPedido.ENTREGADO) {
                throw new IllegalStateException("Solo se pueden crear devoluciones de pedidos ENTREGADOS. El pedido origen está en estado: " + pedidoOrigen.getEstado());
            }
            
            System.out.println("✅ [DEVOLUCION SERVICE] Pedido origen validado - Estado: ENTREGADO");
        }
        
        Pedido p = Pedido.builder()
                .clienteId(clienteId)
                .usuario(usuario) // Establecer la relación con Usuario
                .pedidoOrigenId(pedidoOrigenId)
                .tipo(TipoDocumento.DEVOLUCION)
                .estado(EstadoPedido.BORRADOR) // Empieza como BORRADOR, luego DOCUMENTADO cuando se agregan items
                .fecha(LocalDateTime.now())
                .total(0.0) // puede ser 0 si solo registrás físicas; si hacés nota de crédito, calculás
                .build();
        return pedidoRepo.save(p);
    }

    @Override @Transactional
    public Pedido agregarItem(Long devolucionId, Long varianteId, int cantidad, String motivo) {
        System.out.println("🔵 [DEVOLUCION SERVICE] Agregando item a devolución ID: " + devolucionId);
        System.out.println("🔵 [DEVOLUCION SERVICE] VarianteId: " + varianteId + ", Cantidad: " + cantidad);
        
        Pedido p = pedidoRepo.findById(devolucionId)
                .orElseThrow(() -> new IllegalArgumentException("Devolución no encontrada"));

        if (p.getTipo() != TipoDocumento.DEVOLUCION)
            throw new IllegalStateException("El documento no es una devolución");

        if (p.getEstado() != EstadoPedido.DOCUMENTADO && p.getEstado() != EstadoPedido.BORRADOR && p.getEstado() != EstadoPedido.CONFIRMADO)
            throw new IllegalStateException("No se puede editar en estado " + p.getEstado());

        ProductoVariante v = varianteRepo.findById(varianteId)
                .orElseThrow(() -> new IllegalArgumentException("Variante no encontrada"));

        // ⭐ VALIDACIÓN: Verificar que no se exceda el total entregado
        Long clienteId = p.getClienteId();
        int totalEntregado = calcularTotalEntregado(clienteId, varianteId);
        int totalDevueltoOtrasNotas = calcularTotalDevuelto(clienteId, varianteId, devolucionId); // Excluir la devolución actual
        
        // Calcular cantidad ya devuelta en esta devolución (items existentes de la misma variante)
        // Usar el repositorio para asegurar que se cargan todos los detalles correctamente
        int cantidadEnEstaDevolucion = 0;
        List<DetallePedido> detallesDevolucionActual = detalleRepo.findByPedidoId(devolucionId);
        for (DetallePedido detalleExistente : detallesDevolucionActual) {
            if (detalleExistente.getVariante() != null && detalleExistente.getVariante().getId().equals(varianteId)) {
                cantidadEnEstaDevolucion += detalleExistente.getCantidad();
            }
        }
        
        int cantidadAdevolver = cantidad;
        int totalDevuelto = totalDevueltoOtrasNotas + cantidadEnEstaDevolucion;
        int totalQueQuedariaDevuelto = totalDevuelto + cantidadAdevolver;
        
        System.out.println("🔵 [DEVOLUCION SERVICE] Validación de stock:");
        System.out.println("🔵 [DEVOLUCION SERVICE] - Total entregado al cliente: " + totalEntregado);
        System.out.println("🔵 [DEVOLUCION SERVICE] - Total ya devuelto (otras devoluciones): " + totalDevueltoOtrasNotas);
        System.out.println("🔵 [DEVOLUCION SERVICE] - Cantidad ya en esta devolución (misma variante): " + cantidadEnEstaDevolucion);
        System.out.println("🔵 [DEVOLUCION SERVICE] - Cantidad a agregar en esta nota: " + cantidadAdevolver);
        System.out.println("🔵 [DEVOLUCION SERVICE] - Total que quedaría devuelto: " + totalQueQuedariaDevuelto);
        
        if (totalQueQuedariaDevuelto > totalEntregado) {
            int maxDisponible = Math.max(0, totalEntregado - totalDevuelto);
            String mensajeError = String.format(
                "El monto a devolver (%d unidades) excede el total entregado al cliente (%d unidades). " +
                "Ya se han devuelto %d unidades en otras notas de devolución. " +
                "Máximo disponible para devolver: %d unidades.",
                cantidadAdevolver, totalEntregado, totalDevueltoOtrasNotas, maxDisponible
            );
            System.out.println("🔴 [DEVOLUCION SERVICE] " + mensajeError);
            throw new IllegalStateException(mensajeError);
        }

        // El precio ahora siempre está en la variante
        Double precio = v.getPrecio();
        System.out.println("🔵 [DEVOLUCION SERVICE] Precio unitario: " + precio);

        DetallePedido d = DetallePedido.builder()
                .pedido(p)
                .variante(v)
                .cantidad(cantidad)
                .precioUnitario(precio)
                .build();
        // si agregaste campo motivo en DetallePedido: d.setMotivo(motivo);

        detalleRepo.save(d);
        p.getDetalles().add(d);

        p.setTotal(p.getDetalles().stream()
                .mapToDouble(it -> it.getPrecioUnitario() * it.getCantidad())
                .sum());

        System.out.println("🔵 [DEVOLUCION SERVICE] Total actualizado: " + p.getTotal());
        
        // Guardar el pedido actualizado
        Pedido pedidoActualizado = pedidoRepo.save(p);
        System.out.println("🔵 [DEVOLUCION SERVICE] Pedido guardado exitosamente");
        
        return pedidoActualizado;
    }

    @Override @Transactional
    public Pedido aprobarApta(Long devolucionId) {
        Pedido p = pedidoRepo.findById(devolucionId)
                .orElseThrow(() -> new IllegalArgumentException("Devolución no encontrada"));
        if (p.getTipo() != TipoDocumento.DEVOLUCION)
            throw new IllegalStateException("No es una devolución");

        for (DetallePedido d : p.getDetalles()) {
            ProductoVariante v = varianteRepo.findById(d.getVariante().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Variante no encontrada"));

            // suma stock
            v.setStockDisponible(v.getStockDisponible() + d.getCantidad());
            varianteRepo.save(v);

            movRepo.save(MovimientoStock.builder()
                    .variante(v)
                    .pedido(p)
                    .detalle(d)
                    .tipo(TipoMovimiento.DEVOLUCION_ENTRADA)
                    .cantidad(d.getCantidad())
                    .fecha(LocalDateTime.now())
                    .build());
        }
        p.setEstado(EstadoPedido.CONFIRMADO);
        p.setTipoAprobacionDevolucion(TipoAprobacionDevolucion.APTA); // ⭐ Registrar tipo de aprobación
        return pedidoRepo.save(p);
    }

    @Override @Transactional
    public Pedido aprobarScrap(Long devolucionId) {
        Pedido p = pedidoRepo.findById(devolucionId)
                .orElseThrow(() -> new IllegalArgumentException("Devolución no encontrada"));
        if (p.getTipo() != TipoDocumento.DEVOLUCION)
            throw new IllegalStateException("No es una devolución");

        for (DetallePedido d : p.getDetalles()) {
            ProductoVariante v = varianteRepo.findById(d.getVariante().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Variante no encontrada"));

            // no sumo al stock vendible; sólo registro merma (ledger)
            movRepo.save(MovimientoStock.builder()
                    .variante(v)
                    .pedido(p)
                    .detalle(d)
                    .tipo(TipoMovimiento.DESPERFECTO_SCRAP)
                    .cantidad(d.getCantidad())
                    .fecha(LocalDateTime.now())
                    .build());
        }
        p.setEstado(EstadoPedido.CONFIRMADO);
        p.setTipoAprobacionDevolucion(TipoAprobacionDevolucion.SCRAP); // ⭐ Registrar tipo de aprobación
        return pedidoRepo.save(p);
    }

    @Override
    @Transactional
    public Pedido findById(Long devolucionId) {
        return pedidoRepo.findByIdAndTipo(devolucionId, TipoDocumento.DEVOLUCION)
                .orElseThrow(() -> new IllegalArgumentException("Devolución no encontrada"));
    }

    /**
     * Calcula el total entregado al cliente para una variante específica
     * (suma de cantidades en pedidos ENTREGADOS que NO sean devoluciones)
     */
    private int calcularTotalEntregado(Long clienteId, Long varianteId) {
        // Obtener todos los pedidos del cliente que están ENTREGADOS y NO son devoluciones
        List<Pedido> pedidosCliente = pedidoRepo.findByClienteId(clienteId);
        
        System.out.println("🔵 [DEVOLUCION SERVICE] Buscando total entregado - Cliente: " + clienteId + ", Variante: " + varianteId);
        System.out.println("🔵 [DEVOLUCION SERVICE] Total de pedidos del cliente: " + pedidosCliente.size());
        
        int total = 0;
        for (Pedido pedido : pedidosCliente) {
            // Solo contar pedidos ENTREGADOS que NO sean devoluciones
            // (pueden ser VENTA o null, pero NO DEVOLUCION)
            boolean esPedidoNormal = pedido.getTipo() == null || pedido.getTipo() == TipoDocumento.VENTA;
            boolean estaEntregado = pedido.getEstado() == EstadoPedido.ENTREGADO;
            
            System.out.println("🔵 [DEVOLUCION SERVICE] Analizando pedido ID: " + pedido.getId() + 
                ", Tipo: " + pedido.getTipo() + ", Estado: " + pedido.getEstado() + 
                ", Es normal: " + esPedidoNormal + ", Está entregado: " + estaEntregado);
            
            if (esPedidoNormal && estaEntregado) {
                // Buscar directamente los detalles de este pedido y esta variante usando el repositorio
                // Esto evita problemas de lazy loading
                List<DetallePedido> detallesPedido = detalleRepo.findByPedidoId(pedido.getId());
                
                System.out.println("🔵 [DEVOLUCION SERVICE] Pedido " + pedido.getId() + " tiene " + detallesPedido.size() + " detalles");
                
                for (DetallePedido detalle : detallesPedido) {
                    // Acceder al ID de la variante sin cargar completamente la entidad
                    Long detalleVarianteId = detalle.getVariante().getId();
                    
                    System.out.println("🔵 [DEVOLUCION SERVICE] Detalle ID: " + detalle.getId() + 
                        ", Variante ID: " + detalleVarianteId + ", Cantidad: " + detalle.getCantidad());
                    
                    if (detalleVarianteId.equals(varianteId)) {
                        total += detalle.getCantidad();
                        System.out.println("🔵 [DEVOLUCION SERVICE] ✅ Encontrado detalle - Pedido ID: " + pedido.getId() + 
                            ", Variante ID: " + detalleVarianteId + ", Cantidad: " + detalle.getCantidad());
                    }
                }
            }
        }
        
        System.out.println("🔵 [DEVOLUCION SERVICE] ✅ Total entregado calculado: " + total + " para variante " + varianteId + " y cliente " + clienteId);
        return total;
    }


    /**
     * Consulta la disponibilidad de devolución para una variante específica
     * Retorna: totalEntregado, totalDevuelto, disponibleParaDevolver
     */
    @Override
    public Map<String, Integer> consultarDisponibilidadDevolucion(Long clienteId, Long varianteId) {
        int totalEntregado = calcularTotalEntregado(clienteId, varianteId);
        // Para consulta, no excluimos ninguna devolución (queremos el total devuelto real)
        int totalDevuelto = calcularTotalDevuelto(clienteId, varianteId, null);
        int disponibleParaDevolver = Math.max(0, totalEntregado - totalDevuelto);
        
        Map<String, Integer> resultado = new HashMap<>();
        resultado.put("totalEntregado", totalEntregado);
        resultado.put("totalDevuelto", totalDevuelto);
        resultado.put("disponibleParaDevolver", disponibleParaDevolver);
        
        return resultado;
    }
    
    /**
     * Calcula el total ya devuelto al cliente para una variante específica
     * (suma de cantidades en todas las devoluciones, sin importar su estado)
     * Excluye la devolución actual que se está editando (si devolucionActualId no es null)
     */
    private int calcularTotalDevuelto(Long clienteId, Long varianteId, Long devolucionActualId) {
        List<Pedido> pedidosCliente = pedidoRepo.findByClienteId(clienteId);
        
        int total = 0;
        for (Pedido devolucion : pedidosCliente) {
            // Solo contar devoluciones (excluyendo la devolución actual si se especifica)
            if (devolucion.getTipo() == TipoDocumento.DEVOLUCION && 
                (devolucionActualId == null || !devolucion.getId().equals(devolucionActualId))) {
                // Buscar directamente los detalles usando el repositorio para evitar problemas de lazy loading
                List<DetallePedido> detallesDevolucion = detalleRepo.findByPedidoId(devolucion.getId());
                
                for (DetallePedido detalle : detallesDevolucion) {
                    Long detalleVarianteId = detalle.getVariante().getId();
                    
                    if (detalleVarianteId.equals(varianteId)) {
                        total += detalle.getCantidad();
                        System.out.println("🔵 [DEVOLUCION SERVICE] Encontrada devolución - Devolución ID: " + devolucion.getId() + 
                            ", Variante ID: " + detalleVarianteId + ", Cantidad: " + detalle.getCantidad());
                    }
                }
            }
        }
        
        String excluirInfo = devolucionActualId != null ? " (excluyendo devolución " + devolucionActualId + ")" : " (todas las devoluciones)";
        System.out.println("🔵 [DEVOLUCION SERVICE] Total devuelto calculado: " + total + " para variante " + varianteId + " y cliente " + clienteId + excluirInfo);
        return total;
    }
}