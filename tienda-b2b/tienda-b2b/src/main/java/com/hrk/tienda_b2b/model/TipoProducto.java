package com.hrk.tienda_b2b.model;

public enum TipoProducto {
    REMERA("remera.jpg"),
    MUSCULOSA("musculosa.jpg"),
    PANTALON("pantalon.jpg"),
    SHORT("short.jpg"),
    CARDIGAN("cardigan.jpg"),
    RUANA("ruana.jpg"),
    SWEATER("sweater.jpg"),
    BUFANDA("bufanda.jpg"),
    GORRO("gorro.jpg"),
    TAPADO("tapado.jpg"),
    CAPA("capa.jpg"),
    SACO("saco.jpg"),
    BLUSA("blusa.jpg"),
    MONO("mono.jpg"),
    VESTIDO("vestido.jpg"),
    BUZO("buzo.jpg"),
    MITONES("mitones.jpg"),
    CAMPERA("campera.jpg"),
    TOP("top.jpg"),
    CHALECO("chaleco.jpg");

    private final String imagenDefault;

    // Constructor del enum
    TipoProducto(String imagenDefault) {
        this.imagenDefault = imagenDefault;
    }

    // Método que necesitas
    public String getImagenDefault() {
        return "/images/categories/" + imagenDefault;
    }
}