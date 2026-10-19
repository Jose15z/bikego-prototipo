package com.bikego.web.dto;
import jakarta.validation.constraints.*;
public record PosicionRequest(
    @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitud,
    @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitud,
    @NotNull @DecimalMin("0") @DecimalMax("30") Double precision) {}
