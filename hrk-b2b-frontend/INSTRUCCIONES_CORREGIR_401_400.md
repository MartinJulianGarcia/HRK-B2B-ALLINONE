# 🔧 INSTRUCCIONES: Corregir Error 401/400 en Registro

## ❌ **Problema Actual:**
- Error 401 (Unauthorized) seguido de 400 (Bad Request) al intentar registrarse
- Causa: El backend está bloqueando las peticiones del frontend por **CORS**

---

## ✅ **SOLUCIÓN PASO A PASO:**

### **PASO 1: Agregar CORS al Backend** ⚠️ **IMPORTANTE**

1. **Abre tu proyecto Spring Boot en IntelliJ**

2. **Copia el archivo `CorsConfig.java`** desde la raíz del proyecto frontend a:
   ```
   src/main/java/com/hrk/tienda_b2b/config/CorsConfig.java
   ```

3. **Verifica que el contenido sea:**
   ```java
   package com.hrk.tienda_b2b.config;

   import org.springframework.context.annotation.Bean;
   import org.springframework.context.annotation.Configuration;
   import org.springframework.web.cors.CorsConfiguration;
   import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
   import org.springframework.web.filter.CorsFilter;
   import java.util.Arrays;

   @Configuration
   public class CorsConfig {
       @Bean
       public CorsFilter corsFilter() {
           UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
           CorsConfiguration config = new CorsConfiguration();
           
           config.setAllowCredentials(true);
           config.setAllowedOrigins(Arrays.asList(
               "http://localhost:4200",
               "http://localhost:58926",
               "http://localhost:*"
           ));
           config.addAllowedHeader("*");
           config.addAllowedMethod("*");
           
           source.registerCorsConfiguration("/api/**", config);
           return new CorsFilter(source);
       }
   }
   ```

4. **Reinicia el backend:**
   - En IntelliJ, para la aplicación (botón rojo ⏹️)
   - Ejecuta de nuevo (botón verde ▶️)

5. **Verifica que diga en la consola:**
   ```
   Tomcat started on port(s): 8081 (http)
   ```

---

### **PASO 2: Verificar el Frontend**

1. El frontend ya está corriendo en `http://localhost:4200`

2. **Abre el navegador en:** `http://localhost:4200`

3. **Ve a la página de registro** (botón "Create Account")

---

### **PASO 3: Probar el Registro**

1. **Abre las herramientas de desarrollador (F12)**

2. **Ve a la pestaña "Network" (Red)**

3. **Llena el formulario de registro:**
   - **Nombre de usuario/razón social:** Test Usuario
   - **CUIT:** 20-12345678-9
   - **Email:** test@hrk.com
   - **Contraseña:** test123
   - **Repetir Contraseña:** test123

4. **Presiona "Crear cuenta"**

5. **Verifica en la pestaña Network:**
   - Busca la petición `register`
   - **Debe devolver Status: 200 OK** ✅
   - Si sigue dando 401 o 400, revisa el error específico

---

## 🔍 **Errores Comunes:**

### **Error: "El CUIT ya está registrado"**
**Solución:** Usa un CUIT diferente (ejemplo: `20-98765432-1`)

### **Error: "El email ya está registrado"**
**Solución:** Usa un email diferente (ejemplo: `test2@hrk.com`)

### **Error: CORS persiste**
**Solución:** 
1. Verifica que el archivo `CorsConfig.java` esté en la carpeta correcta
2. Reinicia el backend completamente
3. Cierra IntelliJ y abre de nuevo si es necesario

### **Error: Backend no inicia**
**Solución:**
1. Verifica que MySQL esté corriendo
2. Verifica las credenciales en `application.properties`
3. Revisa la consola de IntelliJ para ver el error específico

---

## ✅ **Resultado Esperado:**

Si todo funciona correctamente:

1. **Al registrarte:** Deberías ser redirigido al catálogo (`/catalog`)
2. **En el catálogo:** Deberías ver tu nombre de usuario en el perfil
3. **En localStorage:** Deberías ver el `token` y `currentUser` guardados

---

## 📞 **Si Sigue Fallando:**

Dime exactamente:
1. ¿Agregaste el archivo `CorsConfig.java` al backend?
2. ¿Reiniciaste el backend después de agregarlo?
3. ¿Qué error aparece en la consola del navegador (pestaña Console)?
4. ¿Qué error aparece en la pestaña Network al hacer clic en la petición `register`?
5. ¿Qué error aparece en la consola de IntelliJ (backend)?

---

**¡Ahora procede con el PASO 1!** 🚀

