package com.hrk.tienda_b2b.model;

public enum MetodoPago {
    EFECTIVO("Efectivo"),
    TRANSFERENCIA("Transferencia"),
    CHEQUE("Cheque"),
    MERCADOPAGO("MercadoPago");

    private final String descripcion;

    MetodoPago(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    // Método para convertir String a enum (case-insensitive)
    public static MetodoPago fromString(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return null;
        }
        
        String valorUpper = valor.toUpperCase().trim();
        
        // Manejar diferentes formatos
        switch (valorUpper) {
            case "EFECTIVO":
                return EFECTIVO;
            case "TRANSFERENCIA":
                return TRANSFERENCIA;
            case "CHEQUE":
                return CHEQUE;
            case "MERCADOPAGO":
            case "MERCADO_PAGO":
            case "MERCADO PAGO":
                return MERCADOPAGO;
            default:
                // Si no coincide, intentar por descripción
                for (MetodoPago metodo : MetodoPago.values()) {
                    if (metodo.getDescripcion().equalsIgnoreCase(valor)) {
                        return metodo;
                    }
                }
                return null;
        }
    }
}

