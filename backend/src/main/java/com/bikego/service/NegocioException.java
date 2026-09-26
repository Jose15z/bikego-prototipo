package com.bikego.service;

import org.springframework.http.HttpStatus;

/** Error de negocio con un codigo estable que el frontend traduce en un mensaje especifico. */
public class NegocioException extends RuntimeException {

    private final CodigoError codigo;
    private final String accionSugerida;

    public NegocioException(CodigoError codigo, String mensaje) {
        this(codigo, mensaje, null);
    }

    public NegocioException(CodigoError codigo, String mensaje, String accionSugerida) {
        super(mensaje);
        this.codigo = codigo;
        this.accionSugerida = accionSugerida;
    }

    public CodigoError getCodigo() { return codigo; }
    public String getAccionSugerida() { return accionSugerida; }
    public HttpStatus getStatus() { return codigo.getStatus(); }
}
