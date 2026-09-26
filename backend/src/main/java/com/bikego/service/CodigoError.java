package com.bikego.service;

import org.springframework.http.HttpStatus;

public enum CodigoError {
    // F-01
    BICICLETA_NO_ENCONTRADA(HttpStatus.NOT_FOUND),
    BICICLETA_NO_DISPONIBLE(HttpStatus.CONFLICT),
    RADIO_INVALIDO(HttpStatus.BAD_REQUEST),
    // F-02 SWR-02.1
    QR_INVALIDO(HttpStatus.UNPROCESSABLE_ENTITY),
    // F-02 SWR-02.2
    BICICLETA_NO_COINCIDE(HttpStatus.CONFLICT),
    BICICLETA_EN_USO(HttpStatus.CONFLICT),
    BICICLETA_EN_MANTENIMIENTO(HttpStatus.CONFLICT),
    BATERIA_INSUFICIENTE(HttpStatus.CONFLICT),
    // F-02 SWR-02.3
    USUARIO_NO_ENCONTRADO(HttpStatus.NOT_FOUND),
    CUENTA_SUSPENDIDA(HttpStatus.FORBIDDEN),
    DEUDA_PENDIENTE(HttpStatus.PAYMENT_REQUIRED),
    VIAJE_EN_CURSO(HttpStatus.CONFLICT),
    USUARIO_BLOQUEADO(HttpStatus.FORBIDDEN),
    SIN_METODO_PAGO(HttpStatus.PAYMENT_REQUIRED),
    // F-02 SWR-02.4 / SWR-02.7
    PAGO_RECHAZADO(HttpStatus.PAYMENT_REQUIRED),
    // F-02 SWR-02.5
    DESBLOQUEO_FALLIDO(HttpStatus.GATEWAY_TIMEOUT),
    // otros
    VIAJE_NO_ENCONTRADO(HttpStatus.NOT_FOUND),
    DATOS_INVALIDOS(HttpStatus.BAD_REQUEST);

    private final HttpStatus status;

    CodigoError(HttpStatus status) { this.status = status; }

    public HttpStatus getStatus() { return status; }
}
