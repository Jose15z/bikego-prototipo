package com.bikego.integration;
import com.bikego.domain.Mensaje;
import com.bikego.repository.MensajeRepository;
import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class BandejaLocal {
    private final MensajeRepository mensajes;
    private final Clock clock;
    public BandejaLocal(MensajeRepository mensajes,Clock clock) {this.mensajes=mensajes;this.clock=clock;}
    // La alerta por fallo IoT debe sobrevivir al rollback del intento de cierre del viaje.
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void entregar(String destinatario,String canal,String asunto,String contenido) {
        mensajes.save(new Mensaje(destinatario,canal,asunto,contenido,LocalDateTime.now(clock)));
    }
}
