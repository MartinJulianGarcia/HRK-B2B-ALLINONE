package com.hrk.tienda_b2b.dto;

import lombok.Data;
import java.util.List;

@Data
public class CreateDevolucionRequest {
    private Long clienteId;
    private List<ItemDevolucion> items;
    
    @Data
    public static class ItemDevolucion {
        private Long pedidoId;
        private Long itemId;
        private Long varianteId;
        private Integer cantidad;
        private String motivo;
    }
}



