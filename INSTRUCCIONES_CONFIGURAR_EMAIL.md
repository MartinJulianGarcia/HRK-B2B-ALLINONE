# 📧 Instrucciones para Configurar el Email de Recuperación de Contraseña

## ⚠️ Error Actual
El error 500 se debe a que las propiedades de email en `application.properties` tienen valores de ejemplo (`tu-email@gmail.com`). Necesitas cambiarlos por tus datos reales.

## 📝 Pasos para Configurar Gmail

### 1. Abre el archivo `application.properties`
Ruta: `tienda-b2b/tienda-b2b/src/main/resources/application.properties`

### 2. Busca estas líneas (al final del archivo):
```properties
# Email Configuration (Gmail SMTP)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=tu-email@gmail.com
spring.mail.password=tu-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.from=tu-email@gmail.com
```

### 3. Reemplaza con tus datos:
```properties
# Email Configuration (Gmail SMTP)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=TU-EMAIL-REAL@gmail.com
spring.mail.password=TU-CONTRASEÑA-DE-APLICACION
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.from=TU-EMAIL-REAL@gmail.com
```

### 4. Cómo obtener la Contraseña de Aplicación de Gmail:

#### Paso 1: Habilitar Verificación en Dos Pasos
1. Ve a tu cuenta de Google: https://myaccount.google.com/
2. Ve a **Seguridad** (Security)
3. Activa **Verificación en dos pasos** si no la tienes activada

#### Paso 2: Generar Contraseña de Aplicación
1. Ve a **Seguridad** → **Verificación en dos pasos**
2. Al final de la página, busca **Contraseñas de aplicaciones** (App passwords)
3. Si no aparece, búscalo directamente: https://myaccount.google.com/apppasswords
4. Selecciona **Correo** como aplicación
5. Selecciona **Otro (nombre personalizado)** como dispositivo
6. Escribe "HRK B2B" o cualquier nombre
7. Haz clic en **Generar**
8. **Copia la contraseña de 16 caracteres** que aparece (formato: `xxxx xxxx xxxx xxxx`)
9. **Úsala sin espacios** en `application.properties`

### 5. Ejemplo Completo:
```properties
# Email Configuration (Gmail SMTP)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=miempresa@gmail.com
spring.mail.password=abcd efgh ijkl mnop
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.from=miempresa@gmail.com
```

**IMPORTANTE**: En `spring.mail.password` pon la contraseña sin espacios: `abcdefghijklmnop`

### 6. Reinicia el Backend
Después de cambiar `application.properties`, reinicia el servidor:
1. Detén el backend (Ctrl+C en la terminal)
2. Inicia nuevamente: `mvn spring-boot:run`

## 🧪 Probar

1. Ve a la página de login
2. Haz clic en "¿Olvidaste tu contraseña?"
3. Ingresa un email que esté registrado en la base de datos
4. Deberías recibir un email con la contraseña

## ⚠️ Si No Funciona

### Verifica en los logs del backend:
Busca mensajes que empiecen con:
- `🔴 [EMAIL SERVICE]` - Indica el error específico
- `✅ [EMAIL SERVICE]` - Indica que el email se envió correctamente

### Errores Comunes:
- **"Invalid credentials"**: La contraseña de aplicación es incorrecta
- **"Connection refused"**: Verifica que Gmail no esté bloqueando la conexión
- **"Could not connect"**: Verifica tu conexión a internet

## 🔐 Seguridad

**IMPORTANTE**: La contraseña de aplicación es muy sensible. 
- **NO la compartas**
- **NO la subas a Git** (considera usar variables de entorno en producción)
- Si la expones, revócala en Google y genera una nueva

