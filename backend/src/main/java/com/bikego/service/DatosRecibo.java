package com.bikego.service;

import com.bikego.domain.Viaje;
import java.time.LocalDateTime;

/** Datos del recibo capturados dentro de la transacción, sin formato HTTP. */
public record DatosRecibo(long idViaje, String bicicleta, LocalDateTime fecha,
    long minutos, long tarifaBase, long tarifaMinuto, long recargo, long total,
    String estadoPago, boolean conIncidente) {

    public static DatosRecibo de(Viaje viaje) {
        return new DatosRecibo(viaje.getIdViaje(), viaje.getBicicleta().getId(), viaje.getHoraFin(),
            viaje.getMinutos(), viaje.getTarifaBaseAplicada(), viaje.getTarifaMinutoAplicada(),
            viaje.getRecargo(), viaje.getTotal(), viaje.getEstadoPago(), viaje.isConIncidente());
    }
}
