package com.bikego.integration;

public interface NotificadorVerificacion {
    void enviarCodigo(String correo, String nombre, String codigo, int vigenciaMinutos);
}
