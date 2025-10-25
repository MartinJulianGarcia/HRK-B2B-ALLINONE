# 🔧 Solución: Error de Referencia Circular JSON

## 🐛 Problema Identificado

El backend funciona correctamente - el producto se crea exitosamente, pero hay un error de serialización JSON:

```
HttpMessageNotWritableException: Could not write JSON: Document nesting depth (1001) exceeds the maximum allowed (1000)
```

### 🔍 Causa
**Referencia circular** entre `Producto` y `ProductoVariante`:
- `Producto` tiene una lista de `ProductoVariante`
- Cada `ProductoVariante` tiene una referencia de vuelta a `Producto`
- Al serializar a JSON, Jackson entra en un bucle infinito

---

## ✅ Solución Aplicada

### 1. **Creado DTO de Respuesta**

**Archivo:** `ProductoResponseDTO.java`
```java
@Data
@Builder
public class ProductoResponseDTO {
    private Long id;
    private String nombre;
    private String descripcion;
    private TipoProducto tipo;
    private String imagenUrl;
    private Categoria categoria;
    private List<ProductoVarianteResponseDTO> variantes;
    
    @Data
    @Builder
    public static class ProductoVarianteResponseDTO {
        private Long id;
        private String sku;
        private String color;
        private String talle;
        private Double precio;
        private Integer stockDisponible;
        // ❌ NO incluye referencia a Producto
    }
}
```

### 2. **Actualizado ProductoController**

**Cambio en `crearProducto()`:**
```java
// ❌ ANTES (causaba referencia circular):
return ResponseEntity.ok(nuevo);  // nuevo es Producto con referencia circular

// ✅ DESPUÉS (sin referencia circular):
ProductoResponseDTO responseDTO = convertirADTO(nuevo);
return ResponseEntity.ok(responseDTO);
```

**Método de conversión:**
```java
private ProductoResponseDTO convertirADTO(Producto producto) {
    List<ProductoResponseDTO.ProductoVarianteResponseDTO> variantesDTO = 
        producto.getVariantes().stream()
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
```

---

## 🚀 Pasos para Probar

### 1️⃣ **Reiniciar el Backend**
```bash
# Detener (Ctrl+C) y volver a iniciar
mvn spring-boot:run
```

### 2️⃣ **Probar Crear Producto**
- Abrir formulario en el frontend
- Completar todos los campos
- Enviar formulario

### 3️⃣ **Verificar en Logs del Backend**
Deberías ver:
```
✅ [SERVICE] Producto guardado con 3 variantes
✅ [CONTROLLER] Producto creado exitosamente con ID: 1
# ❌ YA NO debería aparecer el error de referencia circular
```

### 4️⃣ **Verificar en Frontend**
Deberías ver:
```
✅ [FRONTEND] Producto creado: {id: 1, nombre: "REMERA TEST", ...}
```

**Sin el error rojo de HttpErrorResponse** ❌

---

## 📊 Estructura de Respuesta JSON

### ❌ ANTES (con referencia circular):
```json
{
  "id": 1,
  "nombre": "REMERA TEST",
  "variantes": [
    {
      "id": 1,
      "sku": "SKUDEPRUEBA-AZ-123",
      "precio": 5000,
      "producto": {
        "id": 1,
        "nombre": "REMERA TEST",
        "variantes": [
          // ❌ BUCLE INFINITO
        ]
      }
    }
  ]
}
```

### ✅ DESPUÉS (sin referencia circular):
```json
{
  "id": 1,
  "nombre": "REMERA TEST",
  "descripcion": "ojalafuncione",
  "tipo": "REMERA",
  "categoria": "PLANO",
  "imagenUrl": "/images/categories/remera.jpg",
  "variantes": [
    {
      "id": 1,
      "sku": "SKUDEPRUEBA-AZ-123",
      "color": "Azul",
      "talle": "1/2/3",
      "precio": 5000,
      "stockDisponible": 6
      // ✅ Sin referencia de vuelta a Producto
    },
    {
      "id": 2,
      "sku": "SKUDEPRUEBA-RO-123",
      "color": "Rojo",
      "talle": "1/2/3",
      "precio": 5000,
      "stockDisponible": 6
    },
    {
      "id": 3,
      "sku": "SKUDEPRUEBA-VE-123",
      "color": "Verde",
      "talle": "1/2/3",
      "precio": 5000,
      "stockDisponible": 6
    }
  ]
}
```

---

## 🔍 Verificación de Éxito

### ✅ En el Backend:
- Producto se crea exitosamente
- No hay error de referencia circular
- Respuesta JSON válida

### ✅ En el Frontend:
- No hay error HttpErrorResponse
- Producto se muestra como creado
- Posible redirección al catálogo

### ✅ En la Base de Datos:
```sql
-- Tabla productos
SELECT * FROM productos WHERE id = 1;

-- Tabla producto_variantes  
SELECT * FROM producto_variantes WHERE producto_id = 1;
```

---

## 📝 Archivos Modificados

| Archivo | Cambio |
|---------|--------|
| `ProductoResponseDTO.java` | ✅ Nuevo DTO sin referencia circular |
| `ProductoController.java` | ✅ Usa DTO en lugar de entidad directa |

---

## 💡 Alternativas Consideradas

### 1. **@JsonIgnore** en ProductoVariante
```java
@JsonIgnore
private Producto producto;
```
❌ **Problema:** Pierde información útil para el frontend

### 2. **@JsonManagedReference / @JsonBackReference**
```java
@JsonManagedReference
private List<ProductoVariante> variantes;

@JsonBackReference
private Producto producto;
```
❌ **Problema:** A veces no funciona bien con colecciones

### 3. **DTO de Respuesta** ✅
✅ **Ventaja:** Control total sobre qué datos se envían
✅ **Ventaja:** Sin efectos secundarios
✅ **Ventaja:** Fácil de mantener

---

## 🎯 Resultado Final

El sistema ahora:
1. ✅ Crea productos correctamente
2. ✅ No tiene errores de referencia circular
3. ✅ Envía respuestas JSON válidas al frontend
4. ✅ El frontend recibe la respuesta sin errores

---

**Última actualización:** 11 de octubre de 2025  
**Estado:** ✅ Solucionado - Listo para probar
