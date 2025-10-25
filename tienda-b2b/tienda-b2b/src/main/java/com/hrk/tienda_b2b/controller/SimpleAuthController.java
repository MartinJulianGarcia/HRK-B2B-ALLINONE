package com.hrk.tienda_b2b.controller;

import com.hrk.tienda_b2b.dto.RegisterRequest;
import com.hrk.tienda_b2b.dto.LoginRequest;
import com.hrk.tienda_b2b.dto.UsuarioDTO;
import com.hrk.tienda_b2b.model.Usuario;
import com.hrk.tienda_b2b.service.UsuarioService;
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

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        log.info("🔵 [SIMPLE] Registro llamado con email: {}", request.getEmail());

        try {
            Usuario usuario = usuarioService.registrar(request);
            UsuarioDTO usuarioDTO = UsuarioDTO.fromEntity(usuario);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Usuario registrado exitosamente");
            response.put("usuario", usuarioDTO);
            response.put("token", "fake-token-for-testing");
            
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
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Login exitoso");
            response.put("usuario", usuarioDTO);
            response.put("token", "fake-token-for-testing");
            
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

    @GetMapping("/test")
    public String test() {
        log.info("🔵 [SIMPLE] Test endpoint llamado - DEVOLVIENDO RESPUESTA");
        String respuesta = "Auth Controller funcionando con servicios - " + System.currentTimeMillis();
        log.info("🔵 [SIMPLE] Respuesta: {}", respuesta);
        return respuesta;
    }
}