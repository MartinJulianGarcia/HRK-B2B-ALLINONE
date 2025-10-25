package com.hrk.tienda_b2b.controller;

import com.hrk.tienda_b2b.dto.CreateProductoRequest;
import com.hrk.tienda_b2b.dto.ProductoResponseDTO;
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
    public ResponseEntity<List<ProductoResponseDTO>> listarTodos() {
        List<Producto> productos = productoService.obtenerTodos();
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
                .variantes(variantesDTO)
                .build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarProducto(@PathVariable Long id) {
        productoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
