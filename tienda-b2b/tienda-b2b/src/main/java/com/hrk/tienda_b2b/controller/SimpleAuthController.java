package com.hrk.tienda_b2b.controller;

import com.hrk.tienda_b2b.dto.RegisterRequest;
import com.hrk.tienda_b2b.dto.LoginRequest;
import com.hrk.tienda_b2b.dto.UsuarioDTO;
import com.hrk.tienda_b2b.model.Usuario;
import com.hrk.tienda_b2b.service.UsuarioService;
import com.hrk.tienda_b2b.service.EmailService;
import com.hrk.tienda_b2b.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class SimpleAuthController {
    
    private final UsuarioService usuarioService;
    private final EmailService emailService;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        log.info("🔵 [SIMPLE] Registro llamado con email: {}", request.getEmail());

        try {
            Usuario usuario = usuarioService.registrar(request);
            UsuarioDTO usuarioDTO = UsuarioDTO.fromEntity(usuario);
            String token = jwtService.generateToken(usuario);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Usuario registrado exitosamente");
            response.put("usuario", usuarioDTO);
            response.put("token", token);
            response.put("mustChangePassword", usuarioDTO.getMustChangePassword());
            
            log.info("🟢 [SIMPLE] Usuario registrado: {}", usuario.getEmail());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("🔴 [SIMPLE] Error en registro: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        log.info("🔵 [SIMPLE] Login llamado con email: {}", request.getEmail());

        try {
            Usuario usuario = usuarioService.login(request);
            UsuarioDTO usuarioDTO = UsuarioDTO.fromEntity(usuario);
            String token = jwtService.generateToken(usuario);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Login exitoso");
            response.put("usuario", usuarioDTO);
            response.put("token", token);
            response.put("mustChangePassword", usuarioDTO.getMustChangePassword());
            
            log.info("🟢 [SIMPLE] Login exitoso: {}", usuario.getEmail());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("🔴 [SIMPLE] Error en login: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/recuperar-contraseña")
    public ResponseEntity<?> recuperarContraseña(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        log.info("🔵 [SIMPLE] Recuperación de contraseña solicitada para email: {}", email);

        try {
            // Buscar usuario por email
            var usuarioOpt = usuarioService.obtenerPorEmail(email);
            
            if (usuarioOpt.isEmpty()) {
                log.warn("🟡 [SIMPLE] Email no encontrado: {}", email);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "No se encontró un usuario con ese email");
                // Por seguridad, no revelamos si el email existe o no
                return ResponseEntity.ok(errorResponse);
            }

            Usuario usuario = usuarioOpt.get();
            
            // Verificar que el usuario esté activo
            if (usuario.getActivo() == null || !usuario.getActivo()) {
                log.warn("🟡 [SIMPLE] Usuario inactivo: {}", email);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "No se encontró un usuario con ese email");
                return ResponseEntity.ok(errorResponse);
            }

            String passwordTemporal = usuarioService.generarPasswordTemporal();
            usuarioService.actualizarPassword(usuario, passwordTemporal, true);

            emailService.enviarPasswordTemporal(
                usuario.getEmail(),
                usuario.getNombreRazonSocial(),
                passwordTemporal
            );

            log.info("🟢 [SIMPLE] Email de recuperación enviado a: {}", email);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Se ha enviado un email con una contraseña temporal a " + email);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("🔴 [SIMPLE] Error al recuperar contraseña: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error al enviar el email de recuperación. Por favor intenta nuevamente.");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/test")
    public String test() {
        log.info("🔵 [SIMPLE] Test endpoint llamado - DEVOLVIENDO RESPUESTA");
        String respuesta = "Auth Controller funcionando con servicios - " + System.currentTimeMillis();
        log.info("🔵 [SIMPLE] Respuesta: {}", respuesta);
        return respuesta;
    }
}