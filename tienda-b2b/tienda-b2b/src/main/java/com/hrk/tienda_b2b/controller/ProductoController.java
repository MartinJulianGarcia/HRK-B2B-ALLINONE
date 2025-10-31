package com.hrk.tienda_b2b.controller;

import com.hrk.tienda_b2b.dto.CreateProductoRequest;
import com.hrk.tienda_b2b.dto.ProductoResponseDTO;
import com.hrk.tienda_b2b.dto.VerificacionActualizacionProductoResponse;
import com.hrk.tienda_b2b.model.Producto;
import com.hrk.tienda_b2b.model.ProductoVariante;
import com.hrk.tienda_b2b.service.ProductoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;

    @GetMapping
    public ResponseEntity<List<ProductoResponseDTO>> listarTodos(
            @RequestParam(value = "incluirOcultos", defaultValue = "false") boolean incluirOcultos) {
        List<Producto> productos = productoService.obtenerTodos(incluirOcultos);
        List<ProductoResponseDTO> productosDTO = productos.stream()
                .map(producto -> convertirADTO(producto))
                .collect(Collectors.toList());
        return ResponseEntity.ok(productosDTO);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return productoService.obtenerPorId(id)
                .map(producto -> convertirADTO(producto))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crearProducto(@RequestBody CreateProductoRequest request) {
        try {
            System.out.println("=================================================");
            System.out.println("🔵 [CONTROLLER] Request recibido");
            System.out.println("🔵 [CONTROLLER] Nombre: " + request.getNombre());
            System.out.println("🔵 [CONTROLLER] Tipo: " + request.getTipo());
            System.out.println("🔵 [CONTROLLER] Categoría: " + request.getCategoria());
            System.out.println("🔵 [CONTROLLER] SKU: " + request.getSku());
            System.out.println("🔵 [CONTROLLER] Colores: " + request.getColores());
            System.out.println("🔵 [CONTROLLER] Talles: " + request.getTalles());
            System.out.println("🔵 [CONTROLLER] Precio: " + request.getPrecio());
            System.out.println("🔵 [CONTROLLER] Stock: " + request.getStock());
            System.out.println("🔵 [CONTROLLER] Descripción: " + request.getDescripcion());
            System.out.println("🔵 [CONTROLLER] Imagen URL: " + request.getImagenUrl());
            System.out.println("🔵 [CONTROLLER] 🔍 STOCK POR VARIANTE RECIBIDO:");
            System.out.println("🔵 [CONTROLLER] - Es null?: " + (request.getStockPorVariante() == null));
            System.out.println("🔵 [CONTROLLER] - Contenido: " + request.getStockPorVariante());
            if (request.getStockPorVariante() != null) {
                System.out.println("🔵 [CONTROLLER] - Tamaño del mapa: " + request.getStockPorVariante().size());
                request.getStockPorVariante().forEach((clave, stock) -> {
                    System.out.println("🔵 [CONTROLLER]   " + clave + " -> " + stock);
                });
            }
            System.out.println("=================================================");
            
            // Validar datos básicos
            if (request.getNombre() == null || request.getNombre().trim().isEmpty()) {
                System.out.println("🔴 [CONTROLLER] Error: Nombre vacío");
                return ResponseEntity.badRequest().body(crearRespuestaError("El nombre es obligatorio"));
            }
            
            if (request.getTipo() == null) {
                System.out.println("🔴 [CONTROLLER] Error: Tipo vacío");
                return ResponseEntity.badRequest().body(crearRespuestaError("El tipo es obligatorio"));
            }
            
            if (request.getCategoria() == null) {
                System.out.println("🔴 [CONTROLLER] Error: Categoría vacía");
                return ResponseEntity.badRequest().body(crearRespuestaError("La categoría es obligatoria"));
            }
            
            if (request.getSku() == null || request.getSku().trim().isEmpty()) {
                System.out.println("🔴 [CONTROLLER] Error: SKU vacío");
                return ResponseEntity.badRequest().body(crearRespuestaError("El SKU es obligatorio"));
            }
            
            if (request.getColores() == null || request.getColores().isEmpty()) {
                System.out.println("🔴 [CONTROLLER] Error: Sin colores");
                return ResponseEntity.badRequest().body(crearRespuestaError("Debe seleccionar al menos un color"));
            }
            
            if (request.getTalles() == null || request.getTalles().isEmpty()) {
                System.out.println("🔴 [CONTROLLER] Error: Sin talles");
                return ResponseEntity.badRequest().body(crearRespuestaError("Debe seleccionar al menos un talle"));
            }
            
            if (request.getPrecio() == null || request.getPrecio() <= 0) {
                System.out.println("🔴 [CONTROLLER] Error: Precio inválido");
                return ResponseEntity.badRequest().body(crearRespuestaError("El precio debe ser mayor a 0"));
            }
            
            if (request.getStock() == null || request.getStock() < 0) {
                System.out.println("🔴 [CONTROLLER] Error: Stock inválido");
                return ResponseEntity.badRequest().body(crearRespuestaError("El stock debe ser mayor o igual a 0"));
            }
            
            // Llamar al servicio para crear el producto
            Producto nuevo = productoService.crearProducto(request);
            System.out.println("✅ [CONTROLLER] Producto creado exitosamente con ID: " + nuevo.getId());
            System.out.println("=================================================");
            
            // Convertir a DTO para evitar referencia circular
            ProductoResponseDTO responseDTO = convertirADTO(nuevo);
            
            return ResponseEntity.ok(responseDTO);
            
        } catch (IllegalArgumentException e) {
            System.out.println("🔴 [CONTROLLER] Error de validación: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(crearRespuestaError(e.getMessage()));
            
        } catch (Exception e) {
            System.out.println("🔴 [CONTROLLER] Error inesperado: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(crearRespuestaError("Error interno del servidor: " + e.getMessage()));
        }
    }
    
    private Map<String, String> crearRespuestaError(String mensaje) {
        Map<String, String> error = new HashMap<>();
        error.put("error", mensaje);
        return error;
    }
    
    private ProductoResponseDTO convertirADTO(Producto producto) {
        try {
            // Asegurarse de que las variantes estén cargadas
            if (producto.getVariantes() != null) {
                producto.getVariantes().size(); // Esto fuerza la carga en JPA si está en lazy loading
            }
            
            List<ProductoResponseDTO.ProductoVarianteResponseDTO> variantesDTO = producto.getVariantes().stream()
                    .map(variante -> ProductoResponseDTO.ProductoVarianteResponseDTO.builder()
                            .id(variante.getId())
                            .sku(variante.getSku())
                            .color(variante.getColor())
                            .talle(variante.getTalle())
                            .precio(variante.getPrecio())
                            .stockDisponible(variante.getStockDisponible())
                            .build())
                    .collect(Collectors.toList());
            
            return ProductoResponseDTO.builder()
                    .id(producto.getId())
                    .nombre(producto.getNombre())
                    .descripcion(producto.getDescripcion())
                    .tipo(producto.getTipo())
                    .imagenUrl(producto.getImagenUrl())
                    .categoria(producto.getCategoria())
                    .oculto(producto.getOculto() != null ? producto.getOculto() : false)
                    .variantes(variantesDTO)
                    .build();
        } catch (Exception e) {
            System.err.println("🔴 [CONTROLLER] Error al convertir Producto a DTO: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al convertir producto a DTO: " + e.getMessage(), e);
        }
    }

    @GetMapping("/{id}/verificar-pedidos")
    public ResponseEntity<VerificacionActualizacionProductoResponse> verificarVariantesConPedidos(@PathVariable Long id) {
        try {
            VerificacionActualizacionProductoResponse verificacion = productoService.verificarVariantesConPedidosDetallado(id);
            return ResponseEntity.ok(verificacion);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarProducto(
            @PathVariable Long id, 
            @RequestBody CreateProductoRequest request,
            @RequestParam(value = "confirmarVariantesConPedidos", defaultValue = "false") boolean confirmarVariantesConPedidos) {
        try {
            System.out.println("=================================================");
            System.out.println("🔵 [CONTROLLER] Request de actualización recibido para ID: " + id);
            System.out.println("🔵 [CONTROLLER] Nombre: " + request.getNombre());
            System.out.println("🔵 [CONTROLLER] Tipo: " + request.getTipo());
            System.out.println("🔵 [CONTROLLER] Categoría: " + request.getCategoria());
            System.out.println("🔵 [CONTROLLER] Imagen URL: " + request.getImagenUrl());
            System.out.println("🔵 [CONTROLLER] Oculto: " + request.getOculto() + " (null: " + (request.getOculto() == null) + ")");
            System.out.println("🔵 [CONTROLLER] Confirmar variantes con pedidos: " + confirmarVariantesConPedidos);
            System.out.println("=================================================");
            
            // Llamar al servicio para actualizar el producto
            Producto actualizado = productoService.actualizarProducto(id, request, confirmarVariantesConPedidos);
            System.out.println("✅ [CONTROLLER] Producto actualizado exitosamente con ID: " + actualizado.getId());
            System.out.println("✅ [CONTROLLER] Producto oculto después de actualizar: " + actualizado.getOculto());
            System.out.println("=================================================");
            
            // Convertir a DTO para evitar referencia circular
            ProductoResponseDTO responseDTO = convertirADTO(actualizado);
            System.out.println("✅ [CONTROLLER] DTO oculto: " + responseDTO.getOculto());
            
            return ResponseEntity.ok(responseDTO);
            
        } catch (IllegalStateException e) {
            // Si hay variantes con pedidos y no se confirmó, devolver información detallada
            System.out.println("🟡 [CONTROLLER] Variantes con pedidos detectadas, solicitando confirmación");
            VerificacionActualizacionProductoResponse verificacion = productoService.verificarVariantesConPedidosDetallado(id);
            
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("requiereConfirmacion", true);
            respuesta.put("mensaje", "El producto tiene variantes con pedidos asociados. Se requiere confirmación para continuar.");
            respuesta.put("verificacion", verificacion);
            
            return ResponseEntity.status(409).body(respuesta); // 409 Conflict
            
        } catch (IllegalArgumentException e) {
            System.out.println("🔴 [CONTROLLER] Error de validación: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(crearRespuestaError(e.getMessage()));
            
        } catch (Exception e) {
            System.out.println("🔴 [CONTROLLER] Error inesperado: " + e.getMessage());
            System.out.println("🔴 [CONTROLLER] Tipo de excepción: " + e.getClass().getName());
            if (e.getCause() != null) {
                System.out.println("🔴 [CONTROLLER] Causa: " + e.getCause().getMessage());
                e.getCause().printStackTrace();
            }
            e.printStackTrace();
            // Incluir más detalles del error en la respuesta
            String mensajeError = "Error interno del servidor: " + e.getMessage();
            if (e.getCause() != null) {
                mensajeError += " (Causa: " + e.getCause().getMessage() + ")";
            }
            return ResponseEntity.status(500).body(crearRespuestaError(mensajeError));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarProducto(@PathVariable Long id) {
        productoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
