package com.hrk.tienda_b2b.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TemporadaResponseDTO {
    private Long id;
    private String nombre;
    private List<Long> productoIds;
    private boolean activa;
}


