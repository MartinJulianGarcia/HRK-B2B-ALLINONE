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
}