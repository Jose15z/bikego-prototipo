package com.bikego.web.dto;
import com.bikego.domain.Viaje;
import com.bikego.service.DatosRecibo;
import java.time.LocalDateTime;
public record ReciboDto(long idViaje, String numero, String bicicleta, LocalDateTime fecha,
    long minutos, long tarifaBase, long tarifaMinuto, long recargo, long total, String estadoPago, boolean conIncidente) {
    public static ReciboDto de(Viaje v) {
        return de(DatosRecibo.de(v));
    }

    public static ReciboDto de(DatosRecibo datos) {
        return new ReciboDto(datos.idViaje(), "BG-" + String.format("%08d", datos.idViaje()),
            datos.bicicleta(), datos.fecha(), datos.minutos(), datos.tarifaBase(), datos.tarifaMinuto(),
            datos.recargo(), datos.total(), datos.estadoPago(), datos.conIncidente());
    }
}
