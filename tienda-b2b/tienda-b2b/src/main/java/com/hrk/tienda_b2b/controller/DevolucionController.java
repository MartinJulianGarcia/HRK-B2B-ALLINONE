package com.hrk.tienda_b2b.controller;


import com.hrk.tienda_b2b.dto.CreateDevolucionRequest;
import com.hrk.tienda_b2b.dto.DevolucionResponseDTO;
import com.hrk.tienda_b2b.dto.PedidoResponseDTO;
import com.hrk.tienda_b2b.dto.UsuarioDTO;
import com.hrk.tienda_b2b.model.Pedido;
import com.hrk.tienda_b2b.model.DetallePedido;
import com.hrk.tienda_b2b.service.DevolucionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/devoluciones")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DevolucionController {

    private final DevolucionService devolucionService;

    @PostMapping("/crear")
    public ResponseEntity<DevolucionResponseDTO> crear(@RequestParam Long clienteId,
                                                      @RequestParam(required = false) Long pedidoOrigenId) {
        Pedido devolucion = devolucionService.crearDevolucion(clienteId, pedidoOrigenId);
        DevolucionResponseDTO response = new DevolucionResponseDTO(
            devolucion.getId(),
            devolucion.getFecha(),
            devolucion.getEstado(),
            devolucion.getTipo(),
            devolucion.getPedidoOrigenId(),
            devolucion.getTotal(),
            devolucion.getMetodoPago() != null ? devolucion.getMetodoPago().toString() : null,
            devolucion.getClienteId()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/crear-completa")
    public ResponseEntity<Pedido> crearCompleta(@RequestBody CreateDevolucionRequest request) {
        try {
            System.out.println("🔵 [DEVOLUCION CONTROLLER] ===== INICIO CREAR COMPLETA =====");
            System.out.println("🔵 [DEVOLUCION CONTROLLER] Request recibido: " + request);
            System.out.println("🔵 [DEVOLUCION CONTROLLER] ClienteId: " + request.getClienteId());
            System.out.println("🔵 [DEVOLUCION CONTROLLER] Items: " + request.getItems());
            
            if (request.getItems() != null) {
                System.out.println("🔵 [DEVOLUCION CONTROLLER] Cantidad de items: " + request.getItems().size());
                for (int i = 0; i < request.getItems().size(); i++) {
                    CreateDevolucionRequest.ItemDevolucion item = request.getItems().get(i);
                    System.out.println("🔵 [DEVOLUCION CONTROLLER] Item " + i + ": " + item);
                }
            }
            
            // Crear la devolución
            System.out.println("🔵 [DEVOLUCION CONTROLLER] Creando devolución...");
            Pedido devolucion = devolucionService.crearDevolucion(request.getClienteId(), null);
            System.out.println("🔵 [DEVOLUCION CONTROLLER] Devolución creada con ID: " + devolucion.getId());
            
            // Agregar cada item
            if (request.getItems() != null && !request.getItems().isEmpty()) {
                for (CreateDevolucionRequest.ItemDevolucion item : request.getItems()) {
                    try {
                        System.out.println("🔵 [DEVOLUCION CONTROLLER] Agregando item - VarianteId: " + item.getVarianteId() + ", Cantidad: " + item.getCantidad());
                        devolucion = devolucionService.agregarItem(
                            devolucion.getId(), 
                            item.getVarianteId(), 
                            item.getCantidad(), 
                            item.getMotivo()
                        );
                        System.out.println("🔵 [DEVOLUCION CONTROLLER] Item agregado exitosamente");
                    } catch (Exception e) {
                        System.err.println("🔴 [DEVOLUCION CONTROLLER] Error al agregar item: " + e.getMessage());
                        e.printStackTrace();
                        throw e;
                    }
                }
            }
            
            System.out.println("🔵 [DEVOLUCION CONTROLLER] Devolución completa creada exitosamente");
            System.out.println("🔵 [DEVOLUCION CONTROLLER] ===== FIN CREAR COMPLETA =====");
            return ResponseEntity.ok(devolucion);
        } catch (Exception e) {
            System.err.println("🔴 [DEVOLUCION CONTROLLER] Error al crear devolución: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping("/{devId}/items")
    public ResponseEntity<DevolucionResponseDTO> agregarItem(@PathVariable Long devId,
                                                           @RequestParam Long varianteId,
                                                           @RequestParam int cantidad,
                                                           @RequestParam(required = false) String motivo) {
        Pedido devolucion = devolucionService.agregarItem(devId, varianteId, cantidad, motivo);
        DevolucionResponseDTO response = new DevolucionResponseDTO(
            devolucion.getId(),
            devolucion.getFecha(),
            devolucion.getEstado(),
            devolucion.getTipo(),
            devolucion.getPedidoOrigenId(),
            devolucion.getTotal(),
            devolucion.getMetodoPago() != null ? devolucion.getMetodoPago().toString() : null,
            devolucion.getClienteId()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{devId}/aprobar-apta")
    public ResponseEntity<?> aprobarApta(@PathVariable Long devId) {
        try {
            System.out.println("🔵 [DEVOLUCION CONTROLLER] Aprobando devolución como apta: " + devId);
            Pedido pedido = devolucionService.aprobarApta(devId);
            
            // Convertir a DTO para evitar referencias circulares
            PedidoResponseDTO responseDTO = convertirPedidoADTO(pedido);
            
            System.out.println("✅ [DEVOLUCION CONTROLLER] Devolución aprobada como apta exitosamente");
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            System.out.println("🔴 [DEVOLUCION CONTROLLER] Error al aprobar devolución como apta: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(crearRespuestaError("Error al aprobar devolución: " + e.getMessage()));
        }
    }

    @PostMapping("/{devId}/aprobar-scrap")
    public ResponseEntity<?> aprobarScrap(@PathVariable Long devId) {
        try {
            System.out.println("🔵 [DEVOLUCION CONTROLLER] Aprobando devolución como scrap: " + devId);
            Pedido pedido = devolucionService.aprobarScrap(devId);
            
            // Convertir a DTO para evitar referencias circulares
            PedidoResponseDTO responseDTO = convertirPedidoADTO(pedido);
            
            System.out.println("✅ [DEVOLUCION CONTROLLER] Devolución aprobada como scrap exitosamente");
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            System.out.println("🔴 [DEVOLUCION CONTROLLER] Error al aprobar devolución como scrap: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(crearRespuestaError("Error al aprobar devolución: " + e.getMessage()));
        }
    }

    // Método helper para convertir Pedido a PedidoResponseDTO (igual que en PedidoController)
    private PedidoResponseDTO convertirPedidoADTO(Pedido pedido) {
        try {
            System.out.println("🔵 [DEVOLUCION CONTROLLER] Convirtiendo pedido a DTO: " + pedido.getId());

            // Convertir detalles del pedido
            List<PedidoResponseDTO.PedidoDetalleResponseDTO> detallesDTO = new ArrayList<>();

            if (pedido.getDetalles() != null && !pedido.getDetalles().isEmpty()) {
                System.out.println("🔵 [DEVOLUCION CONTROLLER] Procesando " + pedido.getDetalles().size() + " detalles");

                for (DetallePedido detalle : pedido.getDetalles()) {
                    System.out.println("🔵 [DEVOLUCION CONTROLLER] Procesando detalle: " + detalle.getId() +
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
                System.out.println("🟡 [DEVOLUCION CONTROLLER] Pedido sin detalles");
            }

            // Crear DTO con detalles
            PedidoResponseDTO responseDTO = PedidoResponseDTO.builder()
                    .id(pedido.getId())
                    .clienteId(pedido.getClienteId())
                    .fecha(pedido.getFecha().toString())
                    .estado(pedido.getEstado().toString())
                    .tipo(pedido.getTipo() != null ? pedido.getTipo().toString() : "DEVOLUCION")
                    .tipoAprobacionDevolucion(pedido.getTipoAprobacionDevolucion() != null ? pedido.getTipoAprobacionDevolucion().toString() : null) // ⭐ NUEVO: Tipo de aprobación para devoluciones
                    .total(pedido.getTotal())
                    .metodoPago(pedido.getMetodoPago() != null ? pedido.getMetodoPago().toString() : null)
                    .usuario(pedido.getUsuario() != null ?
                            UsuarioDTO.builder()
                                    .id(pedido.getUsuario().getId())
                                    .nombreRazonSocial(pedido.getUsuario().getNombreRazonSocial())
                                    .email(pedido.getUsuario().getEmail())
                                    .cuit(pedido.getUsuario().getCuit())
                                    .build() : null)
                    .detalles(detallesDTO)
                    .build();

            System.out.println("🔵 [DEVOLUCION CONTROLLER] Pedido tiene " + (pedido.getDetalles() != null ? pedido.getDetalles().size() : 0) + " detalles");
            return responseDTO;

        } catch (Exception e) {
            System.err.println("🔴 [DEVOLUCION CONTROLLER] Error al convertir pedido a DTO: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al convertir pedido: " + e.getMessage(), e);
        }
    }

    private Map<String, String> crearRespuestaError(String mensaje) {
        Map<String, String> error = new HashMap<>();
        error.put("error", mensaje);
        return error;
    }

    @GetMapping("/{devId}")
    public ResponseEntity<Pedido> obtener(@PathVariable Long devId) {
        // Si querés un DTO, mapealo aquí.

        return ResponseEntity.ok(/* repo o service */ devolucionService.findById(devId)); // <-- reemplazar por find
    }

    @GetMapping("/disponibilidad")
    public ResponseEntity<Map<String, Integer>> consultarDisponibilidad(
            @RequestParam Long clienteId,
            @RequestParam Long varianteId) {
        Map<String, Integer> disponibilidad = devolucionService.consultarDisponibilidadDevolucion(clienteId, varianteId);
        return ResponseEntity.ok(disponibilidad);
    }
}