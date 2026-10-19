package com.bikego.web.dto;

import com.bikego.domain.Usuario;

import java.time.LocalDateTime;

public record UsuarioDto(
        String id,
        String nombre,
        String correo,
        String estadoCuenta,
        int deudaPendiente,
        String metodoPago,
        LocalDateTime bloqueadoHasta,
        String escenario, String rol) {

    public static UsuarioDto de(Usuario u, LocalDateTime ahora) {
        String metodo = u.tieneMetodoPago() ? u.getMarcaTarjeta() + " terminada en " + u.getUltimos4() : null;
        return new UsuarioDto(u.getId(), u.getNombre(), u.getCorreo(), u.getEstadoCuenta().name(), u.getDeudaPendiente(),
                metodo, u.estaBloqueado(ahora) ? u.getBloqueadoHasta() : null, u.getEscenario(), u.getRol());
    }
}
