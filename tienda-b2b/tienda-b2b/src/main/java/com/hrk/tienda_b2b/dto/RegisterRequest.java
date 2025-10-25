package com.hrk.tienda_b2b.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RegisterRequest {
    private String nombreRazonSocial;
    private String cuit;
    private String email;
    private String password;
}