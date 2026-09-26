package com.bikego.web.dto;

import java.util.List;

public record BusquedaDto(
        double latitud,
        double longitud,
        int radioMetros,
        int radioMaximoMetros,
        List<BicicletaDto> bicicletas,
        String mensaje,
        boolean puedeAmpliar) { }
