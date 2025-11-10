package com.hrk.tienda_b2b.dto;

import lombok.Data;

import java.util.List;

@Data
public class TemporadaRequest {
    private String nombre;
    private List<Long> productoIds;
}


