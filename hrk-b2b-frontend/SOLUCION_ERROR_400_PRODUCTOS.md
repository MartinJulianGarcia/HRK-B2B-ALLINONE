# 🔧 Solución Error 400 al Subir Productos

## 🐛 Problemas Identificados

### 1. **ProductoService.java** - Error en el Builder
El builder de `Producto` intentaba setear campos `.precio()` y `.stock()` que **NO EXISTEN** en la clase `Producto`. Estos campos solo existen en `ProductoVariante`.

```java
// ❌ INCORRECTO (causaba error 400)
Producto producto = Producto.builder()
    .nombre(request.getNombre())
    .tipo(request.getTipo())
    .categoria(request.getCategoria())
    .descripcion(request.getDescripcion())
    .precio(request.getPrecio())    // ❌ Este campo no existe
    .stock(request.getStock())      // ❌ Este campo no existe
    .build();

// ✅ CORRECTO
Producto producto = Producto.builder()
    .nombre(request.getNombre())
    .tipo(request.getTipo())
    .categoria(request.getCategoria())
    .descripcion(request.getDescripcion())
    .build();
```

### 2. **ProductoVariante.java** - Tabla Incorrecta
La anotación `@Table(name = "productos")` estaba incorrecta porque esa tabla ya la usa la entidad `Producto`.

```java
// ❌ INCORRECTO
@Table(name = "productos")  // Conflicto con Producto

// ✅ CORRECTO
@Table(name = "producto_variantes")
```

### 3. **Frontend** - Categoría podía ser string vacío
El frontend permitía enviar `categoria` como string vacío (`''`) en lugar de `null`.

### 4. **ProductoController.java** - Faltaban validaciones detalladas
No había validaciones completas ni mensajes de error descriptivos.

---

## ✅ Archivos Corregidos

### Backend (Java)
1. ✅ `ProductoService.java` - Eliminados campos inexistentes del builder
2. ✅ `ProductoController.java` - Agregadas validaciones completas y logs detallados
3. ✅ `ProductoVariante.java` - Corregido nombre de tabla y limpiado el código

### Frontend (Angular)
1. ✅ `add-product-page.component.ts` - Cambiado tipo de `categoria` de `''` a `null`
2. ✅ `add-product-page.component.html` - Corregido select con `[ngValue]="null"`
3. ✅ `products.service.ts` - Agregada validación antes de enviar

---

## 🚀 Pasos para Probar

### 1. **Reiniciar el Backend**
Si el backend está corriendo, reinícialo para cargar los cambios:
```bash
# En la carpeta del backend
mvn spring-boot:run
# O si usas el IDE, detén y vuelve a iniciar
```

### 2. **Limpiar la Base de Datos (Opcional pero Recomendado)**
Si ya creaste productos con errores, es mejor limpiar las tablas:

```sql
-- Si usas H2 o MySQL
DROP TABLE IF EXISTS producto_variantes;
DROP TABLE IF EXISTS productos;
```

O en el `application.properties`:
```properties
spring.jpa.hibernate.ddl-auto=create-drop
```

### 3. **Probar en el Frontend**
1. Abre el formulario de agregar producto
2. Completa **TODOS** los campos:
   - ✅ Nombre: `Remera Test`
   - ✅ Tipo: `REMERA`
   - ✅ **Categoría: `TEJIDO` o `PLANO`** (no dejar vacío)
   - ✅ SKU: `TEST-001`
   - ✅ Colores: Selecciona al menos 1 (ej: Blanco, Negro)
   - ✅ Talles: Selecciona al menos 1 (ej: S, M, L)
   - ✅ Precio: `1500`
   - ✅ Stock: `100`
   - ✅ Descripción: `Producto de prueba`

3. Envía el formulario

---

## 📋 Verificación de Logs

### En el Backend deberías ver:
```
=================================================
🔵 [CONTROLLER] Request recibido
🔵 [CONTROLLER] Nombre: Remera Test
🔵 [CONTROLLER] Tipo: REMERA
🔵 [CONTROLLER] Categoría: TEJIDO
🔵 [CONTROLLER] SKU: TEST-001
🔵 [CONTROLLER] Colores: [Blanco, Negro]
🔵 [CONTROLLER] Talles: [S, M, L]
🔵 [CONTROLLER] Precio: 1500.0
🔵 [CONTROLLER] Stock: 100
🔵 [CONTROLLER] Descripción: Producto de prueba
🔵 [CONTROLLER] Imagen URL: null
=================================================
🔵 [SERVICE] Creando producto con request: ...
🔵 [SERVICE] Producto base creado: Remera Test
🔵 [SERVICE] Imagen URL: /images/categories/remera.jpg
🔵 [SERVICE] Producto guardado con ID: 1
🔵 [SERVICE] Creando 6 variantes con stock 16 cada una
🔵 [SERVICE] Variante creada: TEST-001-BL-S
🔵 [SERVICE] Variante creada: TEST-001-BL-M
🔵 [SERVICE] Variante creada: TEST-001-BL-L
🔵 [SERVICE] Variante creada: TEST-001-NE-S
🔵 [SERVICE] Variante creada: TEST-001-NE-M
🔵 [SERVICE] Variante creada: TEST-001-NE-L
✅ [SERVICE] Producto guardado con 6 variantes
✅ [CONTROLLER] Producto creado exitosamente con ID: 1
=================================================
```

### En el Frontend deberías ver:
```
🔵 [FRONTEND] Datos del producto a crear: {nombre: 'Remera Test', ...}
🔵 [FRONTEND] Request al backend: {nombre: 'Remera Test', ...}
Producto creado: {id: 1, nombre: 'Remera Test', ...}
```

---

## 🔍 Si Aún Tienes Error 400

### Verifica en la consola del backend qué campo está causando el problema:
```
🔴 [CONTROLLER] Error: <mensaje específico>
```

### Causas comunes:
1. ❌ **Categoría vacía** - Asegúrate de seleccionar TEJIDO o PLANO
2. ❌ **Tipo vacío** - Asegúrate de seleccionar un tipo de la lista
3. ❌ **Colores o Talles vacíos** - Debes seleccionar al menos uno
4. ❌ **Precio o Stock inválidos** - Precio > 0, Stock >= 0
5. ❌ **Backend no reiniciado** - Los cambios en Java requieren reiniciar

---

## 📚 Estructura Final

### Modelo Producto (sin precio/stock)
```java
@Entity
@Table(name = "productos")
public class Producto {
    private Long id;
    private String nombre;
    private String descripcion;
    private TipoProducto tipo;
    private String imagenUrl;
    private Categoria categoria;
    private List<ProductoVariante> variantes;  // Aquí van los precios/stocks
}
```

### Modelo ProductoVariante (con precio/stock)
```java
@Entity
@Table(name = "producto_variantes")
public class ProductoVariante {
    private Long id;
    private Producto producto;
    private String sku;
    private String color;
    private String talle;
    private Double precio;         // ✅ Precio por variante
    private Integer stockDisponible; // ✅ Stock por variante
}
```

---

## 💡 Resultado Esperado

Al crear un producto con:
- 2 colores (Blanco, Negro)
- 3 talles (S, M, L)
- Stock total: 100
- Precio: 1500

Se crearán **6 variantes**:
1. TEST-001-BL-S - Blanco/S - Precio: 1500 - Stock: 16
2. TEST-001-BL-M - Blanco/M - Precio: 1500 - Stock: 16
3. TEST-001-BL-L - Blanco/L - Precio: 1500 - Stock: 16
4. TEST-001-NE-S - Negro/S - Precio: 1500 - Stock: 16
5. TEST-001-NE-M - Negro/M - Precio: 1500 - Stock: 16
6. TEST-001-NE-L - Negro/L - Precio: 1500 - Stock: 16

---

## 🎯 Resumen de Cambios Críticos

1. **ELIMINADO** `.precio()` y `.stock()` del builder de Producto
2. **CORREGIDO** nombre de tabla en ProductoVariante
3. **AGREGADAS** validaciones completas en el Controller
4. **MEJORADOS** logs para debug
5. **CORREGIDO** manejo de categoría vacía en el frontend

¡Ahora el sistema debería funcionar correctamente! 🎉

