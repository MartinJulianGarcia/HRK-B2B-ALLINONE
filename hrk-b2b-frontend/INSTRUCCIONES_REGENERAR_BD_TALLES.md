# 🔄 Regenerar Base de Datos para Aplicar División de Talles

## ⚠️ IMPORTANTE

Ya se cambió `application.properties` a `ddl-auto=create`. 

**Esto significa que al reiniciar el backend, se borrarán TODOS los datos:**
- ✅ Usuarios
- ✅ Productos
- ✅ Carritos
- ✅ Pedidos
- ✅ Devoluciones

---

## 📋 Pasos a Seguir

### **Paso 1: Ir a la Terminal del Backend**

Navega a la carpeta del backend:

```bash
cd "C:\Users\HP\Desktop\UCEMA\PRIMER CUATRI 2025\TRABAJO FINAL\tienda-b2b\tienda-b2b"
```

### **Paso 2: Detener el Backend Actual**

Si el backend está corriendo, presiona `Ctrl+C` para detenerlo.

### **Paso 3: Iniciar el Backend (Regenerará la BD)**

```bash
mvn spring-boot:run
```

**Espera a ver este mensaje:**
```
Started TiendaB2bApplication in X.XXX seconds
```

🎉 **¡La base de datos se regeneró automáticamente!**

### **Paso 4: DETENER el Backend Nuevamente**

Presiona `Ctrl+C` para detenerlo.

### **Paso 5: Volver a Cambiar a `update`**

**⚠️ MUY IMPORTANTE:** Necesitas volver a cambiar `application.properties`:

Abrir: `src/main/resources/application.properties`

Cambiar esta línea:
```properties
spring.jpa.hibernate.ddl-auto=create
```

Por:
```properties
spring.jpa.hibernate.ddl-auto=update
```

**Si no haces este paso, se borrará la BD cada vez que reinicies el backend.**

### **Paso 6: Reiniciar el Backend Normalmente**

```bash
mvn spring-boot:run
```

---

## 📝 Recrear Datos de Prueba

Ahora necesitas recrear los datos básicos:

### **1. Registrar Usuario Admin**

Ve al frontend y registra un usuario:
- **Email:** admin@test.com
- **Password:** admin123
- **Nombre:** Admin
- **Tipo:** ADMIN (si es posible, sino cambiar en la BD)

### **2. Registrar Usuario Cliente**

- **Email:** cliente@test.com
- **Password:** cliente123
- **Nombre:** Cliente Test
- **Tipo:** CLIENTE

### **3. Crear Producto de Prueba con Talles Divididos**

Ingresa con el usuario Admin y crea un producto:

```
Nombre: REMERA TEST
Tipo: REMERA
Categoría: PLANO
SKU: TEST-2024
Colores: Negro, Blanco, Azul
Talles: 1/2          ← ¡Importante! Usar 1/2
Stock: 60
Precio: 25000
Descripción: Producto de prueba con talles divididos
```

**Resultado esperado en los logs:**
```
🔵 [SERVICE] Creando 6 variantes con stock 10 cada una
🔵 [SERVICE] Variante creada: TEST-2024-NE-1
🔵 [SERVICE] Variante creada: TEST-2024-NE-2
🔵 [SERVICE] Variante creada: TEST-2024-BL-1
🔵 [SERVICE] Variante creada: TEST-2024-BL-2
🔵 [SERVICE] Variante creada: TEST-2024-AZ-1
🔵 [SERVICE] Variante creada: TEST-2024-AZ-2
✅ [SERVICE] Producto guardado con 6 variantes
```

---

## ✅ Verificación

### **En el Catálogo:**

Deberías ver:

```
┌─────────┬─────────┬─────────┐
│ SKU     │ Talle 1 │ Talle 2 │
├─────────┼─────────┼─────────┤
│ Azul    │  [10]   │  [10]   │
│ Blanco  │  [10]   │  [10]   │
│ Negro   │  [10]   │  [10]   │
└─────────┴─────────┴─────────┘
```

### **Prueba Funcional:**

1. Agrega cantidades diferentes:
   - Azul Talle 1: 3 unidades
   - Azul Talle 2: 5 unidades

2. Presiona "AGREGAR AL PEDIDO"

3. Verifica en el carrito que hay **2 entradas separadas**:
   - ✅ Azul - Talle 1: 3 unidades
   - ✅ Azul - Talle 2: 5 unidades

**NO debería sumar 8 unidades en la misma variante.**

---

## 🐛 Problemas Comunes

### **Problema 1: Error al iniciar el backend**
```
Error creating bean with name 'entityManagerFactory'
```
**Solución:** MySQL no está corriendo. Inicia MySQL Workbench o XAMPP.

### **Problema 2: Sigo viendo talles "1/2" juntos**
**Solución:** El producto fue creado antes del cambio. Elimínalo y créalo nuevamente (ahora sí debería funcionar el DELETE porque la BD está limpia).

### **Problema 3: El frontend no carga los productos**
**Solución:** La BD está vacía. Crea productos nuevamente.

### **Problema 4: No puedo hacer login**
**Solución:** Los usuarios se borraron. Regístrate nuevamente.

---

## 📊 Resumen de Cambios

### **Antes:**
```
Input: Talle "1/2"
Backend crea: 1 variante con talle "1/2"
Frontend muestra: "1/2" como 1 columna
Problema: Al agregar Talle 1 y Talle 2, se sumaban en la misma variante
```

### **Ahora:**
```
Input: Talle "1/2"
Backend crea: 2 variantes separadas ("1" y "2")
Frontend muestra: "Talle 1" y "Talle 2" como columnas separadas
Solución: Cada talle es independiente y tiene su propio stock/cantidad
```

---

## ✅ Checklist Final

- [ ] Backend detenido
- [ ] `application.properties` tiene `ddl-auto=create`
- [ ] Backend iniciado (regeneró la BD)
- [ ] Backend detenido nuevamente
- [ ] `application.properties` cambiado a `ddl-auto=update`
- [ ] Backend iniciado nuevamente
- [ ] Usuario admin registrado
- [ ] Producto de prueba creado con talle "1/2"
- [ ] Verificado en logs que se crearon 6 variantes
- [ ] Verificado en catálogo que se ven 2 columnas (Talle 1 y Talle 2)
- [ ] Probado agregar diferentes cantidades al carrito
- [ ] Verificado que son entradas independientes

---

¡Listo! La base de datos está regenerada y los talles divididos están funcionando. 🎉

