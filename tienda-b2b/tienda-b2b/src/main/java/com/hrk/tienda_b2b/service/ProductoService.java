package com.hrk.tienda_b2b.service;

import com.hrk.tienda_b2b.dto.ActualizacionProductoResponse;
import com.hrk.tienda_b2b.dto.CreateProductoRequest;
import com.hrk.tienda_b2b.dto.VerificacionActualizacionProductoResponse;
import com.hrk.tienda_b2b.model.Categoria;
import com.hrk.tienda_b2b.model.MovimientoStock;
import com.hrk.tienda_b2b.model.Producto;
import com.hrk.tienda_b2b.model.ProductoVariante;
import com.hrk.tienda_b2b.model.TipoMovimiento;
import com.hrk.tienda_b2b.model.TipoProducto;
import com.hrk.tienda_b2b.model.StockHistorico;
import com.hrk.tienda_b2b.repository.DetallePedidoRepository;
import com.hrk.tienda_b2b.repository.MovimientoStockRepository;
import com.hrk.tienda_b2b.repository.ProductoRepository;
import com.hrk.tienda_b2b.repository.ProductoVarianteRepository;
import com.hrk.tienda_b2b.repository.StockHistoricoRepository;
import com.hrk.tienda_b2b.repository.TemporadaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final ProductoVarianteRepository productoVarianteRepository;
    private final MovimientoStockRepository movimientoStockRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final StockHistoricoRepository stockHistoricoRepository;
    private final TemporadaRepository temporadaRepository;

    public List<Producto> obtenerTodos() {
        return obtenerTodos(false);
    }
    
    public List<Producto> obtenerTodos(boolean incluirOcultos) {
        List<Producto> productos = incluirOcultos
                ? productoRepository.findAll()
                : productoRepository.findByOcultoFalse();

        return temporadaRepository.findActiveWithProductos()
                .map(temporadaActiva -> {
                    Set<Long> productIds = temporadaActiva.getProductos().stream()
                            .map(Producto::getId)
                            .collect(Collectors.toSet());

                    if (productIds.isEmpty()) {
                        return new ArrayList<Producto>();
                    }

                    return productos.stream()
                            .filter(producto -> productIds.contains(producto.getId()))
                            .collect(Collectors.toList());
                })
                .orElse(productos);
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

    @Transactional
    public void eliminar(Long id) {
        System.out.println("🔵 [SERVICE] Intentando eliminar producto con ID: " + id);
        
        // 1. Verificar que el producto existe
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + id));
        
        System.out.println("🔵 [SERVICE] Producto encontrado: " + producto.getNombre());
        
        // 2. Forzar la carga de las variantes para evitar problemas de lazy loading
        if (producto.getVariantes() != null) {
            producto.getVariantes().size(); // Esto fuerza la carga en JPA
        }
        
        System.out.println("🔵 [SERVICE] Variantes del producto: " + (producto.getVariantes() != null ? producto.getVariantes().size() : 0));
        
        // 3. Verificar si alguna variante tiene pedidos asociados
        List<Long> variantesConPedidos = verificarVariantesConPedidos(id);
        
        if (!variantesConPedidos.isEmpty()) {
            System.out.println("🔴 [SERVICE] No se puede eliminar el producto: tiene " + variantesConPedidos.size() + " variante(s) con pedidos asociados");
            throw new IllegalStateException("No se puede eliminar un artículo con pedidos. El producto tiene " + variantesConPedidos.size() + " variante(s) con pedidos asociados.");
        }
        
        // 4. Si no tiene pedidos, eliminar el producto (las variantes se eliminarán por cascade)
        System.out.println("✅ [SERVICE] El producto no tiene pedidos asociados. Eliminando producto y sus variantes...");
        productoRepository.deleteById(id);
        System.out.println("✅ [SERVICE] Producto eliminado exitosamente");
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
            .oculto(request.getOculto() != null ? request.getOculto() : false)
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
        
        // 6. Registrar stock histórico inicial para todas las variantes
        for (ProductoVariante variante : producto.getVariantes()) {
            if (variante.getStockDisponible() > 0) {
                registrarStockHistoricoInicial(variante, variante.getStockDisponible());
            }
        }
        
        System.out.println("✅ [SERVICE] Producto guardado con " + producto.getVariantes().size() + " variantes");
        
        return producto;
    }
    
    private String generarSku(String skuBase, String color, String talle) {
        // Generar SKU único para cada variante
        String colorCode = color.length() >= 2 ? color.substring(0, 2).toUpperCase() : color.toUpperCase();
        return String.format("%s-%s-%s", skuBase, colorCode, talle.toUpperCase().replaceAll("/", ""));
    }

    /**
     * Verifica qué variantes de un producto tienen pedidos asociados
     * @param productoId ID del producto
     * @return Lista de IDs de variantes que tienen pedidos
     */
    private List<Long> verificarVariantesConPedidos(Long productoId) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + productoId));
        
        List<Long> variantesConPedidos = new ArrayList<>();
        
        for (ProductoVariante variante : producto.getVariantes()) {
            if (detallePedidoRepository.existsByVarianteId(variante.getId())) {
                variantesConPedidos.add(variante.getId());
                System.out.println("🔵 [SERVICE] Variante " + variante.getSku() + " (ID: " + variante.getId() + ") tiene pedidos asociados");
            }
        }
        
        return variantesConPedidos;
    }

    /**
     * Detecta si solo se están agregando variantes (no eliminando ninguna existente)
     * @param producto Producto existente
     * @param coloresNuevos Lista de colores del request
     * @param tallesNuevos Lista de talles del request
     * @return true si solo se agregan variantes, false si se eliminan algunas
     */
    private boolean soloSeAgreganVariantes(Producto producto, List<String> coloresNuevos, List<String> tallesNuevos) {
        if (producto.getVariantes() == null || producto.getVariantes().isEmpty()) {
            // Si no hay variantes existentes, solo se están agregando
            return true;
        }
        
        // Obtener todas las combinaciones color-talle existentes
        Set<String> combinacionesExistentes = new HashSet<>();
        for (ProductoVariante variante : producto.getVariantes()) {
            String clave = variante.getColor() + "-" + variante.getTalle();
            combinacionesExistentes.add(clave);
        }
        
        System.out.println("🔵 [SERVICE] Combinaciones existentes: " + combinacionesExistentes);
        
        // Obtener todas las combinaciones color-talle del request
        Set<String> combinacionesNuevas = new HashSet<>();
        for (String color : coloresNuevos) {
            for (String talleOriginal : tallesNuevos) {
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
                    String clave = color + "-" + talleLimpio;
                    combinacionesNuevas.add(clave);
                }
            }
        }
        
        System.out.println("🔵 [SERVICE] Combinaciones nuevas: " + combinacionesNuevas);
        
        // Si todas las combinaciones existentes están en las nuevas, solo se están agregando
        boolean soloAgregar = combinacionesNuevas.containsAll(combinacionesExistentes);
        System.out.println("🔵 [SERVICE] ¿Solo se agregan variantes? " + soloAgregar);
        
        return soloAgregar;
    }
    
    /**
     * Obtiene información sobre las variantes nuevas que se agregarán
     * @param producto Producto existente
     * @param coloresNuevos Lista de colores del request
     * @param tallesNuevos Lista de talles del request
     * @param skuBase SKU base del producto
     * @param stockPorVariante Mapa de stock por variante del request
     * @return Lista de información sobre variantes nuevas
     */
    private List<ActualizacionProductoResponse.VarianteNuevaInfo> obtenerVariantesNuevas(
            Producto producto, List<String> coloresNuevos, List<String> tallesNuevos, 
            String skuBase, Map<String, Integer> stockPorVariante) {
        
        List<ActualizacionProductoResponse.VarianteNuevaInfo> variantesNuevas = new ArrayList<>();
        
        // Obtener combinaciones existentes
        Set<String> combinacionesExistentes = new HashSet<>();
        if (producto.getVariantes() != null) {
            for (ProductoVariante variante : producto.getVariantes()) {
                String clave = variante.getColor() + "-" + variante.getTalle();
                combinacionesExistentes.add(clave);
            }
        }
        
        // Obtener combinaciones nuevas
        boolean usarStockIndividual = stockPorVariante != null && !stockPorVariante.isEmpty();
        
        for (String color : coloresNuevos) {
            for (String talleOriginal : tallesNuevos) {
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
                    
                    String claveVariante = color + "-" + talleLimpio;
                    
                    // Solo agregar si no existe
                    if (!combinacionesExistentes.contains(claveVariante)) {
                        String skuVariante = generarSku(skuBase, color, talleLimpio);
                        Integer stockIndividual = 0;
                        
                        if (usarStockIndividual && stockPorVariante.containsKey(claveVariante)) {
                            stockIndividual = stockPorVariante.get(claveVariante);
                        }
                        // Si no hay stock, se crea con stock 0
                        
                        ActualizacionProductoResponse.VarianteNuevaInfo info = 
                            ActualizacionProductoResponse.VarianteNuevaInfo.builder()
                                .sku(skuVariante)
                                .color(color)
                                .talle(talleLimpio)
                                .stock(stockIndividual)
                                .build();
                        
                        variantesNuevas.add(info);
                    }
                }
            }
        }
        
        return variantesNuevas;
    }
    
    /**
     * Detecta si solo se están agregando variantes (método público para el controller)
     * @param productoId ID del producto
     * @param coloresNuevos Lista de colores del request
     * @param tallesNuevos Lista de talles del request
     * @return true si solo se agregan variantes, false si se eliminan algunas
     */
    public boolean soloSeAgreganVariantesPublico(Long productoId, List<String> coloresNuevos, List<String> tallesNuevos) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + productoId));
        
        // Forzar la carga de las variantes
        if (producto.getVariantes() != null) {
            producto.getVariantes().size();
        }
        
        return soloSeAgreganVariantes(producto, coloresNuevos, tallesNuevos);
    }
    
    /**
     * Obtiene información sobre las variantes nuevas que se agregarán (método público para el controller)
     * @param productoId ID del producto
     * @param request Request con colores, talles y stock
     * @return Información sobre variantes nuevas que se agregarán
     */
    public List<ActualizacionProductoResponse.VarianteNuevaInfo> obtenerVariantesNuevasPublico(
            Long productoId, CreateProductoRequest request) {
        
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + productoId));
        
        // Forzar la carga de las variantes
        if (producto.getVariantes() != null) {
            producto.getVariantes().size();
        }
        
        // Obtener SKU base
        String skuBase = request.getSku();
        if (skuBase == null || skuBase.trim().isEmpty()) {
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
        
        return obtenerVariantesNuevas(producto, request.getColores(), request.getTalles(), 
                skuBase, request.getStockPorVariante());
    }
    
    /**
     * Verifica y devuelve información detallada sobre variantes con pedidos
     * @param productoId ID del producto
     * @return Información sobre variantes con pedidos asociados
     */
    public VerificacionActualizacionProductoResponse verificarVariantesConPedidosDetallado(Long productoId) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + productoId));
        
        List<VerificacionActualizacionProductoResponse.VarianteConPedidosInfo> variantesInfo = new ArrayList<>();
        
        for (ProductoVariante variante : producto.getVariantes()) {
            long cantidadPedidos = detallePedidoRepository.countByVarianteId(variante.getId());
            if (cantidadPedidos > 0) {
                VerificacionActualizacionProductoResponse.VarianteConPedidosInfo info = 
                    VerificacionActualizacionProductoResponse.VarianteConPedidosInfo.builder()
                        .varianteId(variante.getId())
                        .sku(variante.getSku())
                        .color(variante.getColor())
                        .talle(variante.getTalle())
                        .stockActual(variante.getStockDisponible())
                        .cantidadPedidos(cantidadPedidos)
                        .build();
                variantesInfo.add(info);
                System.out.println("🔵 [SERVICE] Variante " + variante.getSku() + " tiene " + cantidadPedidos + " pedidos asociados");
            }
        }
        
        return VerificacionActualizacionProductoResponse.builder()
                .tieneVariantesConPedidos(!variantesInfo.isEmpty())
                .cantidadVariantesConPedidos(variantesInfo.size())
                .variantesConPedidos(variantesInfo)
                .build();
    }

    @Transactional
    public Producto actualizarProducto(Long id, CreateProductoRequest request, boolean confirmarVariantesConPedidos) {
        System.out.println("🔵 [SERVICE] Actualizando producto con ID: " + id);
        System.out.println("🔵 [SERVICE] Request recibido: " + request);
        System.out.println("🔵 [SERVICE] SKU recibido: " + request.getSku());
        System.out.println("🔵 [SERVICE] Colores: " + request.getColores() + " (null: " + (request.getColores() == null) + ", empty: " + (request.getColores() != null && request.getColores().isEmpty()) + ")");
        System.out.println("🔵 [SERVICE] Talles: " + request.getTalles() + " (null: " + (request.getTalles() == null) + ", empty: " + (request.getTalles() != null && request.getTalles().isEmpty()) + ")");
        System.out.println("🔵 [SERVICE] Precio: " + request.getPrecio());
        System.out.println("🔵 [SERVICE] Stock por variante: " + request.getStockPorVariante() + " (null: " + (request.getStockPorVariante() == null) + ", empty: " + (request.getStockPorVariante() != null && request.getStockPorVariante().isEmpty()) + ")");
        
        // Buscar el producto existente
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + id));
        
        System.out.println("🔵 [SERVICE] Producto encontrado: " + producto.getNombre());
        
        // Forzar la carga de las variantes para evitar problemas de lazy loading
        if (producto.getVariantes() != null) {
            producto.getVariantes().size(); // Esto fuerza la carga en JPA
        }
        
        System.out.println("🔵 [SERVICE] Variantes existentes: " + (producto.getVariantes() != null ? producto.getVariantes().size() : 0));
        
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
        
        // Manejar campo oculto - siempre actualizar, incluso si es false
        if (request.getOculto() != null) {
            producto.setOculto(request.getOculto());
            System.out.println("🔵 [SERVICE] Campo oculto actualizado a: " + request.getOculto());
        } else {
            System.out.println("🟡 [SERVICE] Campo oculto no viene en el request, manteniendo valor actual: " + producto.getOculto());
        }
        
        // Manejar imagen
        if (request.getImagenUrl() != null && !request.getImagenUrl().isEmpty()) {
            producto.setImagenUrl(request.getImagenUrl());
        }
        
        // Actualizar variantes si se proporcionan (y no estamos solo actualizando stock)
        boolean tieneColoresYTalles = request.getColores() != null && !request.getColores().isEmpty() &&
                                      request.getTalles() != null && !request.getTalles().isEmpty() &&
                                      request.getPrecio() != null && request.getPrecio() > 0;
        
        System.out.println("🔵 [SERVICE] ¿Debe actualizar variantes (colores/talles/precio)? " + tieneColoresYTalles);
        
        if (tieneColoresYTalles) {
            
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
            
            // ⭐ NUEVA LÓGICA: Detectar si solo se están agregando variantes
            boolean soloAgregar = soloSeAgreganVariantes(producto, request.getColores(), request.getTalles());
            
            if (soloAgregar) {
                // ⭐ MODO "SOLO AGREGAR": No eliminar variantes existentes, solo crear las nuevas
                System.out.println("✅ [SERVICE] MODO SOLO AGREGAR: No se eliminarán variantes existentes");
                
                // Obtener combinaciones existentes para evitar duplicados
                Set<String> combinacionesExistentes = new HashSet<>();
                for (ProductoVariante variante : producto.getVariantes()) {
                    String clave = variante.getColor() + "-" + variante.getTalle();
                    combinacionesExistentes.add(clave);
                }
                
                // Crear solo las variantes nuevas (que no existen)
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
                            
                            String claveVariante = color + "-" + talleLimpio;
                            
                            // Solo crear si no existe
                            if (!combinacionesExistentes.contains(claveVariante)) {
                                String skuVariante = generarSku(skuBase, color, talleLimpio);
                                Integer stockIndividual = 0;
                                
                                if (usarStockIndividual && request.getStockPorVariante().containsKey(claveVariante)) {
                                    stockIndividual = request.getStockPorVariante().get(claveVariante);
                                }
                                // Si no hay stock en stockPorVariante, se crea con stock 0
                                
                                ProductoVariante variante = ProductoVariante.builder()
                                    .producto(producto)
                                    .sku(skuVariante)
                                    .color(color)
                                    .talle(talleLimpio)
                                    .precio(request.getPrecio())
                                    .stockDisponible(stockIndividual)
                                    .build();
                                
                                producto.getVariantes().add(variante);
                                System.out.println("✅ [SERVICE] Nueva variante creada: " + skuVariante + " (Color: " + color + ", Talle: " + talleLimpio + ", Stock: " + stockIndividual + ")");
                            } else {
                                System.out.println("🔵 [SERVICE] Variante ya existe, no se crea: " + claveVariante);
                            }
                        }
                    }
                }
                
                // Guardar el producto con las nuevas variantes
                producto = productoRepository.save(producto);
                
                // Registrar stock histórico inicial para las nuevas variantes (solo si tienen stock > 0)
                for (ProductoVariante variante : producto.getVariantes()) {
                    if (variante.getStockDisponible() > 0) {
                        List<StockHistorico> historicos = stockHistoricoRepository.findByVarianteOrderByFechaAsc(variante);
                        if (historicos.isEmpty()) {
                            registrarStockHistoricoInicial(variante, variante.getStockDisponible());
                        }
                    }
                }
                
            } else {
                // ⭐ MODO "ELIMINAR Y RECREAR": Comportamiento original con confirmación
                System.out.println("⚠️ [SERVICE] MODO ELIMINAR Y RECREAR: Se eliminarán variantes existentes");
                
                // Verificar qué variantes tienen pedidos antes de modificar
                List<Long> variantesConPedidos = verificarVariantesConPedidos(id);
                
                if (!variantesConPedidos.isEmpty() && !confirmarVariantesConPedidos) {
                    throw new IllegalStateException("El producto tiene variantes con pedidos asociados. Se requiere confirmación para continuar.");
                }
                
                System.out.println("🔵 [SERVICE] Procesando variantes existentes...");
                
                // Separar variantes: las que tienen pedidos vs las que no
                List<ProductoVariante> variantesAMantener = new ArrayList<>();
                List<ProductoVariante> variantesAEliminar = new ArrayList<>();
                
                for (ProductoVariante variante : producto.getVariantes()) {
                    if (detallePedidoRepository.existsByVarianteId(variante.getId())) {
                        // Variantes con pedidos: poner stock en 0, no eliminar
                        Integer stockAnterior = variante.getStockDisponible();
                        
                        if (stockAnterior > 0) {
                            // Registrar movimiento de ajuste negativo si había stock
                            MovimientoStock movimiento = MovimientoStock.builder()
                                    .variante(variante)
                                    .tipo(TipoMovimiento.AJUSTE_INVENTARIO_NEGATIVO)
                                    .cantidad(stockAnterior)
                                    .fecha(LocalDateTime.now())
                                    .build();
                            movimientoStockRepository.save(movimiento);
                            System.out.println("🔵 [SERVICE] Movimiento registrado: stock de " + variante.getSku() + " será puesto en 0");
                        }
                        
                        variante.setStockDisponible(0);
                        variantesAMantener.add(variante);
                        System.out.println("🔵 [SERVICE] Variante " + variante.getSku() + " mantenida (tiene pedidos), stock puesto en 0");
                    } else {
                        // Variante sin pedidos: se puede eliminar
                        variantesAEliminar.add(variante);
                        System.out.println("🔵 [SERVICE] Variante " + variante.getSku() + " será eliminada (no tiene pedidos)");
                    }
                }
                
                // Eliminar solo las variantes que no tienen pedidos
                if (!variantesAEliminar.isEmpty()) {
                    for (ProductoVariante variante : variantesAEliminar) {
                        productoVarianteRepository.delete(variante);
                    }
                    productoVarianteRepository.flush();
                    System.out.println("🔵 [SERVICE] " + variantesAEliminar.size() + " variantes eliminadas (sin pedidos)");
                }
                
                // Limpiar la lista en memoria y agregar las que mantenemos
                producto.getVariantes().clear();
                producto.getVariantes().addAll(variantesAMantener);
                
                System.out.println("🔵 [SERVICE] " + variantesAMantener.size() + " variantes mantenidas (con pedidos, stock=0)");
                System.out.println("🔵 [SERVICE] Ahora crear nuevas variantes");
                
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
                
                // Guardar para tener los IDs antes de registrar stock histórico
                producto = productoRepository.save(producto);
                
                // Registrar stock histórico inicial para las nuevas variantes
                for (ProductoVariante variante : producto.getVariantes()) {
                    if (variante.getStockDisponible() > 0) {
                        List<StockHistorico> historicos = stockHistoricoRepository.findByVarianteOrderByFechaAsc(variante);
                        if (historicos.isEmpty()) {
                            registrarStockHistoricoInicial(variante, variante.getStockDisponible());
                        }
                    }
                }
            }
        } else if (request.getStockPorVariante() != null && !request.getStockPorVariante().isEmpty()) {
            // Actualizar solo el stock de variantes existentes y registrar movimientos
            System.out.println("🔵 [SERVICE] Actualizando stock de variantes existentes y registrando movimientos");
            System.out.println("🔵 [SERVICE] Stock por variante recibido: " + request.getStockPorVariante());
            System.out.println("🔵 [SERVICE] Total de variantes existentes: " + producto.getVariantes().size());
            
            if (producto.getVariantes() == null || producto.getVariantes().isEmpty()) {
                throw new IllegalStateException("El producto no tiene variantes para actualizar stock");
            }
            
            for (ProductoVariante variante : producto.getVariantes()) {
                if (variante.getColor() == null || variante.getTalle() == null) {
                    System.out.println("⚠️ [SERVICE] Variante " + variante.getSku() + " tiene color o talle null, saltando...");
                    continue;
                }
                
                String claveVariante = variante.getColor() + "-" + variante.getTalle();
                System.out.println("🔵 [SERVICE] Verificando variante: " + claveVariante + " (SKU: " + variante.getSku() + ")");
                
                if (request.getStockPorVariante().containsKey(claveVariante)) {
                    Integer stockAnterior = variante.getStockDisponible();
                    Integer stockNuevo = request.getStockPorVariante().get(claveVariante);
                    Integer diferencia = stockNuevo - stockAnterior;
                    
                    System.out.println("🔵 [SERVICE] Variante " + variante.getSku() + ": Stock anterior=" + stockAnterior + ", Stock nuevo=" + stockNuevo + ", Diferencia=" + diferencia);
                    
                    // Solo registrar movimiento si hay diferencia
                    if (diferencia != 0) {
                        TipoMovimiento tipoMovimiento;
                        Integer cantidadMovimiento;
                        
                        if (diferencia > 0) {
                            // Entrada de stock
                            tipoMovimiento = TipoMovimiento.AJUSTE_INVENTARIO_POSITIVO;
                            cantidadMovimiento = diferencia;
                            System.out.println("🔵 [SERVICE] Registrando entrada de stock: +" + cantidadMovimiento);
                        } else {
                            // Salida de stock
                            tipoMovimiento = TipoMovimiento.AJUSTE_INVENTARIO_NEGATIVO;
                            cantidadMovimiento = Math.abs(diferencia);
                            System.out.println("🔵 [SERVICE] Registrando salida de stock: -" + cantidadMovimiento);
                        }
                        
                        // Crear y guardar el movimiento de stock
                        MovimientoStock movimiento = MovimientoStock.builder()
                                .variante(variante)
                                .tipo(tipoMovimiento)
                                .cantidad(cantidadMovimiento)
                                .fecha(LocalDateTime.now())
                                .build();
                        
                        movimientoStockRepository.save(movimiento);
                        System.out.println("✅ [SERVICE] Movimiento registrado: " + tipoMovimiento + " - " + cantidadMovimiento + " unidades");
                        
                        // Registrar en stock histórico
                        if (diferencia > 0) {
                            registrarStockHistoricoAjuste(variante, diferencia, StockHistorico.TipoMovimientoStock.AJUSTE_SUMA);
                        } else {
                            registrarStockHistoricoAjuste(variante, Math.abs(diferencia), StockHistorico.TipoMovimientoStock.AJUSTE_RESTA);
                        }
                    }
                    
                    // Actualizar el stock de la variante (se guardará al guardar el producto por cascade)
                    variante.setStockDisponible(stockNuevo);
                    System.out.println("✅ [SERVICE] Stock de variante actualizado: " + variante.getSku() + " -> " + stockNuevo);
                } else {
                    System.out.println("⚠️ [SERVICE] Variante " + claveVariante + " no encontrada en stockPorVariante del request");
                }
            }
            
            // Actualizar precio de todas las variantes si viene en el request
            if (request.getPrecio() != null && request.getPrecio() > 0) {
                System.out.println("🔵 [SERVICE] Actualizando precio de todas las variantes a: " + request.getPrecio());
                for (ProductoVariante variante : producto.getVariantes()) {
                    variante.setPrecio(request.getPrecio());
                    System.out.println("✅ [SERVICE] Precio de variante " + variante.getSku() + " actualizado: " + request.getPrecio());
                }
            }
        } else if (request.getPrecio() != null && request.getPrecio() > 0) {
            // Solo actualizar precio si no se están modificando variantes ni stock
            System.out.println("🔵 [SERVICE] Actualizando solo precio de todas las variantes a: " + request.getPrecio());
            
            if (producto.getVariantes() == null || producto.getVariantes().isEmpty()) {
                System.out.println("⚠️ [SERVICE] El producto no tiene variantes para actualizar precio");
            } else {
                for (ProductoVariante variante : producto.getVariantes()) {
                    variante.setPrecio(request.getPrecio());
                    System.out.println("✅ [SERVICE] Precio de variante " + variante.getSku() + " actualizado: " + request.getPrecio());
                }
            }
        }
        
        // Guardar el producto actualizado (las variantes se guardarán automáticamente por cascade)
        try {
            producto = productoRepository.save(producto);
            System.out.println("✅ [SERVICE] Producto actualizado exitosamente con ID: " + producto.getId());
        } catch (Exception e) {
            System.out.println("🔴 [SERVICE] Error al guardar producto: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al guardar producto: " + e.getMessage(), e);
        }
        
        return producto;
    }

    /**
     * Registra el stock histórico inicial cuando se crea una variante
     */
    private void registrarStockHistoricoInicial(ProductoVariante variante, Integer cantidad) {
        StockHistorico historico = StockHistorico.builder()
                .variante(variante)
                .cantidad(cantidad)
                .stockAcumulado(cantidad)
                .fecha(LocalDateTime.now())
                .motivo("Creación inicial de variante")
                .tipo(StockHistorico.TipoMovimientoStock.ENTRADA_INICIAL)
                .build();
        
        stockHistoricoRepository.save(historico);
        System.out.println("✅ [STOCK HISTORICO] Registrado stock inicial: " + variante.getSku() + " - " + cantidad + " unidades");
    }

    /**
     * Registra un ajuste de stock histórico (suma o resta)
     */
    private void registrarStockHistoricoAjuste(ProductoVariante variante, Integer cantidad, StockHistorico.TipoMovimientoStock tipo) {
        // Obtener el stock acumulado anterior
        StockHistorico ultimoHistorico = stockHistoricoRepository.findFirstByVarianteOrderByFechaDesc(variante);
        Integer stockAnterior = (ultimoHistorico != null) ? ultimoHistorico.getStockAcumulado() : 0;
        
        // Si no hay historial previo, considerar el stock actual de la variante como base
        if (ultimoHistorico == null && variante.getStockDisponible() != null) {
            stockAnterior = variante.getStockDisponible();
        }
        
        // Calcular nuevo stock acumulado
        Integer stockAcumulado;
        Integer cantidadMovimiento;
        if (tipo == StockHistorico.TipoMovimientoStock.AJUSTE_SUMA) {
            stockAcumulado = stockAnterior + cantidad;
            cantidadMovimiento = cantidad;
        } else {
            stockAcumulado = Math.max(0, stockAnterior - cantidad); // No permitir negativo
            cantidadMovimiento = -cantidad;
        }
        
        StockHistorico historico = StockHistorico.builder()
                .variante(variante)
                .cantidad(cantidadMovimiento)
                .stockAcumulado(stockAcumulado)
                .fecha(LocalDateTime.now())
                .motivo("Ajuste manual de stock")
                .tipo(tipo)
                .build();
        
        stockHistoricoRepository.save(historico);
        System.out.println("✅ [STOCK HISTORICO] Registrado ajuste: " + variante.getSku() + " - " + 
                (tipo == StockHistorico.TipoMovimientoStock.AJUSTE_SUMA ? "+" : "-") + cantidad + 
                " unidades (Stock acumulado: " + stockAcumulado + ")");
    }
}
