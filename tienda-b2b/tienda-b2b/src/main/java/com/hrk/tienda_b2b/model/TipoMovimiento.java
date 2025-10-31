package com.hrk.tienda_b2b.model;

// com.hrk.tienda_b2b.model.TipoMovimiento
public enum TipoMovimiento {

    BAJA_POR_PEDIDO,
    REVERSION_POR_ANULACION,

    DEVOLUCION_ENTRADA,        // suma stock por devolución
    DESPERFECTO_SCRAP,         // baja definitiva si no es apto venta
    
    AJUSTE_INVENTARIO_POSITIVO,  // ajuste manual: entrada de stock
    AJUSTE_INVENTARIO_NEGATIVO   // ajuste manual: salida de stock

     }