package com.hrk.tienda_b2b.controller;

import com.hrk.tienda_b2b.dto.UsuarioDTO;
import com.hrk.tienda_b2b.model.Usuario;
import com.hrk.tienda_b2b.model.TipoUsuario;
import com.hrk.tienda_b2b.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class UsuarioController {
    
    private final UsuarioService usuarioService;

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
    public ResponseEntity<UsuarioDTO> actualizarUsuario(@PathVariable Long id, @RequestBody UsuarioDTO usuarioDTO) {
        try {
            Usuario usuario = usuarioService.obtenerPorId(id)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
            
            // Actualizar solo los campos permitidos
            usuario.setNombreRazonSocial(usuarioDTO.getNombreRazonSocial());
            usuario.setEmail(usuarioDTO.getEmail());
            
            Usuario usuarioActualizado = usuarioService.actualizar(usuario);
            return ResponseEntity.ok(UsuarioDTO.fromEntity(usuarioActualizado));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
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
