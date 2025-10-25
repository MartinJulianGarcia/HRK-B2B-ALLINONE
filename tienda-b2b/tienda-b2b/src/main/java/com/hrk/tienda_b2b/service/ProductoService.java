package com.hrk.tienda_b2b.service;

import com.hrk.tienda_b2b.dto.CreateProductoRequest;
import com.hrk.tienda_b2b.model.Categoria;
import com.hrk.tienda_b2b.model.Producto;
import com.hrk.tienda_b2b.model.ProductoVariante;
import com.hrk.tienda_b2b.model.TipoProducto;
import com.hrk.tienda_b2b.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;

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
}
