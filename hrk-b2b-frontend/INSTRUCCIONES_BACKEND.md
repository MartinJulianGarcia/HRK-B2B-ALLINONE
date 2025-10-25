# 🔧 Instrucciones para arreglar el backend

## Problema identificado:
El backend no puede conectarse a MySQL debido a un error de autenticación: **"Public Key Retrieval is not allowed"**

## Solución:

Necesitas actualizar tu archivo `application.properties` en el proyecto del backend (Spring Boot).

La ruta típica es: `src/main/resources/application.properties`

### Opción 1: Agregar allowPublicKeyRetrieval=true a la URL de conexión

Busca la línea que comienza con `spring.datasource.url` y agrégale `allowPublicKeyRetrieval=true`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/tu_base_de_datos?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC
```

### Opción 2: Usar mysql_native_password (Recomendado para desarrollo)

Si sigues teniendo problemas, puedes cambiar la autenticación del usuario MySQL ejecutando estos comandos en MySQL:

```sql
ALTER USER 'tu_usuario'@'localhost' IDENTIFIED WITH mysql_native_password BY 'tu_contraseña';
FLUSH PRIVILEGES;
```

### Ejemplo completo de application.properties:

```properties
# Puerto del servidor
server.port=8081

# Configuración de la base de datos
spring.datasource.url=jdbc:mysql://localhost:3306/tienda_b2b?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=tu_contraseña_aqui
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Configuración de JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# Logging
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

### Pasos adicionales:

1. **Verifica que MySQL esté corriendo** en el puerto 3306
2. **Verifica las credenciales** (usuario y contraseña)
3. **Verifica que la base de datos existe**: 
   ```sql
   CREATE DATABASE IF NOT EXISTS tienda_b2b;
   ```
4. **Reinicia el servidor de Spring Boot** después de hacer los cambios

### Advertencia sobre el dialecto:

También vi este warning en tu log:
```
HHH90000026: MySQL8Dialect has been deprecated; use org.hibernate.dialect.MySQLDialect instead
```

Cambia en `application.properties`:
```properties
# En lugar de:
# spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# Usa:
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
```

---

## Una vez arreglado el backend:

1. Inicia el backend (debería quedar escuchando en el puerto 8081)
2. El frontend ya está configurado para conectarse a `http://localhost:8081/api`
3. Podrás probar el login y registro desde el frontend

¿Te ayudo con algo más?


