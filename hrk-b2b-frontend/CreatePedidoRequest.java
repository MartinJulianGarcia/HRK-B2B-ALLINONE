package com.hrk.tienda_b2b.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePedidoRequest {
    
    private Long clienteId;
    private UsuarioInfoDTO usuario;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsuarioInfoDTO {
        private String nombreRazonSocial;
        private String email;
    }
}
