# Instrucciones para Implementar el Sistema de Pedidos

## Problema Identificado
El frontend está intentando hacer llamadas a endpoints del backend que no existen:
- `POST /api/pedidos/crear` - para crear pedidos
- `POST /api/pedidos/{id}/items` - para agregar items a pedidos
- `GET /api/pedidos?clienteId=X` - para obtener historial de pedidos
- `POST /api/pedidos/{id}/confirmar` - para confirmar pedidos
- `POST /api/pedidos/{id}/cancelar` - para cancelar pedidos

## Archivos Creados para el Backend

### 1. Controladores
- `PedidoController.java` - Maneja todas las peticiones HTTP relacionadas con pedidos

### 2. Servicios
- `PedidoService.java` - Lógica de negocio para pedidos
- `UsuarioService.java` - Servicio para usuarios

### 3. Modelos/Entidades
- `Pedido.java` - Entidad principal de pedidos
- `PedidoDetalle.java` - Detalles de cada item en un pedido
- `Usuario.java` - Entidad de usuarios
- `ProductoVariante.java` - Variantes de productos
- `Producto.java` - Productos
- `Categoria.java` - Categorías de productos

### 4. Enums
- `EstadoPedido.java` - Estados posibles de un pedido
- `TipoUsuario.java` - Tipos de usuario

### 5. DTOs
- `PedidoResponseDTO.java` - Respuesta de pedidos
- `CreatePedidoRequest.java` - Request para crear pedidos
- `UsuarioDTO.java` - DTO para usuarios

### 6. Repositorios
- `PedidoRepository.java` - Repositorio para pedidos
- `UsuarioRepository.java` - Repositorio para usuarios
- `ProductoVarianteRepository.java` - Repositorio para variantes

## Pasos para Implementar

### 1. Copiar Archivos al Backend
Copia todos los archivos `.java` creados a las carpetas correspondientes en tu proyecto Spring Boot:

```
src/main/java/com/hrk/tienda_b2b/
├── controller/
│   └── PedidoController.java
├── service/
│   ├── PedidoService.java
│   └── UsuarioService.java
├── model/
│   ├── Pedido.java
│   ├── PedidoDetalle.java
│   ├── Usuario.java
│   ├── ProductoVariante.java
│   ├── Producto.java
│   ├── Categoria.java
│   ├── EstadoPedido.java
│   └── TipoUsuario.java
├── dto/
│   ├── PedidoResponseDTO.java
│   ├── CreatePedidoRequest.java
│   └── UsuarioDTO.java
└── repository/
    ├── PedidoRepository.java
    ├── UsuarioRepository.java
    └── ProductoVarianteRepository.java
```

### 2. Verificar Dependencias
Asegúrate de que tu `pom.xml` tenga las dependencias necesarias:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
</dependency>
```

### 3. Configurar Base de Datos
Asegúrate de que tu `application.properties` tenga la configuración de base de datos:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/tu_base_de_datos
spring.datasource.username=tu_usuario
spring.datasource.password=tu_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

### 4. Crear Tablas en Base de Datos
Ejecuta el backend para que Hibernate cree las tablas automáticamente, o ejecuta estos scripts SQL:

```sql
-- Tabla de usuarios
CREATE TABLE usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre_razon_social VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    tipo_usuario VARCHAR(50)
);

-- Tabla de categorías
CREATE TABLE categorias (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL
);

-- Tabla de productos
CREATE TABLE productos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    descripcion TEXT,
    categoria_id BIGINT,
    FOREIGN KEY (categoria_id) REFERENCES categorias(id)
);

-- Tabla de variantes de productos
CREATE TABLE producto_variantes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    producto_id BIGINT NOT NULL,
    sku VARCHAR(255) UNIQUE NOT NULL,
    color VARCHAR(100),
    talle VARCHAR(50),
    precio DECIMAL(10,2) NOT NULL,
    stock_disponible INT,
    FOREIGN KEY (producto_id) REFERENCES productos(id)
);

-- Tabla de pedidos
CREATE TABLE pedidos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    usuario_id BIGINT,
    fecha DATETIME NOT NULL,
    estado VARCHAR(50) NOT NULL,
    total DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

-- Tabla de detalles de pedidos
CREATE TABLE pedido_detalles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pedido_id BIGINT NOT NULL,
    variante_id BIGINT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (pedido_id) REFERENCES pedidos(id),
    FOREIGN KEY (variante_id) REFERENCES producto_variantes(id)
);
```

### 5. Reiniciar Backend
1. Detén el servidor backend
2. Ejecuta `mvn clean install` (si usas Maven) o `./gradlew build` (si usas Gradle)
3. Inicia el backend nuevamente

### 6. Probar el Frontend
1. Ve al carrito en el frontend
2. Agrega algunos productos
3. Haz clic en "Generar pedido"
4. Selecciona un método de pago
5. Confirma el pedido
6. Ve al historial de pedidos para verificar que aparezca

## Verificación
Si todo está correcto, deberías ver en los logs del backend:
- `🔵 [PEDIDO CONTROLLER] Creando pedido para cliente: X`
- `🔵 [PEDIDO SERVICE] Pedido guardado con ID: X`
- `🔵 [PEDIDO CONTROLLER] Agregando item al pedido X`

Y en el frontend deberías ver:
- El pedido creado exitosamente
- El carrito se limpia
- El pedido aparece en el historial

## Notas Importantes
- Asegúrate de que el backend esté corriendo en el puerto 8081
- Verifica que el CORS esté configurado correctamente
- Los logs te ayudarán a identificar cualquier problema
