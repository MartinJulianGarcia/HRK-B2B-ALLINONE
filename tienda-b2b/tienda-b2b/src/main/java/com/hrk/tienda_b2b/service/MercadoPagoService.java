package com.hrk.tienda_b2b.service;

import com.hrk.tienda_b2b.model.Pedido;
import com.hrk.tienda_b2b.repository.DetallePedidoRepository;
import com.hrk.tienda_b2b.repository.PedidoRepository;
import com.hrk.tienda_b2b.service.PedidoService;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.resources.preference.Preference;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoService {

    private final PedidoRepository pedidoRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final PedidoService pedidoService;

    @Value("${mercadopago.access.token:}")
    private String accessToken;

    @Value("${mercadopago.webhook.url:}")
    private String webhookUrl;

    /**
     * Crea una preferencia de pago en MercadoPago para un pedido
     */
    public Map<String, String> crearPreferenciaPago(Long pedidoId) {
        try {
            // Verificar que el token esté configurado
            if (accessToken == null || accessToken.isEmpty() || accessToken.equals("TU_ACCESS_TOKEN_AQUI")) {
                log.error("🔴 [MERCADOPAGO] Access Token no configurado en application.properties");
                throw new IllegalStateException(
                    "MercadoPago no está configurado. " +
                    "Por favor configura mercadopago.access.token en application.properties con tu Access Token."
                );
            }

            // Configurar el SDK de MercadoPago
            MercadoPagoConfig.setAccessToken(accessToken);

            // Obtener el pedido
            Pedido pedido = pedidoRepository.findById(pedidoId)
                    .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + pedidoId));

            // Crear el cliente de preferencias
            PreferenceClient client = new PreferenceClient();

            // Preparar los items del pedido
            List<PreferenceItemRequest> items = new ArrayList<>();
            
            // Cargar detalles usando el repositorio con JOIN FETCH para evitar problemas de lazy loading
            var detalles = detallePedidoRepository.findByPedidoIdWithRelations(pedidoId);
            
            log.info("🔵 [MERCADOPAGO] Detalles encontrados para pedido {}: {}", pedidoId, detalles != null ? detalles.size() : 0);
            
            if (detalles != null && !detalles.isEmpty()) {
                for (var detalle : detalles) {
                    try {
                        // Validaciones de datos
                        if (detalle.getCantidad() == null || detalle.getCantidad() <= 0) {
                            log.error("🔴 [MERCADOPAGO] Detalle {} tiene cantidad inválida: {}", detalle.getId(), detalle.getCantidad());
                            throw new IllegalStateException("La cantidad del detalle " + detalle.getId() + " es inválida: " + detalle.getCantidad());
                        }
                        
                        if (detalle.getPrecioUnitario() == null || detalle.getPrecioUnitario() <= 0) {
                            log.error("🔴 [MERCADOPAGO] Detalle {} tiene precio inválido: {}", detalle.getId(), detalle.getPrecioUnitario());
                            throw new IllegalStateException("El precio del detalle " + detalle.getId() + " es inválido: " + detalle.getPrecioUnitario());
                        }
                        
                        String nombreProducto = "Producto";
                        
                        // Intentar obtener el nombre del producto de forma segura
                        if (detalle.getVariante() != null) {
                            var variante = detalle.getVariante();
                            if (variante.getProducto() != null) {
                                nombreProducto = variante.getProducto().getNombre();
                                if (nombreProducto == null || nombreProducto.trim().isEmpty()) {
                                    nombreProducto = "Producto";
                                    log.warn("⚠️ [MERCADOPAGO] El producto {} no tiene nombre, usando 'Producto'", variante.getProducto().getId());
                                }
                            } else {
                                log.warn("⚠️ [MERCADOPAGO] La variante {} no tiene producto asociado", variante.getId());
                            }
                        } else {
                            log.warn("⚠️ [MERCADOPAGO] El detalle {} no tiene variante asociada", detalle.getId());
                        }
                        
                        // Limitar el título a 256 caracteres (límite de MercadoPago)
                        if (nombreProducto.length() > 256) {
                            nombreProducto = nombreProducto.substring(0, 253) + "...";
                            log.warn("⚠️ [MERCADOPAGO] Título truncado a 256 caracteres");
                        }
                        
                        BigDecimal precio = BigDecimal.valueOf(detalle.getPrecioUnitario());
                        
                        // Validar que el precio tenga máximo 2 decimales (requerimiento de MercadoPago)
                        precio = precio.setScale(2, RoundingMode.HALF_UP);
                        
                        PreferenceItemRequest item = PreferenceItemRequest.builder()
                                .title(nombreProducto)
                                .quantity(detalle.getCantidad())
                                .unitPrice(precio)
                                .build();
                        items.add(item);
                        log.info("🔵 [MERCADOPAGO] Item agregado: '{}' x {} = ${}", nombreProducto, detalle.getCantidad(), precio);
                    } catch (Exception e) {
                        log.error("🔴 [MERCADOPAGO] Error al procesar detalle {}: {}", detalle.getId(), e.getMessage(), e);
                        throw e; // No continuar si hay un error de validación
                    }
                }
            } else {
                // Si no hay detalles, crear un item único con el total
                Double totalPedido = pedido.getTotal();
                if (totalPedido == null || totalPedido <= 0) {
                    log.error("🔴 [MERCADOPAGO] El pedido {} no tiene total válido: {}", pedidoId, totalPedido);
                    throw new IllegalStateException("El pedido no tiene un total válido para crear la preferencia de pago");
                }
                
                BigDecimal precioTotal = BigDecimal.valueOf(totalPedido);
                precioTotal = precioTotal.setScale(2, RoundingMode.HALF_UP);
                
                PreferenceItemRequest item = PreferenceItemRequest.builder()
                        .title("Pedido #" + pedidoId)
                        .quantity(1)
                        .unitPrice(precioTotal)
                        .build();
                items.add(item);
                log.info("🔵 [MERCADOPAGO] Item único agregado: 'Pedido #{}' x 1 = ${}", pedidoId, precioTotal);
            }

            // URLs de redirección
            // NOTA: MercadoPago requiere HTTPS para backUrls en producción.
            // Para desarrollo local con HTTP, no usaremos backUrls y el usuario volverá manualmente.
            // Si tienes HTTPS configurado (con ngrok o similar), configura MERCADOPAGO_FRONTEND_URL
            String frontendUrl = System.getenv().getOrDefault("MERCADOPAGO_FRONTEND_URL", "http://localhost:4200");
            
            PreferenceBackUrlsRequest backUrls = null;
            
            // Solo usar backUrls si la URL es HTTPS (producción o ngrok)
            if (frontendUrl.startsWith("https://")) {
                backUrls = PreferenceBackUrlsRequest.builder()
                        .success(frontendUrl + "/orders-history?payment_status=success&preference_id=" + pedidoId)
                        .failure(frontendUrl + "/orders-history?payment_status=failure&preference_id=" + pedidoId)
                        .pending(frontendUrl + "/orders-history?payment_status=pending&preference_id=" + pedidoId)
                        .build();
                log.info("🔵 [MERCADOPAGO] Usando backUrls con HTTPS: {}", frontendUrl);
            } else {
                // Para desarrollo local con HTTP, no usar backUrls
                // MercadoPago redirigirá al usuario pero no automáticamente a nuestra app
                // El usuario puede volver manualmente y procesaremos el retorno desde orders-history
                log.info("🔵 [MERCADOPAGO] Modo desarrollo (HTTP). No se usarán backUrls. El usuario deberá volver manualmente.");
            }

            // Validar que tengamos items
            if (items.isEmpty()) {
                log.error("🔴 [MERCADOPAGO] No hay items para crear la preferencia");
                throw new IllegalStateException("No hay items en el pedido para crear la preferencia de pago");
            }
            
            // Log de los items que se van a enviar
            log.info("🔵 [MERCADOPAGO] Creando preferencia con {} items:", items.size());
            for (int i = 0; i < items.size(); i++) {
                PreferenceItemRequest item = items.get(i);
                log.info("🔵 [MERCADOPAGO] Item {}: Título={}, Cantidad={}, Precio={}", 
                        i + 1, item.getTitle(), item.getQuantity(), item.getUnitPrice());
            }

            // Crear la preferencia
            PreferenceRequest.PreferenceRequestBuilder requestBuilder = PreferenceRequest.builder()
                    .items(items)
                    .externalReference(String.valueOf(pedidoId)) // ID del pedido para identificarlo en el webhook
                    .statementDescriptor("HRKB2B"); // Máximo 13 caracteres, solo alfanuméricos, sin espacios
            
            // Solo agregar backUrls y autoReturn si están configuradas (HTTPS)
            // MercadoPago requiere que si usas autoReturn, también debes tener backUrls
            if (backUrls != null) {
                requestBuilder.backUrls(backUrls);
                requestBuilder.autoReturn("approved"); // Redirigir automáticamente cuando se apruebe (solo si hay backUrls)
                log.info("🔵 [MERCADOPAGO] Configurando autoReturn y backUrls (modo producción/HTTPS)");
            } else {
                log.info("🔵 [MERCADOPAGO] No se configurará autoReturn (modo desarrollo HTTP sin backUrls)");
            }
            
            PreferenceRequest preferenceRequest = requestBuilder.build();

            log.info("🔵 [MERCADOPAGO] Enviando solicitud de preferencia a MercadoPago...");
            
            // Crear la preferencia en MercadoPago
            Preference preference = client.create(preferenceRequest);

            log.info("✅ [MERCADOPAGO] Preferencia creada exitosamente. ID: {}, Pedido: {}", 
                    preference.getId(), pedidoId);

            Map<String, String> result = new HashMap<>();
            result.put("preferenceId", preference.getId());
            result.put("initPoint", preference.getInitPoint());
            result.put("sandboxInitPoint", preference.getSandboxInitPoint());
            result.put("pedidoId", String.valueOf(pedidoId)); // Incluir ID del pedido para el callback

            return result;

        } catch (IllegalStateException e) {
            log.error("🔴 [MERCADOPAGO] Error de configuración: {}", e.getMessage());
            throw e;
        } catch (MPApiException e) {
            // Error específico de la API de MercadoPago
            log.error("🔴 [MERCADOPAGO] Error de API de MercadoPago:");
            log.error("🔴 [MERCADOPAGO] Status Code: {}", e.getStatusCode());
            log.error("🔴 [MERCADOPAGO] Message: {}", e.getMessage());
            
            // Intentar obtener más detalles del error
            if (e.getApiResponse() != null && e.getApiResponse().getContent() != null) {
                log.error("🔴 [MERCADOPAGO] API Response Content: {}", e.getApiResponse().getContent());
            }
            
            // Imprimir los headers de la respuesta si están disponibles
            if (e.getApiResponse() != null && e.getApiResponse().getHeaders() != null) {
                log.error("🔴 [MERCADOPAGO] API Response Headers: {}", e.getApiResponse().getHeaders());
            }
            
            // Imprimir el stack trace completo
            log.error("🔴 [MERCADOPAGO] Stack Trace:");
            e.printStackTrace();
            
            if (e.getCause() != null) {
                log.error("🔴 [MERCADOPAGO] Cause: {}", e.getCause().getMessage());
                e.getCause().printStackTrace();
            }
            
            String errorMessage = String.format("Error al crear la preferencia de pago en MercadoPago (Status: %d): %s", 
                    e.getStatusCode(), e.getMessage());
            
            // Si hay contenido en la respuesta, agregarlo al mensaje
            if (e.getApiResponse() != null && e.getApiResponse().getContent() != null) {
                errorMessage += "\nDetalles: " + e.getApiResponse().getContent();
            }
            
            throw new RuntimeException(errorMessage, e);
        } catch (MPException e) {
            // Error general de MercadoPago
            log.error("🔴 [MERCADOPAGO] Error general de MercadoPago: {}", e.getMessage(), e);
            e.printStackTrace();
            throw new RuntimeException("Error al crear la preferencia de pago en MercadoPago: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("🔴 [MERCADOPAGO] Error inesperado al crear preferencia de pago: {}", e.getMessage(), e);
            e.printStackTrace();
            throw new RuntimeException("Error al crear la preferencia de pago en MercadoPago: " + e.getMessage(), e);
        }
    }

    /**
     * Procesa el retorno del pago (cuando el usuario vuelve de MercadoPago)
     * @param preferenceId ID de la preferencia (que corresponde al pedidoId)
     * @param paymentStatus Estado del pago: "approved", "rejected", "pending"
     * @return true si el pago fue exitoso y el pedido se confirmó
     */
    public boolean procesarRetornoPago(String preferenceId, String paymentStatus) {
        try {
            log.info("🔵 [MERCADOPAGO] Procesando retorno de pago. Preference ID: {}, Status: {}", 
                    preferenceId, paymentStatus);
            
            // El preferenceId en nuestro caso es el pedidoId (external_reference)
            Long pedidoId;
            try {
                pedidoId = Long.parseLong(preferenceId);
            } catch (NumberFormatException e) {
                log.error("🔴 [MERCADOPAGO] Preference ID no es un número válido: {}", preferenceId);
                return false;
            }
            
            // Obtener el pedido
            Pedido pedido = pedidoRepository.findById(pedidoId)
                    .orElse(null);
            
            if (pedido == null) {
                log.error("🔴 [MERCADOPAGO] Pedido no encontrado: {}", pedidoId);
                return false;
            }
            
            // Actualizar estado según el resultado del pago
            if ("approved".equals(paymentStatus) || "success".equals(paymentStatus)) {
                // Pago exitoso: confirmar el pedido
                log.info("✅ [MERCADOPAGO] Pago aprobado para pedido: {}", pedidoId);
                try {
                    pedidoService.confirmar(pedidoId);
                    log.info("✅ [MERCADOPAGO] Pedido {} confirmado exitosamente", pedidoId);
                    return true;
                } catch (Exception e) {
                    log.error("🔴 [MERCADOPAGO] Error al confirmar pedido {}: {}", pedidoId, e.getMessage(), e);
                    return false;
                }
            } else if ("rejected".equals(paymentStatus) || "failure".equals(paymentStatus)) {
                // Pago rechazado: cancelar el pedido
                log.warn("🟡 [MERCADOPAGO] Pago rechazado para pedido: {}", pedidoId);
                try {
                    pedidoService.cancelar(pedidoId);
                    log.info("✅ [MERCADOPAGO] Pedido {} cancelado por pago rechazado", pedidoId);
                    return false;
                } catch (Exception e) {
                    log.error("🔴 [MERCADOPAGO] Error al cancelar pedido {}: {}", pedidoId, e.getMessage(), e);
                    return false;
                }
            } else if ("pending".equals(paymentStatus)) {
                // Pago pendiente: mantener el pedido en BORRADOR
                log.info("🟡 [MERCADOPAGO] Pago pendiente para pedido: {}. El pedido permanece en estado BORRADOR", pedidoId);
                return false;
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("🔴 [MERCADOPAGO] Error al procesar retorno de pago: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Procesa una notificación de webhook de MercadoPago
     */
    public void procesarWebhook(Map<String, Object> datos) {
        try {
            log.info("🔵 [MERCADOPAGO] Recibiendo webhook: {}", datos);
            
            // MercadoPago envía diferentes tipos de notificaciones
            // Tipo "payment" contiene información del pago
            String tipo = (String) datos.get("type");
            Object data = datos.get("data");
            
            if ("payment".equals(tipo) && data != null) {
                String paymentId = data.toString();
                log.info("🔵 [MERCADOPAGO] Procesando pago: {}", paymentId);
                
                // Aquí deberías consultar la API de MercadoPago para obtener los detalles del pago
                // y actualizar el estado del pedido correspondiente
                // Por ahora solo registramos el webhook
            }
            
        } catch (Exception e) {
            log.error("🔴 [MERCADOPAGO] Error al procesar webhook: {}", e.getMessage(), e);
        }
    }
}

