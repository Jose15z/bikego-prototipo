package com.bikego.web.dto;
import jakarta.validation.constraints.*;
public record MetodoPagoRequest(
        @NotBlank @Pattern(regexp="^[0-9]{13,19}$",message="Escribe entre 13 y 19 dígitos de la tarjeta") String numeroTarjeta,
        @NotBlank @Pattern(regexp="^(0[1-9]|1[0-2])/\\d{2}$",message="Escribe el vencimiento como MM/AA o MM/AAAA") String vencimiento,
        @NotBlank @Size(max=100) String titular) {
    public MetodoPagoRequest {
        if(numeroTarjeta!=null) numeroTarjeta=numeroTarjeta.replaceAll("[\\s\\p{Zs}-]", "");
        if(titular!=null) titular=titular.trim();
        if(vencimiento!=null) {
            var m=java.util.regex.Pattern.compile("^(0?[1-9]|1[0-2])\\s*/\\s*(20\\d{2}|\\d{2})$").matcher(vencimiento.trim());
            if(m.matches()) vencimiento=String.format("%02d/%s",Integer.parseInt(m.group(1)),m.group(2).substring(m.group(2).length()-2));
        }
    }
}
