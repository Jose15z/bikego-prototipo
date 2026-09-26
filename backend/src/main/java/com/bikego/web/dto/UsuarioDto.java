package com.bikego.web.dto;

import com.bikego.domain.Usuario;

import java.time.LocalDateTime;

public record UsuarioDto(
        String id,
        String nombre,
        String estadoCuenta,
        int deudaPendiente,
        String metodoPago,
        LocalDateTime bloqueadoHasta,
        String escenario) {

    public static UsuarioDto de(Usuario u, LocalDateTime ahora) {
        String metodo = u.tieneMetodoPago() ? u.getMarcaTarjeta() + " terminada en " + u.getUltimos4() : null;
        return new UsuarioDto(u.getId(), u.getNombre(), u.getEstadoCuenta().name(), u.getDeudaPendiente(),
                metodo, u.estaBloqueado(ahora) ? u.getBloqueadoHasta() : null, u.getEscenario());
    }
}
