package com.bikego.integration;

public enum ResultadoPago {
    AUTORIZADO(null),
    VENCIDA("La tarjeta registrada está vencida"),
    FONDOS_INSUFICIENTES("Fondos insuficientes en el método de pago"),
    RECHAZADA("La pasarela rechazó el método de pago");

    private final String motivo;

    ResultadoPago(String motivo) { this.motivo = motivo; }

    public String getMotivo() { return motivo; }

    public boolean autorizado() { return this == AUTORIZADO; }
}
