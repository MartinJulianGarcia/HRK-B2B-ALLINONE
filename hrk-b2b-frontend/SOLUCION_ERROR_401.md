# 🔴 SOLUCIÓN DEFINITIVA - ERROR 401 EN REGISTRO

## 🎯 **PROBLEMA IDENTIFICADO:**

El error 401 significa que **Spring Security está bloqueando la petición** antes de que llegue al endpoint de registro. Esto ocurre porque:

1. El `SecurityConfig` tiene `cors().disable()` que interfiere con `CorsConfig`
2. El filtro JWT está intentando validar todas las peticiones, incluso las de `/api/auth/**`

---

## ✅ **SOLUCIÓN - 2 ARCHIVOS A ACTUALIZAR EN EL BACKEND:**

### **ARCHIVO 1: SecurityConfig.java**

**Ubicación:** `src/main/java/com/hrk/tienda_b2b/config/SecurityConfig.java`

**Reemplaza TODO el contenido con:**

```java
package com.hrk.tienda_b2b.config;

import com.hrk.tienda_b2b.security.JwtAuthenticationEntryPoint;
import com.hrk.tienda_b2b.security.JwtRequestFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtRequestFilter jwtRequestFilter;

    public SecurityConfig(
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            @Lazy JwtRequestFilter jwtRequestFilter) {
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtRequestFilter = jwtRequestFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> {}) // ✅ CAMBIO: Usar configuración de CorsConfig
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/productos/**").permitAll()
                .requestMatchers("/api/carrito/**").permitAll()
                .requestMatchers("/api/pedidos/**").permitAll()
                .requestMatchers("/api/usuarios/**").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

**⚠️ CAMBIO CLAVE:** Línea 40 - cambiar de `.cors(cors -> cors.disable())` a `.cors(cors -> {})`

---

### **ARCHIVO 2: CorsConfig.java** (SI NO LO AGREGASTE AÚN)

**Ubicación:** `src/main/java/com/hrk/tienda_b2b/config/CorsConfig.java`

**Contenido completo:**

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

---

## 🚀 **PASOS A SEGUIR:**

### **PASO 1: Actualizar los archivos**

1. ✅ Abre tu proyecto backend en IntelliJ
2. ✅ Copia/actualiza `SecurityConfig.java` (la línea 40 es CRÍTICA)
3. ✅ Verifica que `CorsConfig.java` esté en la carpeta `config`

### **PASO 2: Limpiar y Recompilar**

En IntelliJ:
1. **Build → Clean Project**
2. **Build → Rebuild Project**

### **PASO 3: Reiniciar el Backend**

1. Para la aplicación (botón rojo ⏹️)
2. Ejecuta de nuevo (botón verde ▶️)
3. **Verifica en la consola que diga:**
   ```
   Tomcat started on port(s): 8081 (http)
   ```

### **PASO 4: Probar el Registro**

1. Abre `http://localhost:4200`
2. Ve a "Create Account"
3. Llena el formulario:
   - **Nombre de usuario/razón social:** Test Usuario
   - **Email:** test@hrk.com
   - **CUIT:** 20-12345678-9
   - **Contraseña:** test123
   - **Repetir Contraseña:** test123
4. Presiona "Crear cuenta"

### **PASO 5: Verificar**

Abre F12 → Network → busca `register`:
- ✅ **Status 200** = ¡ÉXITO!
- ❌ **Status 401** = El `SecurityConfig` no se actualizó correctamente
- ❌ **Status 400** = Hay un problema con los datos (revisar consola de IntelliJ)

---

## 🔍 **SI SIGUE DANDO 401:**

Verifica que en la consola de IntelliJ aparezca:
```
Creating filter chain: any request, [
  ...
  CorsFilter...
  ...
]
```

Si NO aparece `CorsFilter`, el archivo `CorsConfig.java` no está en la ubicación correcta.

---

## 📞 **SI TODO FALLA:**

Dame esta información:

1. **¿Actualizaste la línea 40 de `SecurityConfig.java`?**
   - Antes: `.cors(cors -> cors.disable())`
   - Después: `.cors(cors -> {})`

2. **¿El archivo `CorsConfig.java` está en:** `src/main/java/com/hrk/tienda_b2b/config/`?

3. **¿Qué aparece en la consola de IntelliJ al iniciar el backend?**

4. **¿Qué error aparece en la pestaña "Response" de la petición `register` en el navegador?**

---

¡Actualiza estos dos archivos y reinicia el backend! 🚀

