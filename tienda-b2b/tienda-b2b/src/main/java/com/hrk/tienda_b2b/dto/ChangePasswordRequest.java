package com.hrk.tienda_b2b.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {
    private String passwordActual;
    private String nuevaPassword;
    private String confirmarPassword;
}

