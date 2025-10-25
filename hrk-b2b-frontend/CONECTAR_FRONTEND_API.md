# 🔗 Conectar Frontend con API Real

## ✅ Cambios Aplicados

### 1. **Frontend - ProductsService**
- ✅ Actualizado método `list()` para usar API real
- ✅ Agregado fallback a mocks si hay error
- ✅ Interfaces actualizadas para coincidir con backend

### 2. **Backend - ProductoController**
- ✅ Método `listarTodos()` usa DTO de respuesta
- ✅ Método `obtenerPorId()` usa DTO de respuesta
- ✅ Sin referencias circulares en JSON

---

## 🚀 Pasos para Probar

### 1️⃣ **Reiniciar el Backend**
```bash
# Detener (Ctrl+C) y volver a iniciar
mvn spring-boot:run
```

### 2️⃣ **Verificar que el Backend Esté Funcionando**
En los logs deberías ver:
```
🔍 ============== BEANS DETECTADOS ==============
🟢 Bean encontrado: productoController
✅ Started TiendaB2bHrkApplication
```

### 3️⃣ **Abrir el Frontend**
- Navegar a la página de catálogo
- Abrir DevTools (F12) → Console

### 4️⃣ **Verificar en Console del Frontend**
Deberías ver:
```
🔵 [FRONTEND] Obteniendo productos desde la API...
🔵 [FRONTEND] Productos recibidos: [Array de productos]
```

**Si hay error:**
```
🔴 [FRONTEND] Error al obtener productos: [error]
🟡 [FRONTEND] Usando datos mock como fallback
```

### 5️⃣ **Verificar en Console del Backend**
Cuando cargas la página de catálogo, deberías ver:
```
GET "/api/productos", parameters={}
```

---

## 📊 Estructura de Datos Esperada

### Backend devuelve:
```json
[
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
]
```

### Frontend recibe y procesa:
- ✅ Productos con ID real de la BD
- ✅ Imágenes por defecto según tipo
- ✅ Variantes agrupadas por color y talle
- ✅ Precios y stock reales

---

## 🖼️ Visualización de Imágenes

### Imágenes por Defecto
Si `imagenUrl` es `/images/categories/remera.jpg`, el frontend debería mostrar:
- Para tipo "REMERA" → imagen de remera
- Para tipo "BUZO" → imagen de buzo
- Para tipo "CAMPERA" → imagen de campera
- etc.

### Estructura de Archivos de Imágenes
```
src/assets/images/categories/
├── remera.jpg
├── buzo.jpg
├── campera.jpg
├── pantalon.jpg
├── short.jpg
└── ... (otros tipos)
```

---

## 🔍 Verificación de Funcionamiento

### ✅ **Caso Exitoso:**
1. Frontend hace GET a `/api/productos`
2. Backend responde con productos reales
3. Frontend muestra productos en la grilla
4. Imágenes se cargan correctamente
5. Variantes se agrupan por color/talle

### ❌ **Caso de Error (Fallback):**
1. Frontend hace GET a `/api/productos`
2. Backend no responde o da error
3. Frontend usa datos mock
4. Se muestra mensaje de error en console
5. Grilla funciona con datos de prueba

---

## 🐛 Posibles Problemas

### 1. **Error de CORS**
```
Access to fetch at 'http://localhost:8081/api/productos' from origin 'http://localhost:4200' has been blocked by CORS policy
```
**Solución:** Verificar que `@CrossOrigin(origins = "*")` esté en el controller

### 2. **Backend No Responde**
```
🔴 [FRONTEND] Error al obtener productos: HttpErrorResponse
```
**Solución:** 
- Verificar que el backend esté corriendo en puerto 8081
- Verificar que no haya errores de compilación

### 3. **Imágenes No Se Cargan**
**Solución:** 
- Verificar que existan las imágenes en `src/assets/images/categories/`
- Verificar la ruta en `imagenUrl`

### 4. **Productos No Se Muestran**
**Solución:**
- Verificar en DevTools → Network que la petición GET funcione
- Verificar que el componente de grilla esté usando `ProductsService.list()`

---

## 📋 Checklist de Verificación

- [ ] Backend reiniciado y funcionando
- [ ] Frontend carga página de catálogo
- [ ] Console muestra: "🔵 [FRONTEND] Obteniendo productos desde la API..."
- [ ] Console muestra: "🔵 [FRONTEND] Productos recibidos: [Array]"
- [ ] Grilla muestra productos reales (no mocks)
- [ ] Imágenes se cargan correctamente
- [ ] Variantes se muestran agrupadas
- [ ] No hay errores en Console

---

## 🎯 Resultado Esperado

Al completar estos pasos:
1. ✅ La grilla mostrará los productos reales de la base de datos
2. ✅ Las imágenes se cargarán desde `/images/categories/`
3. ✅ Los productos se agruparán por variantes (colores/talles)
4. ✅ Los precios y stock serán reales
5. ✅ Si hay error, se mostrarán los mocks como fallback

---

**Última actualización:** 11 de octubre de 2025  
**Estado:** ✅ Listo para probar
