package com.hrk.tienda_b2b.controller;

import com.hrk.tienda_b2b.dto.CreatePedidoRequest;
import com.hrk.tienda_b2b.dto.PedidoResponseDTO;
import com.hrk.tienda_b2b.service.PedidoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PedidoController {
    
    private final PedidoService pedidoService;

    @PostMapping("/crear")
    public ResponseEntity<PedidoResponseDTO> crearPedido(@RequestBody CreatePedidoRequest request) {
        System.out.println("🔵 [PEDIDO CONTROLLER] Creando pedido para cliente: " + request.getClienteId());
        System.out.println("🔵 [PEDIDO CONTROLLER] Usuario info: " + request.getUsuario());
        
        try {
            PedidoResponseDTO pedido = pedidoService.crearPedido(request);
            System.out.println("🔵 [PEDIDO CONTROLLER] Pedido creado exitosamente: " + pedido.getId());
            return ResponseEntity.ok(pedido);
        } catch (Exception e) {
            System.err.println("🔴 [PEDIDO CONTROLLER] Error al crear pedido: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{pedidoId}/items")
    public ResponseEntity<PedidoResponseDTO> agregarItem(
            @PathVariable Long pedidoId,
            @RequestParam Long varianteId,
            @RequestParam Integer cantidad,
            @RequestBody(required = false) Object body) {
        
        System.out.println("🔵 [PEDIDO CONTROLLER] Agregando item al pedido " + pedidoId + 
                          " - variante: " + varianteId + ", cantidad: " + cantidad);
        
        try {
            PedidoResponseDTO pedido = pedidoService.agregarItem(pedidoId, varianteId, cantidad);
            System.out.println("🔵 [PEDIDO CONTROLLER] Item agregado exitosamente");
            return ResponseEntity.ok(pedido);
        } catch (Exception e) {
            System.err.println("🔴 [PEDIDO CONTROLLER] Error al agregar item: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<PedidoResponseDTO>> obtenerPedidosPorCliente(@RequestParam Long clienteId) {
        System.out.println("🔵 [PEDIDO CONTROLLER] Obteniendo pedidos para cliente: " + clienteId);
        
        try {
            List<PedidoResponseDTO> pedidos = pedidoService.obtenerPedidosPorCliente(clienteId);
            System.out.println("🔵 [PEDIDO CONTROLLER] Encontrados " + pedidos.size() + " pedidos");
            return ResponseEntity.ok(pedidos);
        } catch (Exception e) {
            System.err.println("🔴 [PEDIDO CONTROLLER] Error al obtener pedidos: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{pedidoId}/confirmar")
    public ResponseEntity<PedidoResponseDTO> confirmarPedido(@PathVariable Long pedidoId) {
        System.out.println("🔵 [PEDIDO CONTROLLER] Confirmando pedido: " + pedidoId);
        
        try {
            PedidoResponseDTO pedido = pedidoService.confirmarPedido(pedidoId);
            System.out.println("🔵 [PEDIDO CONTROLLER] Pedido confirmado exitosamente");
            return ResponseEntity.ok(pedido);
        } catch (Exception e) {
            System.err.println("🔴 [PEDIDO CONTROLLER] Error al confirmar pedido: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{pedidoId}/cancelar")
    public ResponseEntity<PedidoResponseDTO> cancelarPedido(@PathVariable Long pedidoId) {
        System.out.println("🔵 [PEDIDO CONTROLLER] Cancelando pedido: " + pedidoId);
        
        try {
            PedidoResponseDTO pedido = pedidoService.cancelarPedido(pedidoId);
            System.out.println("🔵 [PEDIDO CONTROLLER] Pedido cancelado exitosamente");
            return ResponseEntity.ok(pedido);
        } catch (Exception e) {
            System.err.println("🔴 [PEDIDO CONTROLLER] Error al cancelar pedido: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
