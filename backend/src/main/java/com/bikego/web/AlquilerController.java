package com.bikego.web;

import com.bikego.service.AlquilerService;
import com.bikego.web.dto.IniciarAlquilerRequest;
import com.bikego.web.dto.ViajeDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** F-02 Iniciar alquiler de bicicleta mediante codigo QR. */
@RestController
@RequestMapping("/api")
public class AlquilerController {

    private final AlquilerService alquileres;

    public AlquilerController(AlquilerService alquileres) {
        this.alquileres = alquileres;
    }

    @PostMapping("/alquileres")
    @ResponseStatus(HttpStatus.CREATED)
    public ViajeDto iniciar(@Valid @RequestBody IniciarAlquilerRequest req) {
        return alquileres.iniciar(req.usuarioId(), req.bicicletaSeleccionadaId(), req.codigoQr());
    }

    /** SWR-02.8: pantalla Viaje en curso. */
    @GetMapping("/viajes/{id}")
    public ViajeDto viaje(@PathVariable Long id) {
        return alquileres.consultarViaje(id);
    }
}
