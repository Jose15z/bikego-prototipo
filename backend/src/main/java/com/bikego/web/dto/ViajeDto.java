package com.bikego.web.dto;

import java.time.LocalDateTime;

/** SWR-02.6 y SWR-02.8: datos del viaje en curso. */
public record ViajeDto(
        Long idViaje,
        String idUsuario,
        String idBicicleta,
        LocalDateTime horaInicio,
        String estado,
        long segundosTranscurridos,
        double distanciaKm,
        long costoAcumulado,
        int tarifaBase,
        int tarifaMinuto,
        double velocidadPromedioKmh) { }
