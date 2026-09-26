package com.bikego.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MetodoPagoRequest(
        @NotBlank @Pattern(regexp = "^[0-9 ]{13,23}$", message = "Número de tarjeta inválido") String numeroTarjeta,
        @NotBlank @Pattern(regexp = "^(0[1-9]|1[0-2])/\\d{2}$", message = "Usa el formato MM/AA") String vencimiento,
        @NotBlank String titular) { }
