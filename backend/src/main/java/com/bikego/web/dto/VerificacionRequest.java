package com.bikego.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerificacionRequest(@NotBlank @Email String correo,
                                  @NotBlank @Pattern(regexp = "[0-9]{6}") String codigo) { }
