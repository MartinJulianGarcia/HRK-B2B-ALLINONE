package com.hrk.tienda_b2b.service;

import com.hrk.tienda_b2b.dto.CreatePedidoRequest;
import com.hrk.tienda_b2b.dto.PedidoResponseDTO;
import com.hrk.tienda_b2b.dto.UsuarioDTO;
import com.hrk.tienda_b2b.model.*;
import com.hrk.tienda_b2b.repository.PedidoRepository;
import com.hrk.tienda_b2b.repository.UsuarioRepository;
import com.hrk.tienda_b2b.repository.ProductoVarianteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PedidoService {
    
    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoVarianteRepository productoVarianteRepository;

    public PedidoResponseDTO crearPedido(CreatePedidoRequest request) {
        System.out.println("🔵 [PEDIDO SERVICE] Creando pedido para cliente: " + request.getClienteId());
        
        // Buscar el usuario/cliente
        Usuario cliente = usuarioRepository.findById(request.getClienteId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + request.getClienteId()));
        
        System.out.println("🔵 [PEDIDO SERVICE] Cliente encontrado: " + cliente.getNombreRazonSocial());
        
        // Crear el pedido
        Pedido pedido = new Pedido();
        pedido.setClienteId(request.getClienteId());
        pedido.setUsuario(cliente);
        pedido.setFecha(LocalDateTime.now());
        pedido.setEstado(EstadoPedido.BORRADOR);
        pedido.setTotal(0.0); // Se calculará cuando se agreguen items
        
        // Guardar el pedido
        Pedido pedidoGuardado = pedidoRepository.save(pedido);
        System.out.println("🔵 [PEDIDO SERVICE] Pedido guardado con ID: " + pedidoGuardado.getId());
        
        // Convertir a DTO y retornar
        return convertirADTO(pedidoGuardado);
    }

    public PedidoResponseDTO agregarItem(Long pedidoId, Long varianteId, Integer cantidad) {
        System.out.println("🔵 [PEDIDO SERVICE] Agregando item al pedido " + pedidoId + 
                          " - variante: " + varianteId + ", cantidad: " + cantidad);
        
        // Buscar el pedido
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado con ID: " + pedidoId));
        
        // Buscar la variante
        ProductoVariante variante = productoVarianteRepository.findById(varianteId)
                .orElseThrow(() -> new RuntimeException("Variante no encontrada con ID: " + varianteId));
        
        // Crear el detalle del pedido
        PedidoDetalle detalle = new PedidoDetalle();
        detalle.setPedido(pedido);
        detalle.setVariante(variante);
        detalle.setCantidad(cantidad);
        detalle.setPrecioUnitario(variante.getPrecio());
        
        // Agregar el detalle al pedido
        pedido.getDetalles().add(detalle);
        
        // Recalcular el total
        double nuevoTotal = pedido.getDetalles().stream()
                .mapToDouble(d -> d.getCantidad() * d.getPrecioUnitario())
                .sum();
        pedido.setTotal(nuevoTotal);
        
        // Guardar los cambios
        Pedido pedidoActualizado = pedidoRepository.save(pedido);
        System.out.println("🔵 [PEDIDO SERVICE] Item agregado. Nuevo total: " + nuevoTotal);
        
        return convertirADTO(pedidoActualizado);
    }

    public List<PedidoResponseDTO> obtenerPedidosPorCliente(Long clienteId) {
        System.out.println("🔵 [PEDIDO SERVICE] Obteniendo pedidos para cliente: " + clienteId);
        
        List<Pedido> pedidos = pedidoRepository.findByClienteIdOrderByFechaDesc(clienteId);
        System.out.println("🔵 [PEDIDO SERVICE] Encontrados " + pedidos.size() + " pedidos");
        
        return pedidos.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    public PedidoResponseDTO confirmarPedido(Long pedidoId) {
        System.out.println("🔵 [PEDIDO SERVICE] Confirmando pedido: " + pedidoId);
        
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado con ID: " + pedidoId));
        
        pedido.setEstado(EstadoPedido.CONFIRMADO);
        Pedido pedidoActualizado = pedidoRepository.save(pedido);
        
        System.out.println("🔵 [PEDIDO SERVICE] Pedido confirmado exitosamente");
        return convertirADTO(pedidoActualizado);
    }

    public PedidoResponseDTO cancelarPedido(Long pedidoId) {
        System.out.println("🔵 [PEDIDO SERVICE] Cancelando pedido: " + pedidoId);
        
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado con ID: " + pedidoId));
        
        pedido.setEstado(EstadoPedido.CANCELADO);
        Pedido pedidoActualizado = pedidoRepository.save(pedido);
        
        System.out.println("🔵 [PEDIDO SERVICE] Pedido cancelado exitosamente");
        return convertirADTO(pedidoActualizado);
    }

    private PedidoResponseDTO convertirADTO(Pedido pedido) {
        PedidoResponseDTO dto = new PedidoResponseDTO();
        dto.setId(pedido.getId());
        dto.setClienteId(pedido.getClienteId());
        dto.setFecha(pedido.getFecha().toString());
        dto.setTotal(pedido.getTotal());
        dto.setEstado(pedido.getEstado().toString());
        
        // Agregar información del usuario si está disponible
        if (pedido.getUsuario() != null) {
            UsuarioDTO usuarioDTO = UsuarioDTO.fromEntity(pedido.getUsuario());
            dto.setUsuario(usuarioDTO);
        }
        
        // Convertir detalles
        List<PedidoResponseDTO.DetalleDTO> detallesDTO = pedido.getDetalles().stream()
                .map(detalle -> {
                    PedidoResponseDTO.DetalleDTO detalleDTO = new PedidoResponseDTO.DetalleDTO();
                    detalleDTO.setId(detalle.getId());
                    detalleDTO.setCantidad(detalle.getCantidad());
                    detalleDTO.setPrecioUnitario(detalle.getPrecioUnitario());
                    
                    // Información de la variante
                    if (detalle.getVariante() != null) {
                        PedidoResponseDTO.VarianteDTO varianteDTO = new PedidoResponseDTO.VarianteDTO();
                        varianteDTO.setId(detalle.getVariante().getId());
                        varianteDTO.setSku(detalle.getVariante().getSku());
                        varianteDTO.setColor(detalle.getVariante().getColor());
                        varianteDTO.setTalle(detalle.getVariante().getTalle());
                        varianteDTO.setPrecio(detalle.getVariante().getPrecio());
                        varianteDTO.setStockDisponible(detalle.getVariante().getStockDisponible());
                        
                        // Información del producto
                        if (detalle.getVariante().getProducto() != null) {
                            PedidoResponseDTO.ProductoDTO productoDTO = new PedidoResponseDTO.ProductoDTO();
                            productoDTO.setId(detalle.getVariante().getProducto().getId());
                            productoDTO.setNombre(detalle.getVariante().getProducto().getNombre());
                            varianteDTO.setProducto(productoDTO);
                        }
                        
                        detalleDTO.setVariante(varianteDTO);
                    }
                    
                    return detalleDTO;
                })
                .collect(Collectors.toList());
        
        dto.setDetalles(detallesDTO);
        
        return dto;
    }
}
