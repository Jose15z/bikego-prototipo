package com.bikego.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Adaptador local. En despliegue se sustituye por un proveedor de correo sin cambiar el dominio. */
@Component
public class NotificadorVerificacionConsola implements NotificadorVerificacion {
    private static final Logger log = LoggerFactory.getLogger(NotificadorVerificacionConsola.class);

    private final BandejaLocal bandeja;
    public NotificadorVerificacionConsola(BandejaLocal bandeja) { this.bandeja=bandeja; }

    @Override
    public void enviarCodigo(String correo, String nombre, String codigo, int vigenciaMinutos) {
        bandeja.entregar(correo,"CORREO","Verifica tu cuenta BikeGo","Hola "+nombre+". Tu código es "+codigo+". Caduca en "+vigenciaMinutos+" minutos.");
        log.info("Código de verificación para {} <{}>: {} (vigente {} minutos)",
                nombre, correo, codigo, vigenciaMinutos);
    }
}
