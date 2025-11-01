package com.hrk.tienda_b2b.model.converter;

import com.hrk.tienda_b2b.model.MetodoPago;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class MetodoPagoConverter implements AttributeConverter<MetodoPago, String> {

    @Override
    public String convertToDatabaseColumn(MetodoPago metodoPago) {
        if (metodoPago == null) {
            return null;
        }
        return metodoPago.name();
    }

    @Override
    public MetodoPago convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        
        // Intentar usar el método fromString que maneja diferentes formatos
        MetodoPago metodo = MetodoPago.fromString(dbData);
        if (metodo != null) {
            return metodo;
        }
        
        // Si fromString no funciona, intentar directamente con el nombre del enum
        try {
            return MetodoPago.valueOf(dbData.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            // Si no coincide ningún valor, retornar null en lugar de lanzar excepción
            System.out.println("⚠️ [METODO_PAGO_CONVERTER] Valor desconocido en BD: '" + dbData + "', retornando null");
            return null;
        }
    }
}

