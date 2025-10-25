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

            Pedido p = Pedido.builder()
                    .clienteId(clienteId)
                    .usuario(usuario)
                    .fecha(LocalDateTime.now())
                    .estado(EstadoPedido.BORRADOR)
                    .total(0.0)
                    .metodoPago(metodoPago) // ⭐ NUEVO: Guardar método de pago
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
        Pedido p = pedidoRepo.findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));

        if (p.getEstado() != EstadoPedido.DOCUMENTADO && p.getEstado() != EstadoPedido.BORRADOR) {
            throw new IllegalStateException("El pedido no está listo para confirmar");
        }

        for (DetallePedido d : p.getDetalles()) {
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
        return pedidoRepo.save(p);
    }

    @Override
    @Transactional
    public Pedido cancelar(Long pedidoId) {
        Pedido p = pedidoRepo.findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));

        if (p.getEstado() != EstadoPedido.CONFIRMADO) {
            throw new IllegalStateException("Sólo se cancelan pedidos confirmados");
        }

        for (DetallePedido d : p.getDetalles()) {
            ProductoVariante v = varianteRepo.findById(d.getVariante().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Variante no encontrada"));

            v.setStockDisponible(v.getStockDisponible() + d.getCantidad());
            varianteRepo.save(v);

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
        return pedidoRepo.save(p);
    }

    @Override
    @Transactional
    public Pedido confirmarPedido(Long pedidoId) {
        System.out.println("🔵 [BACKEND] Confirmando pedido: " + pedidoId);
        
        Pedido pedido = pedidoRepo.findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado con ID: " + pedidoId));
        
        // Verificar que el pedido esté en estado válido para confirmar
        if (pedido.getEstado() != EstadoPedido.BORRADOR && 
            pedido.getEstado() != EstadoPedido.DOCUMENTADO && 
            pedido.getEstado() != EstadoPedido.CONFIRMADO) {
            throw new IllegalStateException("No se puede confirmar un pedido en estado: " + pedido.getEstado());
        }
        
        pedido.setEstado(EstadoPedido.ENTREGADO);
        return pedidoRepo.save(pedido);
    }

    @Override
    @Transactional
    public Pedido cancelarPedido(Long pedidoId) {
        System.out.println("🔵 [BACKEND] Cancelando pedido: " + pedidoId);
        
        Pedido pedido = pedidoRepo.findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado con ID: " + pedidoId));
        
        // Verificar que el pedido esté en estado válido para cancelar
        if (pedido.getEstado() == EstadoPedido.CANCELADO) {
            throw new IllegalStateException("El pedido ya está cancelado");
        }
        
        // TEMPORAL: Permitir cancelar pedidos entregados para testing
        // TODO: Remover esta validación en producción
        if (pedido.getEstado() == EstadoPedido.ENTREGADO) {
            System.out.println("⚠️ [BACKEND] ADVERTENCIA: Cancelando pedido ya entregado (solo para testing)");
        }
        
        // Log del estado actual para debugging
        System.out.println("🔵 [BACKEND] Estado actual del pedido: " + pedido.getEstado());
        
        pedido.setEstado(EstadoPedido.CANCELADO);
        return pedidoRepo.save(pedido);
    }

    @Override
    public Pedido obtenerPedidoPorId(Long pedidoId) {
        System.out.println("🔵 [BACKEND] Obteniendo pedido por ID: " + pedidoId);
        
        return pedidoRepo.findById(pedidoId).orElse(null);
    }

    @Override
    public List<Pedido> obtenerTodosLosPedidos() {
        System.out.println("🔵 [BACKEND] Obteniendo todos los pedidos");
        
        return pedidoRepo.findAll();
    }
}