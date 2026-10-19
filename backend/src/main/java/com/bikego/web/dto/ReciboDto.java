package com.bikego.web.dto;
import com.bikego.domain.Viaje;
import java.time.LocalDateTime;
public record ReciboDto(long idViaje, String numero, String bicicleta, LocalDateTime fecha,
    long minutos, long tarifaBase, long tarifaMinuto, long recargo, long total, String estadoPago, boolean conIncidente) {
    public static ReciboDto de(Viaje v) {
        return new ReciboDto(v.getIdViaje(), "BG-" + String.format("%08d", v.getIdViaje()), v.getBicicleta().getId(),
            v.getHoraFin(), v.getMinutos(), v.getTarifaBaseAplicada(), v.getTarifaMinutoAplicada(),
            v.getRecargo(), v.getTotal(), v.getEstadoPago(), v.isConIncidente());
    }
}
