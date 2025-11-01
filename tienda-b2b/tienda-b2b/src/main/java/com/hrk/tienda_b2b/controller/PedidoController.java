package com.hrk.tienda_b2b.controller;

import com.hrk.tienda_b2b.dto.CreatePedidoRequest;
import com.hrk.tienda_b2b.dto.PedidoResponseDTO;
import com.hrk.tienda_b2b.service.PedidoService;
import com.hrk.tienda_b2b.model.Pedido;
import com.hrk.tienda_b2b.model.DetallePedido;
import com.hrk.tienda_b2b.model.EstadoPedido;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pedidos")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;

    @PostMapping("/crear")
    public ResponseEntity<?> crearPedido(@RequestBody CreatePedidoRequest request) {
        try {
            System.out.println("🔵 [PEDIDO CONTROLLER] Request recibido");
            System.out.println("🔵 [PEDIDO CONTROLLER] Cliente ID: " + request.getClienteId());
            System.out.println("🔵 [PEDIDO CONTROLLER] Método de pago: " + request.getMetodoPago()); // ⭐ NUEVO LOG
            System.out.println("🔵 [PEDIDO CONTROLLER] Usuario: " + request.getUsuario());

            // Validar que el cliente existe
            if (request.getClienteId() == null) {
                return ResponseEntity.badRequest().body(crearRespuestaError("Cliente ID es obligatorio"));
            }

            // ⭐ USAR EL SERVICIO REAL CON MÉTODO DE PAGO
            Pedido pedidoCreado = pedidoService.crearPedido(request.getClienteId(), request.getMetodoPago());

            // Convertir a DTO
            PedidoResponseDTO responseDTO = convertirPedidoADTO(pedidoCreado, request.getUsuario());

            System.out.println("✅ [PEDIDO CONTROLLER] Pedido creado exitosamente con ID: " + pedidoCreado.getId());

            return ResponseEntity.ok(responseDTO);

        } catch (Exception e) {
            System.out.println("🔴 [PEDIDO CONTROLLER] Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(crearRespuestaError("Error interno: " + e.getMessage()));
        }
    }

    @PostMapping("/{pedidoId}/items")
    public ResponseEntity<?> agregarItem(
            @PathVariable Long pedidoId,
            @RequestParam Long varianteId,
            @RequestParam Integer cantidad,
            @RequestBody(required = false) Object body) {

        System.out.println("🔵 [PEDIDO CONTROLLER] Agregando item al pedido " + pedidoId +
                " - variante: " + varianteId + ", cantidad: " + cantidad);

        try {
            // ⭐ USAR EL SERVICIO REAL EN LUGAR DE MOCK
            Pedido pedidoActualizado = pedidoService.agregarItem(pedidoId, varianteId, cantidad);

            // Convertir a DTO
            PedidoResponseDTO responseDTO = convertirPedidoADTO(pedidoActualizado, null);

            System.out.println("✅ [PEDIDO CONTROLLER] Item agregado exitosamente");

            return ResponseEntity.ok(responseDTO);

        } catch (Exception e) {
            System.out.println("🔴 [PEDIDO CONTROLLER] Error al agregar item: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(crearRespuestaError("Error al agregar item: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> obtenerPedidosPorCliente(@RequestParam Long clienteId) {
        System.out.println("🔵 [PEDIDO CONTROLLER] Obteniendo pedidos para cliente: " + clienteId);

        try {
            // ⭐ USAR EL SERVICIO REAL EN LUGAR DE MOCK
            List<Pedido> pedidos = pedidoService.obtenerPedidosPorCliente(clienteId);

            // Convertir a DTOs
            List<PedidoResponseDTO> pedidosDTO = pedidos.stream()
                    .map(pedido -> convertirPedidoADTO(pedido, null))
                    .collect(Collectors.toList());

            System.out.println("✅ [PEDIDO CONTROLLER] Encontrados " + pedidos.size() + " pedidos");

            return ResponseEntity.ok(pedidosDTO);

        } catch (Exception e) {
            System.out.println("🔴 [PEDIDO CONTROLLER] Error al obtener pedidos: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(crearRespuestaError("Error al obtener pedidos: " + e.getMessage()));
        }
    }

    @GetMapping("/todos")
    public ResponseEntity<?> obtenerTodosLosPedidos() {
        System.out.println("🔵 [PEDIDO CONTROLLER] Obteniendo todos los pedidos");

        try {
            // Obtener todos los pedidos
            List<Pedido> pedidos = pedidoService.obtenerTodosLosPedidos();

            // Convertir a DTOs
            List<PedidoResponseDTO> pedidosDTO = pedidos.stream()
                    .map(pedido -> convertirPedidoADTO(pedido, null))
                    .collect(Collectors.toList());

            System.out.println("✅ [PEDIDO CONTROLLER] Encontrados " + pedidos.size() + " pedidos en total");

            return ResponseEntity.ok(pedidosDTO);

        } catch (Exception e) {
            System.out.println("🔴 [PEDIDO CONTROLLER] Error al obtener todos los pedidos: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(crearRespuestaError("Error al obtener todos los pedidos: " + e.getMessage()));
        }
    }

    @GetMapping("/{pedidoId}")
    public ResponseEntity<?> obtenerPedidoPorId(@PathVariable Long pedidoId) {
        System.out.println("🔵 [PEDIDO CONTROLLER] Obteniendo pedido por ID: " + pedidoId);

        try {
            // Buscar el pedido por ID
            Pedido pedido = pedidoService.obtenerPedidoPorId(pedidoId);
            
            if (pedido == null) {
                System.out.println("🔴 [PEDIDO CONTROLLER] Pedido no encontrado: " + pedidoId);
                return ResponseEntity.status(404).body(crearRespuestaError("Pedido no encontrado"));
            }

            // Convertir a DTO
            PedidoResponseDTO pedidoDTO = convertirPedidoADTO(pedido, null);

            System.out.println("✅ [PEDIDO CONTROLLER] Pedido encontrado: " + pedido.getId());

            return ResponseEntity.ok(pedidoDTO);

        } catch (Exception e) {
            System.out.println("🔴 [PEDIDO CONTROLLER] Error al obtener pedido: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(crearRespuestaError("Error al obtener pedido: " + e.getMessage()));
        }
    }

    // Método helper para convertir Pedido a PedidoResponseDTO
    private PedidoResponseDTO convertirPedidoADTO(Pedido pedido, CreatePedidoRequest.UsuarioInfoDTO usuarioInfo) {
        try {
            System.out.println("🔵 [PEDIDO CONTROLLER] Convirtiendo pedido a DTO: " + pedido.getId());

            // Convertir detalles del pedido
            List<PedidoResponseDTO.PedidoDetalleResponseDTO> detallesDTO = new ArrayList<>();

            if (pedido.getDetalles() != null && !pedido.getDetalles().isEmpty()) {
                System.out.println("🔵 [PEDIDO CONTROLLER] Procesando " + pedido.getDetalles().size() + " detalles");

                for (DetallePedido detalle : pedido.getDetalles()) {
                    System.out.println("🔵 [PEDIDO CONTROLLER] Procesando detalle: " + detalle.getId() +
                            " - Variante: " + detalle.getVariante().getId() +
                            " - Cantidad: " + detalle.getCantidad());

                    PedidoResponseDTO.PedidoDetalleResponseDTO detalleDTO = PedidoResponseDTO.PedidoDetalleResponseDTO.builder()
                            .id(detalle.getId())
                            .cantidad(detalle.getCantidad())
                            .precioUnitario(detalle.getPrecioUnitario())
                            .variante(PedidoResponseDTO.VarianteResponseDTO.builder()
                                    .id(detalle.getVariante().getId())
                                    .sku(detalle.getVariante().getSku())
                                    .color(detalle.getVariante().getColor())
                                    .talle(detalle.getVariante().getTalle())
                                    .precio(detalle.getVariante().getPrecio())
                                    .stockDisponible(detalle.getVariante().getStockDisponible())
                                    .producto(PedidoResponseDTO.ProductoResponseDTO.builder()
                                            .id(detalle.getVariante().getProducto().getId())
                                            .nombre(detalle.getVariante().getProducto().getNombre())
                                            .build())
                                    .build())
                            .build();

                    detallesDTO.add(detalleDTO);
                }
            } else {
                System.out.println("🟡 [PEDIDO CONTROLLER] Pedido sin detalles");
            }

            // Crear DTO con detalles
            PedidoResponseDTO responseDTO = PedidoResponseDTO.builder()
                    .id(pedido.getId())
                    .clienteId(pedido.getClienteId())
                    .fecha(pedido.getFecha().toString())
                    .estado(pedido.getEstado().toString())
                    .tipo(pedido.getTipo() != null ? pedido.getTipo().toString() : "PEDIDO") // ⭐ NUEVO: Incluir tipo de documento
                    .tipoAprobacionDevolucion(pedido.getTipoAprobacionDevolucion() != null ? pedido.getTipoAprobacionDevolucion().toString() : null) // ⭐ NUEVO: Tipo de aprobación para devoluciones
                    .total(pedido.getTotal())
                    .metodoPago(pedido.getMetodoPago() != null ? pedido.getMetodoPago().toString() : null) // ⭐ NUEVO: Incluir método de pago (convertir enum a String)
                    .detalles(detallesDTO)
                    .usuario(usuarioInfo != null ?
                            com.hrk.tienda_b2b.dto.UsuarioDTO.builder()
                                    .id(pedido.getUsuario() != null ? pedido.getUsuario().getId() : 1L)
                                    .nombreRazonSocial(usuarioInfo.getNombreRazonSocial())
                                    .email(usuarioInfo.getEmail())
                                    .build() :
                            // Si no hay usuarioInfo, usar la información del usuario del pedido
                            pedido.getUsuario() != null ?
                                    com.hrk.tienda_b2b.dto.UsuarioDTO.builder()
                                            .id(pedido.getUsuario().getId())
                                            .nombreRazonSocial(pedido.getUsuario().getNombreRazonSocial())
                                            .email(pedido.getUsuario().getEmail())
                                            .build() : null)
                    .build();

            System.out.println("🔵 [PEDIDO CONTROLLER] Pedido tiene " + (pedido.getDetalles() != null ? pedido.getDetalles().size() : 0) + " detalles");
            return responseDTO;

        } catch (Exception e) {
            System.err.println("🔴 [PEDIDO CONTROLLER] Error al convertir pedido a DTO: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al convertir pedido: " + e.getMessage(), e);
        }
    }

    @PostMapping("/{pedidoId}/confirmar")
    public ResponseEntity<?> confirmarPedido(@PathVariable Long pedidoId) {
        try {
            System.out.println("🔵 [PEDIDO CONTROLLER] Procesando confirmación para pedido: " + pedidoId);
            
            // Primero obtener el pedido para ver su estado
            Pedido pedidoActual = pedidoService.obtenerPedidoPorId(pedidoId);
            if (pedidoActual == null) {
                throw new IllegalArgumentException("Pedido no encontrado con ID: " + pedidoId);
            }
            
            // ⭐ Si está en DOCUMENTADO o BORRADOR, confirmar (descontar stock)
            // Si está en CONFIRMADO, marcar como ENTREGADO (solo cambiar estado)
            if (pedidoActual.getEstado() == EstadoPedido.DOCUMENTADO || 
                pedidoActual.getEstado() == EstadoPedido.BORRADOR) {
                System.out.println("🔵 [PEDIDO CONTROLLER] Confirmando pedido y descontando stock...");
                Pedido pedido = pedidoService.confirmar(pedidoId);
                PedidoResponseDTO responseDTO = convertirPedidoADTO(pedido, null);
                System.out.println("✅ [PEDIDO CONTROLLER] Pedido confirmado - Stock descontado");
                return ResponseEntity.ok(responseDTO);
            } else if (pedidoActual.getEstado() == EstadoPedido.CONFIRMADO) {
                System.out.println("🔵 [PEDIDO CONTROLLER] Marcando pedido como ENTREGADO...");
                Pedido pedido = pedidoService.confirmarPedido(pedidoId);
                PedidoResponseDTO responseDTO = convertirPedidoADTO(pedido, null);
                System.out.println("✅ [PEDIDO CONTROLLER] Pedido marcado como ENTREGADO");
                return ResponseEntity.ok(responseDTO);
            } else {
                throw new IllegalStateException("No se puede confirmar un pedido en estado: " + pedidoActual.getEstado());
            }
            
        } catch (IllegalArgumentException e) {
            System.out.println("🔴 [PEDIDO CONTROLLER] Pedido no encontrado: " + e.getMessage());
            return ResponseEntity.status(404).body(crearRespuestaError("Pedido no encontrado: " + e.getMessage()));
        } catch (IllegalStateException e) {
            System.out.println("🔴 [PEDIDO CONTROLLER] Estado inválido: " + e.getMessage());
            return ResponseEntity.status(400).body(crearRespuestaError(e.getMessage()));
        } catch (Exception e) {
            System.out.println("🔴 [PEDIDO CONTROLLER] Error al confirmar pedido: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(crearRespuestaError("Error al confirmar pedido: " + e.getMessage()));
        }
    }

    @PostMapping("/{pedidoId}/cancelar")
    public ResponseEntity<?> cancelarPedido(@PathVariable Long pedidoId) {
        try {
            System.out.println("🔵 [PEDIDO CONTROLLER] Cancelando pedido (con restauración de stock): " + pedidoId);
            
            // ⭐ IMPORTANTE: Usar cancelar() en lugar de cancelarPedido() para que restaure stock
            Pedido pedido = pedidoService.cancelar(pedidoId);
            PedidoResponseDTO responseDTO = convertirPedidoADTO(pedido, null);
            
            System.out.println("✅ [PEDIDO CONTROLLER] Pedido cancelado exitosamente - Stock restaurado");
            return ResponseEntity.ok(responseDTO);
            
        } catch (IllegalArgumentException e) {
            System.out.println("🔴 [PEDIDO CONTROLLER] Pedido no encontrado: " + e.getMessage());
            return ResponseEntity.status(404).body(crearRespuestaError("Pedido no encontrado: " + e.getMessage()));
        } catch (IllegalStateException e) {
            System.out.println("🔴 [PEDIDO CONTROLLER] Estado inválido: " + e.getMessage());
            return ResponseEntity.status(400).body(crearRespuestaError(e.getMessage()));
        } catch (Exception e) {
            System.out.println("🔴 [PEDIDO CONTROLLER] Error al cancelar pedido: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(crearRespuestaError("Error al cancelar pedido: " + e.getMessage()));
        }
    }

    private Map<String, String> crearRespuestaError(String mensaje) {
        Map<String, String> error = new HashMap<>();
        error.put("error", mensaje);
        return error;
    }
}