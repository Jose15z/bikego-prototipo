package com.bikego.web.dto;

/** SWR-01.3: ID, bateria, estacion, distancia y autonomia estimada. */
public record BicicletaDto(
        String id,
        int bateria,
        String estado,
        String estacion,
        double latitud,
        double longitud,
        long distanciaMetros,
        double autonomiaKm) { }
