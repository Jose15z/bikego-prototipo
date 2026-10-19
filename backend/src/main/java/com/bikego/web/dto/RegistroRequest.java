package com.bikego.web.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegistroRequest(
        @NotBlank @Size(min = 3, max = 100) String nombre,
        @NotBlank @Pattern(regexp = "[0-9A-Za-z.-]{5,20}") String documento,
        @NotBlank @Email String correo,
        @NotBlank @Pattern(regexp = "\\+?[0-9 ]{7,16}") String telefono,
        @NotNull @Past LocalDate fechaNacimiento,
        @NotBlank @Size(max = 160) String direccion,
        @NotBlank @Size(min = 10, max = 128) String password,
        @AssertTrue boolean aceptaTerminos) { }
