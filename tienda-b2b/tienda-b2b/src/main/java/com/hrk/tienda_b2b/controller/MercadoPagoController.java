package com.hrk.tienda_b2b.controller;

import com.hrk.tienda_b2b.service.MercadoPagoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/mercadopago")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoController {

    private final MercadoPagoService mercadoPagoService;

    /**
     * Endpoint para crear una preferencia de pago en MercadoPago
     */
    @PostMapping("/crear-preferencia/{pedidoId}")
    public ResponseEntity<?> crearPreferencia(@PathVariable Long pedidoId) {
        try {
            log.info("🔵 [MERCADOPAGO] Creando preferencia para pedido: {}", pedidoId);
            
            Map<String, String> preferencia = mercadoPagoService.crearPreferenciaPago(pedidoId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("preferenceId", preferencia.get("preferenceId"));
            response.put("initPoint", preferencia.get("initPoint"));
            response.put("sandboxInitPoint", preferencia.get("sandboxInitPoint"));
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            log.error("🔴 [MERCADOPAGO] Error de configuración: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("🔴 [MERCADOPAGO] Error al crear preferencia: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Error al crear la preferencia de pago: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Endpoint para recibir webhooks de MercadoPago
     */
    @PostMapping("/webhook")
    public ResponseEntity<?> recibirWebhook(@RequestBody Map<String, Object> datos) {
        try {
            log.info("🔵 [MERCADOPAGO] Webhook recibido: {}", datos);
            
            mercadoPagoService.procesarWebhook(datos);
            
            // MercadoPago espera un 200 OK como respuesta
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("🔴 [MERCADOPAGO] Error al procesar webhook: {}", e.getMessage(), e);
            // Aún así devolver 200 para que MercadoPago no reintente
            return ResponseEntity.ok().build();
        }
    }

    /**
     * Endpoint para procesar el retorno del pago (cuando el usuario vuelve de MercadoPago)
     */
    @GetMapping("/procesar-retorno")
    public ResponseEntity<Map<String, Object>> procesarRetorno(
            @RequestParam(required = false) String preference_id,
            @RequestParam(required = false) String payment_status,
            @RequestParam(required = false) String status) {
        
        try {
            log.info("🔵 [MERCADOPAGO] Retorno recibido. preference_id: {}, payment_status: {}, status: {}", 
                    preference_id, payment_status, status);
            
            // MercadoPago puede enviar 'status' o 'payment_status'
            String estadoPago = payment_status != null ? payment_status : status;
            if (estadoPago == null) {
                estadoPago = "pending"; // Por defecto
            }
            
            // Usar preference_id como pedidoId
            if (preference_id == null || preference_id.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "preference_id no proporcionado");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            boolean exito = mercadoPagoService.procesarRetornoPago(preference_id, estadoPago);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", exito);
            response.put("paymentStatus", estadoPago);
            response.put("message", exito ? 
                "Pago procesado exitosamente. El pedido ha sido confirmado." : 
                estadoPago.equals("rejected") || estadoPago.equals("failure") ?
                    "El pago fue rechazado. El pedido ha sido cancelado." :
                    "El pago está pendiente. El pedido permanecerá en estado de borrador hasta que se complete el pago.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("🔴 [MERCADOPAGO] Error al procesar retorno: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Error al procesar el retorno del pago: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}

