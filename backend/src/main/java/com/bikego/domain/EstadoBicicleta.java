package com.bikego.domain;

public enum EstadoBicicleta {
    DISPONIBLE("Disponible"),
    EN_USO("En uso"),
    EN_MANTENIMIENTO("En mantenimiento");

    private final String etiqueta;

    EstadoBicicleta(String etiqueta) { this.etiqueta = etiqueta; }

    public String getEtiqueta() { return etiqueta; }
}
