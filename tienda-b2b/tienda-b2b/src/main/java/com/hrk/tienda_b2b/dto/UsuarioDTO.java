package com.hrk.tienda_b2b.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioDTO {
    private Long id;
    private String nombreRazonSocial;
    private String email;
    private String cuit;
    private String tipoUsuario;
    private String fechaCreacion;
    private Boolean activo;
    private Boolean mustChangePassword;

    // Método estático para convertir desde entidad
    public static UsuarioDTO fromEntity(com.hrk.tienda_b2b.model.Usuario usuario) {
        return UsuarioDTO.builder()
                .id(usuario.getId())
                .nombreRazonSocial(usuario.getNombreRazonSocial())
                .email(usuario.getEmail())
                .cuit(usuario.getCuit())
                .tipoUsuario(usuario.getTipoUsuario().toString())
                .fechaCreacion(usuario.getFechaCreacion() != null ? usuario.getFechaCreacion().toString() : null)
                .activo(usuario.getActivo())
                .mustChangePassword(Boolean.TRUE.equals(usuario.getMustChangePassword()))
                .build();
    }
}