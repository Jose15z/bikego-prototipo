package com.bikego.web.dto;
import jakarta.validation.constraints.*;
public record OperacionRequest(
    @NotBlank String bicicletaId,
    @NotBlank String tipo,
    @NotBlank @Size(min=10, max=2000) String descripcion,
    @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitud,
    @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitud) {}
