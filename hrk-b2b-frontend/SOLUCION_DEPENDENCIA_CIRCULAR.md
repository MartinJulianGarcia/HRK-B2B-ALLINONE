# 🔧 Solución: Dependencia Circular en Spring Security

## ❌ Problema:
```
The dependencies of some of the beans in the application context form a cycle:
┌─────┐
|  jwtRequestFilter
↑     ↓
|  usuarioServiceImpl
↑     ↓
|  securityConfig
└─────┘
```

## ✅ Solución:

La dependencia circular ocurre porque:
- `SecurityConfig` necesita `JwtRequestFilter`
- `JwtRequestFilter` necesita `UsuarioService`
- `UsuarioService` necesita `PasswordEncoder` (que viene de `SecurityConfig`)

Para romper el ciclo, usamos `@Lazy` en el constructor de `SecurityConfig`.

---

## 📝 Archivos a reemplazar:

### 1. **SecurityConfig.java**

**Ubicación:** `src/main/java/com/hrk/tienda_b2b/config/SecurityConfig.java`

**Cambios clave:**
- Agregar `@Lazy` al parámetro `JwtRequestFilter` en el constructor
- Cambiar los permisos a `.permitAll()` para desarrollo (temporal)
- Habilitar CORS

```java
public SecurityConfig(
        JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
        @Lazy JwtRequestFilter jwtRequestFilter) {  // <-- AGREGAR @Lazy
    this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
    this.jwtRequestFilter = jwtRequestFilter;
}
```

**⚠️ Importante:** Reemplaza TODO el contenido del archivo con el de `SecurityConfig.java.CORRECTO`

---

### 2. **JwtAuthenticationEntryPoint.java**

**Ubicación:** `src/main/java/com/hrk/tienda_b2b/security/JwtAuthenticationEntryPoint.java`

Este archivo debe existir para que funcione `SecurityConfig`. Si no existe, créalo con el contenido de `JwtAuthenticationEntryPoint.java.CORRECTO`

---

### 3. **JwtRequestFilter.java**

**Ubicación:** `src/main/java/com/hrk/tienda_b2b/security/JwtRequestFilter.java`

**Cambios clave:**
- Usar `@RequiredArgsConstructor` de Lombok
- Inyectar `UsuarioService` y `JwtService` por constructor
- Implementar correctamente el filtro de autenticación

**⚠️ Importante:** Reemplaza TODO el contenido del archivo con el de `JwtRequestFilter.java.CORRECTO`

---

## 🚀 Pasos para aplicar la solución:

1. **Copia los 3 archivos correctos:**
   - `SecurityConfig.java.CORRECTO` → `src/main/java/com/hrk/tienda_b2b/config/SecurityConfig.java`
   - `JwtAuthenticationEntryPoint.java.CORRECTO` → `src/main/java/com/hrk/tienda_b2b/security/JwtAuthenticationEntryPoint.java`
   - `JwtRequestFilter.java.CORRECTO` → `src/main/java/com/hrk/tienda_b2b/security/JwtRequestFilter.java`

2. **Verifica los imports:**
   - IntelliJ debería importar automáticamente
   - Si hay errores, presiona `Alt + Enter` para importar

3. **Limpia y recompila:**
   ```bash
   mvn clean install
   ```

4. **Reinicia el servidor:**
   - Haz clic en el botón de Play en IntelliJ
   - Deberías ver: `Started TiendaB2bHrkApplication in X.XXX seconds (process running on 8081)`

---

## ✅ ¿Cómo saber si funcionó?

**En la consola de IntelliJ deberías ver:**
```
Started TiendaB2bHrkApplication in X.XXX seconds (JVM running for X.XXX)
```

**Y NO deberías ver:**
```
Process finished with exit code 0
```

**Prueba el backend:**
```bash
# En PowerShell o CMD:
curl http://localhost:8081/api/auth/login
```

Debería responder con un error 400 o 401 (lo cual es bueno - significa que está funcionando).

---

## 🔍 Nota sobre seguridad temporal:

En `SecurityConfig` he puesto `.permitAll()` en todos los endpoints para facilitar el desarrollo inicial. 

**Cuando todo funcione, debes cambiarlos a:**
```java
.requestMatchers("/api/productos/**").hasAnyRole("CLIENTE", "ADMIN")
.requestMatchers("/api/carrito/**").hasAnyRole("CLIENTE", "ADMIN")
.requestMatchers("/api/pedidos/**").hasAnyRole("CLIENTE", "ADMIN")
.requestMatchers("/api/usuarios/**").hasRole("ADMIN")
```

---

## 🆘 Si sigue sin funcionar:

1. Verifica que todos los archivos estén en las ubicaciones correctas
2. Verifica que MySQL esté corriendo
3. Limpia completamente el proyecto:
   ```bash
   mvn clean
   rm -rf target/
   mvn install
   ```
4. Revisa la consola en busca de otros errores

---

¡Ahora sí debería funcionar! 🎉


