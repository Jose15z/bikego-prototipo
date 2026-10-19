package com.bikego.web.dto;

import jakarta.validation.constraints.NotBlank;

public record IniciarAlquilerRequest(String bicicletaSeleccionadaId, @NotBlank String codigoQr) { }
