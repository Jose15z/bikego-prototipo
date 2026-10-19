package com.bikego.integration;
import org.springframework.stereotype.Component;
import com.bikego.web.dto.ReciboDto;

@Component
public class NotificadorOperacionesConsola implements NotificadorOperaciones {
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(NotificadorOperacionesConsola.class);
    private final BandejaLocal bandeja;
    public NotificadorOperacionesConsola(BandejaLocal bandeja) {this.bandeja=bandeja;}
    public void operador(String mensaje) {
        bandeja.entregar("OPERADORES","ALERTA","Alerta de flota",mensaje);
        log.info("OPERADOR: {}",mensaje);
    }
    public void recibo(String correo,ReciboDto r) {
        String contenido="Recibo " + r.numero()+"\nFecha: "+r.fecha()+"\nViaje: "+r.idViaje()+"\nBicicleta: "+r.bicicleta()
            +"\nDuración: "+r.minutos()+" min\nTarifa base: $"+r.tarifaBase()+" COP\nTarifa por minuto: $"+r.tarifaMinuto()
            +" COP\nRecargo: $"+r.recargo()+" COP\nTotal: $"+r.total()+" COP\nEstado: "+r.estadoPago();
        bandeja.entregar(correo,"CORREO","Tu recibo BikeGo "+r.numero(),contenido);
        log.info("CORREO SIMULADO entregado a {}: {}",correo,r.numero());
    }
}
