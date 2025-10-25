# 🔧 Instrucciones para Regenerar la Base de Datos

## 🐛 Problema Encontrado

La tabla `productos` en MySQL tiene columnas viejas (`sku`, `precio`, `stock`) que ya **NO existen** en el modelo Java actualizado. Esto causa el error:

```
ERROR: Field 'sku' doesn't have a default value
```

## ✅ Solución Aplicada

He actualizado:

1. ✅ **`Producto.java`** - Eliminados los campos `sku`, `precio` y `stock`
2. ✅ **`ProductoVariante.java`** - Estos campos ahora solo existen aquí
3. ✅ **`ProductoService.java`** - Corregido para no usar campos inexistentes
4. ✅ **`application.properties`** - Cambiado a `ddl-auto=create` para regenerar tablas

---

## 🚀 Pasos a Seguir (IMPORTANTE)

### 1️⃣ **Detener el Backend**
Si está corriendo, detén el backend (Ctrl+C en la terminal o Stop en el IDE).

### 2️⃣ **Reiniciar el Backend**
Vuelve a iniciar el backend:

```bash
# En la carpeta del backend
cd "C:\Users\HP\Desktop\UCEMA\PRIMER CUATRI 2025\TRABAJO FINAL\tienda-b2b\tienda-b2b"
mvn spring-boot:run
```

O si usas tu IDE, simplemente haz clic en "Run".

### 3️⃣ **¿Qué va a pasar?**

Al iniciar, verás en los logs:

```sql
Hibernate: drop table if exists producto_variantes
Hibernate: drop table if exists productos
Hibernate: create table productos (
    id bigint not null auto_increment,
    categoria varchar(255) not null,
    descripcion varchar(1000),
    imagen_url varchar(255),
    nombre varchar(255) not null,
    tipo varchar(255) not null,
    primary key (id)
)
Hibernate: create table producto_variantes (
    id bigint not null auto_increment,
    color varchar(255) not null,
    precio double precision not null,
    sku varchar(255) not null,
    stock_disponible integer not null,
    talle varchar(255) not null,
    version bigint,
    producto_id bigint not null,
    primary key (id)
)
```

✅ **Esto es CORRECTO** - Las tablas se están regenerando con la estructura correcta.

### 4️⃣ **Probar Crear Producto**

Una vez que el backend esté corriendo:

1. Ve al frontend
2. Abre el formulario de agregar producto
3. Completa todos los campos:
   - Nombre: `Remera Test`
   - Tipo: `REMERA`
   - Categoría: `PLANO` o `TEJIDO`
   - SKU: `TEST-001`
   - Colores: Selecciona al menos 1
   - Talles: Selecciona al menos 1
   - Precio: `1500`
   - Stock: `100`
   - Descripción: `Producto de prueba`

4. Envía el formulario

### 5️⃣ **Verificar en los Logs**

Deberías ver en el backend:

```
🔵 [CONTROLLER] Request recibido
🔵 [SERVICE] Creando producto con request: ...
🔵 [SERVICE] Producto base creado: Remera Test
🔵 [SERVICE] Producto guardado con ID: 1
🔵 [SERVICE] Creando 3 variantes...
Hibernate: insert into productos (categoria,descripcion,imagen_url,nombre,tipo,id) values (?,?,?,?,?,?)
Hibernate: insert into producto_variantes (color,precio,producto_id,sku,stock_disponible,talle,version,id) values (?,?,?,?,?,?,?,?)
✅ [SERVICE] Producto guardado con 3 variantes
✅ [CONTROLLER] Producto creado exitosamente con ID: 1
```

---

## ⚠️ IMPORTANTE: Después de la Primera Ejecución

Después de que funcione la primera vez, **CAMBIA de vuelta el `application.properties`**:

1. Abre: `src/main/resources/application.properties`
2. Cambia la línea 10:
   ```properties
   # De esto:
   spring.jpa.hibernate.ddl-auto=create
   
   # A esto:
   spring.jpa.hibernate.ddl-auto=update
   ```

3. Reinicia el backend

**¿Por qué?**
- `create` = Borra y regenera las tablas cada vez que inicias (pierdes todos los datos)
- `update` = Solo actualiza la estructura sin borrar datos

---

## 📋 Estructura Final de las Tablas

### Tabla `productos`
```sql
id          | bigint (PK, AUTO_INCREMENT)
nombre      | varchar(255) NOT NULL
descripcion | varchar(1000)
tipo        | varchar(255) NOT NULL  -- Enum: REMERA, BUZO, etc.
categoria   | varchar(255) NOT NULL  -- Enum: PLANO, TEJIDO
imagen_url  | varchar(255)
```

### Tabla `producto_variantes`
```sql
id                | bigint (PK, AUTO_INCREMENT)
producto_id       | bigint (FK -> productos.id)
sku               | varchar(255) UNIQUE NOT NULL
color             | varchar(255) NOT NULL
talle             | varchar(255) NOT NULL
precio            | double NOT NULL
stock_disponible  | integer NOT NULL
version           | bigint (optimistic locking)
```

---

## 🔍 Si Aún Tienes Problemas

### Error: "Access denied for user 'root'"
Verifica las credenciales en `application.properties`:
```properties
spring.datasource.username=root
spring.datasource.password=Cascuino38
```

### Error: "Unknown database 'tienda_b2b'"
Crea la base de datos en MySQL:
```sql
CREATE DATABASE tienda_b2b;
```

### Error: "Could not create connection to database"
Asegúrate de que MySQL esté corriendo:
```bash
# Windows
net start MySQL80
```

---

## 📦 Resumen de Cambios

| Archivo | Cambio |
|---------|--------|
| `Producto.java` | ❌ Eliminados: `sku`, `precio`, `stock` |
| `ProductoVariante.java` | ✅ Tiene: `sku`, `precio`, `stockDisponible` |
| `ProductoService.java` | ✅ No intenta setear campos inexistentes |
| `application.properties` | ⚠️ Temporalmente: `ddl-auto=create` |

---

## ✅ Lista de Verificación

- [ ] Backend detenido
- [ ] Backend reiniciado
- [ ] Logs muestran "Hibernate: drop table..." y "Hibernate: create table..."
- [ ] Formulario completado con todos los campos
- [ ] Producto creado exitosamente
- [ ] **IMPORTANTE:** `application.properties` cambiado de vuelta a `update`

¡Ahora debería funcionar! 🎉

