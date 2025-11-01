package com.hrk.tienda_b2b.service;

import com.hrk.tienda_b2b.model.*;
import com.hrk.tienda_b2b.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Service
@RequiredArgsConstructor
public class PedidoServiceImpl implements PedidoService {

    private final PedidoRepository pedidoRepo;
    private final ProductoVarianteRepository varianteRepo;
    private final DetallePedidoRepository detalleRepo;
    private final MovimientoStockRepository movRepo;
    private final UsuarioRepository usuarioRepo;

    @Override
    @Transactional
    public Pedido crearPedido(Long clienteId, String metodoPago) { // ⭐ NUEVO: Agregar método de pago
        try {
            System.out.println("🔵 [BACKEND] Creando pedido para cliente: " + clienteId);
            System.out.println("🔵 [BACKEND] Método de pago: " + metodoPago); // ⭐ NUEVO LOG

            if (clienteId == null || clienteId <= 0) {
                throw new IllegalArgumentException("ClienteId inválido: " + clienteId);
            }

            Usuario usuario = usuarioRepo.findById(clienteId)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + clienteId));

            // Convertir String a enum MetodoPago
            MetodoPago metodoPagoEnum = MetodoPago.fromString(metodoPago);
            
            Pedido p = Pedido.builder()
                    .clienteId(clienteId)
                    .usuario(usuario)
                    .fecha(LocalDateTime.now())
                    .estado(EstadoPedido.BORRADOR)
                    .total(0.0)
                    .metodoPago(metodoPagoEnum) // ⭐ NUEVO: Guardar método de pago como enum
                    .build();

            Pedido savedPedido = pedidoRepo.save(p);
            System.out.println("🔵 [BACKEND] Pedido creado exitosamente con ID: " + savedPedido.getId());

            return savedPedido;
        } catch (Exception e) {
            System.err.println("🔴 [BACKEND] Error al crear pedido: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error interno al crear pedido: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Pedido> obtenerPedidosPorCliente(Long clienteId) {
        return pedidoRepo.findByClienteId(clienteId);
    }

    @Override
    @Transactional
    public Pedido agregarItem(Long pedidoId, Long varianteId, int cantidad) {
        Pedido p = pedidoRepo.findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));

        if (p.getEstado() != EstadoPedido.BORRADOR && p.getEstado() != EstadoPedido.DOCUMENTADO) {
            throw new IllegalStateException("No se pueden agregar items en estado " + p.getEstado());
        }

        ProductoVariante v = varianteRepo.findById(varianteId)
                .orElseThrow(() -> new IllegalArgumentException("Variante no encontrada"));

        Double precioUnitario = v.getPrecio();

        DetallePedido d = DetallePedido.builder()
                .pedido(p)
                .variante(v)
                .cantidad(cantidad)
                .precioUnitario(precioUnitario)
                .build();

        detalleRepo.save(d);
        p.getDetalles().add(d);

        p.setTotal(p.getDetalles().stream()
                .mapToDouble(it -> it.getPrecioUnitario() * it.getCantidad())
                .sum());

        if (p.getEstado() == EstadoPedido.BORRADOR) p.setEstado(EstadoPedido.DOCUMENTADO);

        return p;
    }

    @Override
    @Transactional
    public Pedido confirmar(Long pedidoId) {
        System.out.println("🔵 [BACKEND] Confirmando pedido (descontando stock): " + pedidoId);
        
        Pedido p = pedidoRepo.findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));

        if (p.getEstado() != EstadoPedido.DOCUMENTADO && p.getEstado() != EstadoPedido.BORRADOR) {
            throw new IllegalStateException("El pedido no está listo para confirmar. Estado actual: " + p.getEstado());
        }

        // Forzar carga de detalles usando el repositorio para evitar problemas de lazy loading
        List<DetallePedido> detalles = detalleRepo.findByPedidoId(pedidoId);
        
        if (detalles == null || detalles.isEmpty()) {
            throw new IllegalStateException("El pedido no tiene detalles");
        }
        
        System.out.println("🔵 [BACKEND] Pedido tiene " + detalles.size() + " detalles");

        for (DetallePedido d : detalles) {
            ProductoVariante v = varianteRepo.findById(d.getVariante().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Variante no encontrada"));

            if (v.getStockDisponible() < d.getCantidad()) {
                throw new IllegalStateException("Sin stock para SKU " + v.getSku());
            }

            v.setStockDisponible(v.getStockDisponible() - d.getCantidad());
            varianteRepo.save(v);

            movRepo.save(MovimientoStock.builder()
                    .variante(v)
                    .pedido(p)
                    .detalle(d)
                    .tipo(TipoMovimiento.BAJA_POR_PEDIDO)
                    .cantidad(d.getCantidad())
                    .fecha(LocalDateTime.now())
                    .build());
        }

        p.setEstado(EstadoPedido.CONFIRMADO);
        System.out.println("✅ [BACKEND] Pedido confirmado - Stock descontado");
        return pedidoRepo.save(p);
    }

    @Override
    @Transactional
    public Pedido cancelar(Long pedidoId) {
        System.out.println("🔵 [BACKEND] Cancelando pedido (restaurando stock): " + pedidoId);
        
        Pedido p = pedidoRepo.findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));

        // ⭐ Solo se pueden cancelar pedidos CONFIRMADOS (no ENTREGADOS, esos ya no se cancelan)
        if (p.getEstado() != EstadoPedido.CONFIRMADO) {
            throw new IllegalStateException("Solo se pueden cancelar pedidos CONFIRMADOS. Estado actual: " + p.getEstado() + ". Los pedidos ENTREGADOS no se pueden cancelar.");
        }

        // Forzar carga de detalles usando el repositorio para evitar problemas de lazy loading
        List<DetallePedido> detalles = detalleRepo.findByPedidoId(pedidoId);
        
        if (detalles == null || detalles.isEmpty()) {
            throw new IllegalStateException("El pedido no tiene detalles");
        }
        
        System.out.println("🔵 [BACKEND] Pedido tiene " + detalles.size() + " detalles a cancelar");

        // Restaurar stock de todas las variantes
        for (DetallePedido d : detalles) {
            ProductoVariante v = varianteRepo.findById(d.getVariante().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Variante no encontrada"));

            v.setStockDisponible(v.getStockDisponible() + d.getCantidad());
            varianteRepo.save(v);
            
            System.out.println("🔵 [BACKEND] Stock restaurado para variante " + v.getSku() + ": +" + d.getCantidad());

            movRepo.save(MovimientoStock.builder()
                    .variante(v)
                    .pedido(p)
                    .detalle(d)
                    .tipo(TipoMovimiento.REVERSION_POR_ANULACION)
                    .cantidad(d.getCantidad())
                    .fecha(LocalDateTime.now())
                    .build());
        }

        p.setEstado(EstadoPedido.CANCELADO);
        System.out.println("✅ [BACKEND] Pedido cancelado - Stock restaurado");
        return pedidoRepo.save(p);
    }

    @Override
    @Transactional
    public Pedido confirmarPedido(Long pedidoId) {
        System.out.println("🔵 [BACKEND] Marcando pedido como ENTREGADO: " + pedidoId);
        
        Pedido pedido = pedidoRepo.findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado con ID: " + pedidoId));
        
        // ⭐ Solo se puede marcar como ENTREGADO desde CONFIRMADO
        // El stock ya está descontado en CONFIRMADO, no se toca aquí
        if (pedido.getEstado() != EstadoPedido.CONFIRMADO) {
            throw new IllegalStateException("Solo se pueden marcar como ENTREGADO los pedidos CONFIRMADOS. Estado actual: " + pedido.getEstado());
        }
        
        pedido.setEstado(EstadoPedido.ENTREGADO);
        System.out.println("✅ [BACKEND] Pedido marcado como ENTREGADO - Stock se mantiene descontado");
        return pedidoRepo.save(pedido);
    }

    @Override
    @Transactional
    public Pedido cancelarPedido(Long pedidoId) {
        // ⭐ Este método ya no se usa directamente, se usa cancelar() que restaura stock
        // Pero lo dejamos para compatibilidad, redirige a cancelar()
        System.out.println("🔵 [BACKEND] cancelarPedido() llamado - redirigiendo a cancelar() que restaura stock");
        return cancelar(pedidoId);
    }

    @Override
    @Transactional
    public Pedido obtenerPedidoPorId(Long pedidoId) {
        System.out.println("🔵 [BACKEND] Obteniendo pedido por ID: " + pedidoId);
        
        Pedido pedido = pedidoRepo.findById(pedidoId).orElse(null);
        
        if (pedido != null) {
            System.out.println("🔵 [BACKEND] Pedido encontrado - ID: " + pedido.getId() + 
                ", Estado: " + pedido.getEstado() + ", Tipo: " + pedido.getTipo());
            
            // Forzar carga de detalles para evitar problemas de lazy loading
            List<DetallePedido> detalles = detalleRepo.findByPedidoId(pedidoId);
            System.out.println("🔵 [BACKEND] Pedido tiene " + (detalles != null ? detalles.size() : 0) + " detalles");
        } else {
            System.out.println("🔴 [BACKEND] Pedido no encontrado con ID: " + pedidoId);
        }
        
        return pedido;
    }

    @Override
    public List<Pedido> obtenerTodosLosPedidos() {
        System.out.println("🔵 [BACKEND] Obteniendo todos los pedidos");
        
        return pedidoRepo.findAll();
    }
}