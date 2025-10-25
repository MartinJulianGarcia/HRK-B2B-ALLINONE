# 🔧 Solución: Error de tipo en ProductoRepository

## ❌ Problema:
```
Cannot compare left expression of type 'com.hrk.tienda_b2b.model.Categoria' 
with right expression of type 'java.lang.String'
```

**Causa:** El método `findByCategoria(String categoria)` en `ProductoRepository` espera un `String`, pero la entidad `Producto` tiene `categoria` como un **enum** (`Categoria`).

---

## ✅ Solución:

Necesitas actualizar 3 archivos en tu backend:

### 1. **ProductoRepository.java**

**Ubicación:** `src/main/java/com/hrk/tienda_b2b/repository/ProductoRepository.java`

**Cambio:**
```java
// ANTES (❌):
List<Producto> findByCategoria(String categoria);
List<Producto> findByCategoriaAndTipo(String categoria, TipoProducto tipo);

// DESPUÉS (✅):
List<Producto> findByCategoria(Categoria categoria);
List<Producto> findByCategoriaAndTipo(Categoria categoria, TipoProducto tipo);
```

**⚠️ Reemplaza TODO el contenido con:** `ProductoRepository.java.CORRECTO`

---

### 2. **Categoria.java** (Crear si no existe)

**Ubicación:** `src/main/java/com/hrk/tienda_b2b/model/Categoria.java`

Este es un nuevo archivo que define el enum de categorías.

**⚠️ Crea este archivo con el contenido de:** `Categoria.java.CORRECTO`

---

### 3. **Producto.java** (Actualizar)

**Ubicación:** `src/main/java/com/hrk/tienda_b2b/model/Producto.java`

**Cambio importante:**
```java
@Enumerated(EnumType.STRING)
@Column(nullable = false)
private Categoria categoria;  // Debe ser tipo Categoria (enum), no String
```

**⚠️ Reemplaza TODO el contenido con:** `Producto.java.CORRECTO`

---

## 🚀 Pasos para aplicar:

1. **Copia los 3 archivos:**
   - `ProductoRepository.java.CORRECTO` → `src/main/java/com/hrk/tienda_b2b/repository/ProductoRepository.java`
   - `Categoria.java.CORRECTO` → `src/main/java/com/hrk/tienda_b2b/model/Categoria.java` (CREAR NUEVO)
   - `Producto.java.CORRECTO` → `src/main/java/com/hrk/tienda_b2b/model/Producto.java`

2. **Verifica los imports en IntelliJ:**
   - Presiona `Ctrl + Alt + O` para optimizar imports
   - Si hay errores rojos, presiona `Alt + Enter` para importar

3. **Limpia la base de datos (IMPORTANTE):**
   
   Como cambiamos el tipo de dato de `categoria` de `String` a `Enum`, necesitas:
   
   **Opción A - Recrear la base de datos:**
   ```sql
   DROP DATABASE tienda_b2b;
   CREATE DATABASE tienda_b2b;
   ```
   
   **Opción B - Cambiar ddl-auto temporalmente:**
   En `application.properties`:
   ```properties
   spring.jpa.hibernate.ddl-auto=create-drop
   ```
   Luego reinicia el backend (creará las tablas desde cero).
   Después vuelve a cambiar a:
   ```properties
   spring.jpa.hibernate.ddl-auto=update
   ```

4. **Reinicia el servidor:**
   - Haz clic en el botón de Play en IntelliJ
   - Deberías ver: `Started TiendaB2bHrkApplication in X.XXX seconds`

---

## ✅ ¿Cómo saber si funcionó?

**En la consola deberías ver:**
```
Started TiendaB2bHrkApplication in X.XXX seconds (JVM running for X.XXX)
```

**Y NO deberías ver:**
```
Process finished with exit code 0
```

**Verifica que el servidor esté activo:**
```bash
curl http://localhost:8081/api/auth/login
```

---

## 📝 Nota sobre compatibilidad con el frontend:

El frontend ya está configurado para usar el enum `Categoria` correctamente en:
- `src/app/app/core/categories.enum.ts`

Los valores del enum coinciden entre frontend y backend:
- Frontend: `Categoria.REMERA`
- Backend: `Categoria.REMERA`

---

## 🔄 Si ya tenías datos en la base:

Si tenías productos guardados con `categoria` como String, necesitas migrarlos manualmente o simplemente recrear la base de datos (como se indica arriba).

Para preservar datos existentes, necesitarías crear un script de migración SQL, pero para desarrollo es más fácil recrear la base de datos.

---

¡Ahora sí debería funcionar completamente! 🎉


