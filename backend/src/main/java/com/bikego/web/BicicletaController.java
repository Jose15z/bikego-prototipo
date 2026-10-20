package com.bikego.web;

import com.bikego.repository.EstacionRepository;
import com.bikego.service.DisponibilidadService;
import com.bikego.web.dto.BicicletaDto;
import com.bikego.web.dto.BusquedaDto;
import com.bikego.web.dto.EstacionDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** F-01 Consultar bicicletas disponibles cercanas. */
@RestController
@RequestMapping("/api")
public class BicicletaController {

    private final DisponibilidadService disponibilidad;
    private final EstacionRepository estaciones;

    public BicicletaController(DisponibilidadService disponibilidad, EstacionRepository estaciones) {
        this.disponibilidad = disponibilidad;
        this.estaciones = estaciones;
    }

    /** SWR-01.1: estaciones para elegir un punto de busqueda cuando se niega el permiso de ubicacion. */
    @GetMapping("/estaciones")
    public List<EstacionDto> estaciones() {
        return estaciones.findAll().stream()
                .map(e -> new EstacionDto(e.getId(), e.getNombre(), e.getLatitud(), e.getLongitud(), e.getReferencia()))
                .toList();
    }

    /** SWR-01.2, SWR-01.3, SWR-01.4, SWR-01.5. */
    @GetMapping("/bicicletas/cercanas")
    public BusquedaDto cercanas(@RequestParam double lat, @RequestParam double lng,
                                @RequestParam(required = false) Integer radio) {
        return disponibilidad.buscarCercanas(lat, lng, radio);
    }

    /** SWR-01.6. */
    @GetMapping("/bicicletas/{id}")
    public BicicletaDto detalle(@PathVariable String id,
                                @RequestParam(required = false) Double lat,
                                @RequestParam(required = false) Double lng) {
        return disponibilidad.detalleDisponible(id.toUpperCase(), lat, lng);
    }
}
