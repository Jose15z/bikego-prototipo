package com.bikego.web.dto;

import jakarta.validation.constraints.NotBlank;

public record IniciarAlquilerRequest(
        @NotBlank String usuarioId,
        String bicicletaSeleccionadaId,
        @NotBlank String codigoQr) { }
