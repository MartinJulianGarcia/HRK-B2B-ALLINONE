package com.hrk.tienda_b2b.controller;

import com.hrk.tienda_b2b.dto.UsuarioDTO;
import com.hrk.tienda_b2b.model.Usuario;
import com.hrk.tienda_b2b.model.TipoUsuario;
import com.hrk.tienda_b2b.service.UsuarioService;
import com.hrk.tienda_b2b.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class UsuarioController {
    
    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;

    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> obtenerTodos() {
        List<Usuario> usuarios = usuarioService.obtenerTodos();
        List<UsuarioDTO> usuariosDTO = usuarios.stream()
                .map(UsuarioDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(usuariosDTO);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> obtenerPorId(@PathVariable Long id) {
        return usuarioService.obtenerPorId(id)
                .map(UsuarioDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/puede-cambiar-a-admin")
    public ResponseEntity<Map<String, Boolean>> puedeCambiarARolAdmin(@PathVariable Long id) {
        boolean allowed = usuarioService.puedeCambiarRolAAdmin(id);
        Map<String, Boolean> response = new HashMap<>();
        response.put("allowed", allowed);
        response.put("adminExists", usuarioService.existeAdministradorActivo());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/rol")
    public ResponseEntity<UsuarioDTO> cambiarRol(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String nuevoRolStr = request.get("tipoUsuario");
            TipoUsuario nuevoRol = TipoUsuario.valueOf(nuevoRolStr);
            
            Usuario usuario = usuarioService.obtenerPorId(id)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
            
            usuario.setTipoUsuario(nuevoRol);
            Usuario usuarioActualizado = usuarioService.actualizar(usuario);
            
            return ResponseEntity.ok(UsuarioDTO.fromEntity(usuarioActualizado));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarUsuario(@PathVariable Long id, @RequestBody UsuarioDTO usuarioDTO) {
        try {
            Usuario usuario = usuarioService.obtenerPorId(id)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
            
            // Validar y actualizar nombre
            if (usuarioDTO.getNombreRazonSocial() != null && !usuarioDTO.getNombreRazonSocial().trim().isEmpty()) {
                usuario.setNombreRazonSocial(usuarioDTO.getNombreRazonSocial().trim());
            }
            
            // Validar y actualizar CUIT (verificar que no exista en otro usuario)
            if (usuarioDTO.getCuit() != null && !usuarioDTO.getCuit().trim().isEmpty()) {
                String nuevoCuit = usuarioDTO.getCuit().trim();
                // Si el CUIT es diferente al actual, validar que no esté en uso por otro usuario
                if (!nuevoCuit.equals(usuario.getCuit())) {
                    // Verificar si existe otro usuario con este CUIT
                    var usuarioConMismoCuit = usuarioRepository.findByCuit(nuevoCuit);
                    if (usuarioConMismoCuit.isPresent() && !usuarioConMismoCuit.get().getId().equals(id)) {
                        Map<String, String> error = new HashMap<>();
                        error.put("error", "El CUIT ya está registrado por otro usuario");
                        return ResponseEntity.badRequest().body(error);
                    }
                }
                usuario.setCuit(nuevoCuit);
            }
            
            // NO permitir cambiar el email (es crítico para el login)
            // El email se mantiene sin cambios
            
            Usuario usuarioActualizado = usuarioService.actualizar(usuario);
            return ResponseEntity.ok(UsuarioDTO.fromEntity(usuarioActualizado));
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error al actualizar el usuario: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable Long id) {
        try {
            usuarioService.eliminar(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
