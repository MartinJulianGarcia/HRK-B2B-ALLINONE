package com.hrk.tienda_b2b.service;

import com.hrk.tienda_b2b.dto.CreateProductoRequest;
import com.hrk.tienda_b2b.model.Categoria;
import com.hrk.tienda_b2b.model.Producto;
import com.hrk.tienda_b2b.model.ProductoVariante;
import com.hrk.tienda_b2b.model.TipoProducto;
import com.hrk.tienda_b2b.repository.ProductoRepository;
import com.hrk.tienda_b2b.repository.ProductoVarianteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final ProductoVarianteRepository productoVarianteRepository;

    public List<Producto> obtenerTodos() {
        return productoRepository.findAll();
    }

    public List<Producto> obtenerPorCategoria(Categoria categoria) {
        return productoRepository.findByCategoria(categoria);
    }

    public List<Producto> obtenerPorTipo(TipoProducto tipo) {
        return productoRepository.findByTipo(tipo);
    }

    public Optional<Producto> obtenerPorId(Long id) {
        return productoRepository.findById(id);
    }

    public Producto guardar(Producto producto) {
        // Validar que tenga imagen o asignar default
        if (producto.getImagenUrl() == null || producto.getImagenUrl().isEmpty()) {
            producto.setImagenUrl(producto.getTipo().getImagenDefault());
        }
        return productoRepository.save(producto);
    }

    public void eliminar(Long id) {
        productoRepository.deleteById(id);
    }

    @Transactional
    public Producto crearProducto(CreateProductoRequest request) {
        System.out.println("🔵 [SERVICE] Creando producto con request: " + request);
        System.out.println("🔵 [SERVICE] Stock por variante recibido: " + request.getStockPorVariante());
        System.out.println("🔵 [SERVICE] ¿Es null stockPorVariante?: " + (request.getStockPorVariante() == null));
        System.out.println("🔵 [SERVICE] ¿Está vacío stockPorVariante?: " + (request.getStockPorVariante() != null && request.getStockPorVariante().isEmpty()));
        
        // Validar que colores y talles no estén vacíos
        if (request.getColores() == null || request.getColores().isEmpty()) {
            throw new IllegalArgumentException("Debe proporcionar al menos un color");
        }
        if (request.getTalles() == null || request.getTalles().isEmpty()) {
            throw new IllegalArgumentException("Debe proporcionar al menos un talle");
        }
        
        // 1. Crear el producto base (SIN precio ni stock, esos van en las variantes)
        Producto producto = Producto.builder()
            .nombre(request.getNombre())
            .tipo(request.getTipo())
            .categoria(request.getCategoria())
            .descripcion(request.getDescripcion())
            .build();
        
        System.out.println("🔵 [SERVICE] Producto base creado: " + producto.getNombre());
        
        // 2. Manejar imagen: usar la proporcionada o la por defecto
        if (request.getImagenUrl() != null && !request.getImagenUrl().isEmpty()) {
            producto.setImagenUrl(request.getImagenUrl());
        } else {
            producto.setImagenUrl(request.getTipo().getImagenDefault());
        }
        
        System.out.println("🔵 [SERVICE] Imagen URL: " + producto.getImagenUrl());
        
        // 3. Guardar el producto primero
        producto = productoRepository.save(producto);
        
        System.out.println("🔵 [SERVICE] Producto guardado con ID: " + producto.getId());
        
        // 4. Crear las variantes para cada combinación de color y talle
        boolean usarStockIndividual = request.getStockPorVariante() != null && !request.getStockPorVariante().isEmpty();
        System.out.println("🔵 [SERVICE] Creando variantes - Usar stock individual: " + usarStockIndividual);
        
        if (usarStockIndividual) {
            System.out.println("🔵 [SERVICE] 📋 MAPA DE STOCK INDIVIDUAL RECIBIDO:");
            request.getStockPorVariante().forEach((clave, stock) -> {
                System.out.println("🔵 [SERVICE]   " + clave + " -> " + stock);
            });
        }
        
        for (String color : request.getColores()) {
            for (String talleOriginal : request.getTalles()) {
                // Si el talle contiene "/", dividirlo en talles individuales
                String[] tallesIndividuales;
                if (talleOriginal.contains("/")) {
                    tallesIndividuales = talleOriginal.split("/");
                } else {
                    tallesIndividuales = new String[]{talleOriginal};
                }
                
                // Validar que no se mezclen talles numéricos con "U" (Único)
                boolean tieneU = false;
                boolean tieneNumericos = false;
                for (String talleIndividual : tallesIndividuales) {
                    String talleLimpio = talleIndividual.trim().toUpperCase();
                    if (talleLimpio.equals("U") || talleLimpio.equals("TU") || talleLimpio.equals("UNICO")) {
                        tieneU = true;
                    } else if (talleLimpio.matches("\\d+")) {
                        tieneNumericos = true;
                    }
                }
                
                // Si tiene tanto "U" como numéricos, es inválido
                if (tieneU && tieneNumericos) {
                    throw new IllegalArgumentException("No se puede mezclar talle Único (U) con talles numéricos en el mismo producto");
                }
                
                // Crear una variante para cada talle individual
                for (String talleIndividual : tallesIndividuales) {
                    String talleLimpio = talleIndividual.trim();
                    
                    // Normalizar "U", "TU", "UNICO" a "U"
                    if (talleLimpio.toUpperCase().matches("U|TU|UNICO")) {
                        talleLimpio = "U";
                    }
                    
                    String skuVariante = generarSku(request.getSku(), color, talleLimpio);
                    
                    // Obtener stock individual para esta variante
                    String claveVariante = color + "-" + talleLimpio;
                    Integer stockIndividual = 0;
                    
                    System.out.println("🔵 [SERVICE] Buscando stock para clave: " + claveVariante);
                    System.out.println("🔵 [SERVICE] Stock por variante disponible: " + request.getStockPorVariante());
                    
                    if (usarStockIndividual && request.getStockPorVariante().containsKey(claveVariante)) {
                        stockIndividual = request.getStockPorVariante().get(claveVariante);
                        System.out.println("🔵 [SERVICE] ✅ Stock individual encontrado para " + claveVariante + ": " + stockIndividual);
                    } else if (usarStockIndividual) {
                        // Si se debe usar stock individual pero no se encuentra la clave, usar 0
                        stockIndividual = 0;
                        System.out.println("🔵 [SERVICE] ⚠️ Clave no encontrada en stock individual para " + claveVariante + ", usando 0");
                    } else {
                        // Si no hay stock individual, usar distribución igualitaria como fallback
                        System.out.println("🔵 [SERVICE] ⚠️ No se encontró stock individual para " + claveVariante + ", usando distribución igualitaria");
                        int totalTalles = 0;
                        for (String talleItem : request.getTalles()) {
                            if (talleItem.contains("/")) {
                                totalTalles += talleItem.split("/").length;
                            } else {
                                totalTalles += 1;
                            }
                        }
                        int totalVariantes = request.getColores().size() * totalTalles;
                        stockIndividual = request.getStock() / totalVariantes;
                        System.out.println("🔵 [SERVICE] Usando distribución igualitaria para " + claveVariante + ": " + stockIndividual);
                    }
                    
                    System.out.println("🔵 [SERVICE] 🎯 ASIGNANDO STOCK FINAL para " + claveVariante + ": " + stockIndividual);
                    
                    ProductoVariante variante = ProductoVariante.builder()
                        .producto(producto)
                        .sku(skuVariante)
                        .color(color)
                        .talle(talleLimpio)
                        .precio(request.getPrecio())
                        .stockDisponible(stockIndividual)
                        .build();
                    
                    producto.getVariantes().add(variante);
                    System.out.println("🔵 [SERVICE] Variante creada: " + skuVariante);
                }
            }
        }
        
        // 5. Guardar el producto con las variantes
        producto = productoRepository.save(producto);
        
        System.out.println("✅ [SERVICE] Producto guardado con " + producto.getVariantes().size() + " variantes");
        
        return producto;
    }
    
    private String generarSku(String skuBase, String color, String talle) {
        // Generar SKU único para cada variante
        String colorCode = color.length() >= 2 ? color.substring(0, 2).toUpperCase() : color.toUpperCase();
        return String.format("%s-%s-%s", skuBase, colorCode, talle.toUpperCase().replaceAll("/", ""));
    }

    @Transactional
    public Producto actualizarProducto(Long id, CreateProductoRequest request) {
        System.out.println("🔵 [SERVICE] Actualizando producto con ID: " + id);
        System.out.println("🔵 [SERVICE] Request recibido: " + request);
        System.out.println("🔵 [SERVICE] SKU recibido: " + request.getSku());
        System.out.println("🔵 [SERVICE] Colores: " + request.getColores());
        System.out.println("🔵 [SERVICE] Talles: " + request.getTalles());
        System.out.println("🔵 [SERVICE] Precio: " + request.getPrecio());
        System.out.println("🔵 [SERVICE] Stock por variante: " + request.getStockPorVariante());
        
        // Buscar el producto existente
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + id));
        
        System.out.println("🔵 [SERVICE] Producto encontrado: " + producto.getNombre());
        System.out.println("🔵 [SERVICE] Variantes existentes: " + producto.getVariantes().size());
        
        // Validaciones básicas
        if (request.getNombre() != null && !request.getNombre().trim().isEmpty()) {
            producto.setNombre(request.getNombre());
        }
        
        if (request.getTipo() != null) {
            producto.setTipo(request.getTipo());
        }
        
        if (request.getCategoria() != null) {
            producto.setCategoria(request.getCategoria());
        }
        
        if (request.getDescripcion() != null) {
            producto.setDescripcion(request.getDescripcion());
        }
        
        // Manejar imagen
        if (request.getImagenUrl() != null && !request.getImagenUrl().isEmpty()) {
            producto.setImagenUrl(request.getImagenUrl());
        }
        
        // Actualizar variantes si se proporcionan
        if (request.getColores() != null && !request.getColores().isEmpty() &&
            request.getTalles() != null && !request.getTalles().isEmpty() &&
            request.getPrecio() != null && request.getPrecio() > 0) {
            
            // Obtener SKU base antes de eliminar variantes (necesario si no viene en el request)
            String skuBase = request.getSku();
            if (skuBase == null || skuBase.trim().isEmpty()) {
                // Intentar obtener el SKU base de las variantes existentes
                if (!producto.getVariantes().isEmpty()) {
                    String primerSku = producto.getVariantes().get(0).getSku();
                    String[] partes = primerSku.split("-");
                    if (partes.length > 0) {
                        skuBase = partes[0];
                    } else {
                        skuBase = "SKU-" + producto.getId();
                    }
                } else {
                    skuBase = "SKU-" + producto.getId();
                }
            }
            
            // Eliminar variantes existentes usando query directa por producto_id
            // Esto es más eficiente y evita problemas de caché/transacción
            System.out.println("🔵 [SERVICE] Eliminando variantes existentes del producto ID: " + producto.getId());
            productoVarianteRepository.deleteByProductoId(producto.getId());
            productoVarianteRepository.flush(); // Forzar commit de la eliminación
            producto.getVariantes().clear(); // Limpiar la lista en memoria
            System.out.println("🔵 [SERVICE] Variantes eliminadas, ahora crear nuevas");
            
            // Crear nuevas variantes
            boolean usarStockIndividual = request.getStockPorVariante() != null && !request.getStockPorVariante().isEmpty();
            
            for (String color : request.getColores()) {
                for (String talleOriginal : request.getTalles()) {
                    String[] tallesIndividuales;
                    if (talleOriginal.contains("/")) {
                        tallesIndividuales = talleOriginal.split("/");
                    } else {
                        tallesIndividuales = new String[]{talleOriginal};
                    }
                    
                    for (String talleIndividual : tallesIndividuales) {
                        String talleLimpio = talleIndividual.trim();
                        if (talleLimpio.toUpperCase().matches("U|TU|UNICO")) {
                            talleLimpio = "U";
                        }
                        
                        String skuVariante = generarSku(skuBase, color, talleLimpio);
                        
                        String claveVariante = color + "-" + talleLimpio;
                        Integer stockIndividual = 0;
                        
                        if (usarStockIndividual && request.getStockPorVariante().containsKey(claveVariante)) {
                            stockIndividual = request.getStockPorVariante().get(claveVariante);
                        } else if (request.getStock() != null && request.getStock() > 0) {
                            // Distribución igualitaria como fallback
                            int totalTalles = 0;
                            for (String talleItem : request.getTalles()) {
                                if (talleItem.contains("/")) {
                                    totalTalles += talleItem.split("/").length;
                                } else {
                                    totalTalles += 1;
                                }
                            }
                            int totalVariantes = request.getColores().size() * totalTalles;
                            stockIndividual = request.getStock() / totalVariantes;
                        }
                        
                        // Nota: No verificamos duplicados aquí porque:
                        // 1. Si el SKU pertenece al mismo producto, ya fue eliminado con flush()
                        // 2. Si el SKU pertenece a otro producto, la base de datos lanzará una excepción
                        // que será capturada por el @Transactional
                        
                        ProductoVariante variante = ProductoVariante.builder()
                            .producto(producto)
                            .sku(skuVariante)
                            .color(color)
                            .talle(talleLimpio)
                            .precio(request.getPrecio())
                            .stockDisponible(stockIndividual)
                            .build();
                        
                        producto.getVariantes().add(variante);
                        System.out.println("🔵 [SERVICE] Variante creada: " + skuVariante + " (Color: " + color + ", Talle: " + talleLimpio + ", Stock: " + stockIndividual + ")");
                    }
                }
            }
        } else if (request.getStockPorVariante() != null && !request.getStockPorVariante().isEmpty()) {
            // Actualizar solo el stock de variantes existentes
            for (ProductoVariante variante : producto.getVariantes()) {
                String claveVariante = variante.getColor() + "-" + variante.getTalle();
                if (request.getStockPorVariante().containsKey(claveVariante)) {
                    variante.setStockDisponible(request.getStockPorVariante().get(claveVariante));
                }
            }
        }
        
        // Guardar el producto actualizado
        producto = productoRepository.save(producto);
        
        System.out.println("✅ [SERVICE] Producto actualizado exitosamente con ID: " + producto.getId());
        
        return producto;
    }
}
