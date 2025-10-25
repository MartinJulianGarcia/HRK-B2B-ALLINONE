# 🔄 Cambios para convertir Categoria a Enum simple

## ✅ Lo que YA hiciste:
1. ✅ Creaste el nuevo enum `Categoria` con PLANO y TEJIDO

---

## 🔧 Lo que debes hacer ahora:

### **1. ELIMINAR el archivo Categoria.java antiguo**

**Ubicación:** `src/main/java/com/hrk/tienda_b2b/model/Categoria.java`

**El archivo a BORRAR es el que tiene:**
```java
@Entity
@Table(name = "categorias")
public class Categoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // ...
}
```

**¡NO borres el nuevo enum!** El enum que acabas de crear debe quedar.

---

### **2. Actualizar Producto.java**

**Ubicación:** `src/main/java/com/hrk/tienda_b2b/model/Producto.java`

**Reemplaza TODO el contenido con:** `Producto.java.CORRECTO_V2`

**Cambios clave:**
```java
@Enumerated(EnumType.STRING)
@Column(nullable = false)
private Categoria categoria;  // PLANO o TEJIDO (enum, no entidad)
```

**Asegúrate de eliminar cualquier relación `@ManyToOne` con Categoria.**

---

### **3. Verificar ProductoRepository.java**

**Ya debería estar correcto**, pero verifica que tenga:

```java
List<Producto> findByCategoria(Categoria categoria);  // Enum, no entidad
```

---

### **4. RECREAR la base de datos**

**Opción A - En MySQL:**
```sql
DROP DATABASE tienda_b2b;
CREATE DATABASE tienda_b2b;
```

**Opción B - Temporal en application.properties:**
```properties
# Cambiar temporalmente a:
spring.jpa.hibernate.ddl-auto=create-drop

# Reiniciar el backend

# Luego volver a cambiar a:
spring.jpa.hibernate.ddl-auto=update
```

---

### **5. Verificar imports en Producto.java**

Después de actualizar, presiona `Ctrl + Alt + O` en IntelliJ para optimizar imports.

Asegúrate de que el import de `Categoria` sea:
```java
import com.hrk.tienda_b2b.model.Categoria;  // El enum, no la entidad
```

---

## 🚀 Orden de pasos:

1. **ELIMINAR** `Categoria.java` (el archivo @Entity con tabla)
2. **MANTENER** `Categoria.java` (el nuevo enum con PLANO/TEJIDO)
3. **ACTUALIZAR** `Producto.java` con `Producto.java.CORRECTO_V2`
4. **RECREAR** la base de datos
5. **REINICIAR** el backend

---

## ✅ ¿Cómo saber si funcionó?

El backend debería iniciar sin errores:
```
Started TiendaB2bHrkApplication in X.XXX seconds
```

Si ves:
```
Process finished with exit code 0
```
Es que algo salió mal.

---

## 🔍 Estructura final correcta:

```
model/
├── Categoria.java        → ENUM con PLANO, TEJIDO
├── TipoProducto.java     → ENUM con REMERA, PANTALON, etc.
├── Producto.java         → @Entity con tipo (TipoProducto) y categoria (Categoria)
├── ProductoVariante.java → @Entity
├── Usuario.java          → @Entity
└── ... (otras entidades)
```

**NO debe existir:**
- ❌ `Categoria.java` con `@Entity` y `@Table`
- ❌ Tabla `categorias` en la base de datos

---

## 📝 Nota sobre seed data:

Cuando crees productos en el backend, ahora debes asignarles:

```java
Producto producto = Producto.builder()
    .nombre("Remera Básica")
    .tipo(TipoProducto.REMERA)
    .categoria(Categoria.TEJIDO)  // PLANO o TEJIDO
    .build();
```

---

¡Con esto debería funcionar! 🎉


