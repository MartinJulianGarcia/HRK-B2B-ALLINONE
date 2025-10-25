package com.hrk.tienda_b2b.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePedidoRequest {
    private Long clienteId;
    private String metodoPago; // ⭐ NUEVO: Método de pago
    private UsuarioInfoDTO usuario;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsuarioInfoDTO {
        private String nombreRazonSocial;
        private String email;
    }
}