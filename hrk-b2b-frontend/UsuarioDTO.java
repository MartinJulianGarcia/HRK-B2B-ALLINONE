package com.hrk.tienda_b2b.dto;

import com.hrk.tienda_b2b.model.Usuario;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioDTO {
    
    private Long id;
    private String nombreRazonSocial;
    private String email;
    private String tipoUsuario;
    
    public static UsuarioDTO fromEntity(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        
        UsuarioDTO dto = new UsuarioDTO();
        dto.setId(usuario.getId());
        dto.setNombreRazonSocial(usuario.getNombreRazonSocial());
        dto.setEmail(usuario.getEmail());
        dto.setTipoUsuario(usuario.getTipoUsuario() != null ? usuario.getTipoUsuario().toString() : null);
        
        return dto;
    }
}
