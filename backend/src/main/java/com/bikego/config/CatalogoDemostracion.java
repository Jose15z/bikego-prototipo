package com.bikego.config;

import com.bikego.domain.*;
import com.bikego.repository.*;
import com.bikego.service.Geo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/** Catálogo versionado y migración no destructiva de las estaciones de demostración. */
@Service
public class CatalogoDemostracion {
    public record Punto(double latitud, double longitud) { }
    public record EstacionSemilla(String id, String nombre, double latitud, double longitud,
                                  String osm, Punto coordenadasAnteriores, String referencia) { }
    public record BiciSemilla(String id, String estacionId, EstadoBicicleta estado, int bateria,
                             double deltaLatitud, double deltaLongitud, boolean candadoConFalla) { }
    public record Catalogo(String version, String fechaConsulta, String nota,
                           List<EstacionSemilla> estaciones, List<BiciSemilla> bicicletas) { }

    private final Catalogo catalogo;
    private final EstacionRepository estaciones;
    private final BicicletaRepository bicicletas;
    private final ViajeRepository viajes;

    public CatalogoDemostracion(ObjectMapper mapper, EstacionRepository estaciones,
                               BicicletaRepository bicicletas, ViajeRepository viajes) throws IOException {
        try (var stream = new ClassPathResource("demo/catalogo.json").getInputStream()) {
            catalogo = mapper.readValue(stream, Catalogo.class);
        }
        this.estaciones = estaciones;
        this.bicicletas = bicicletas;
        this.viajes = viajes;
    }

    public List<EstacionSemilla> estacionesIniciales() { return catalogo.estaciones(); }

    @Transactional
    public void sincronizar() {
        var activas = viajes.findByEstado(EstadoViaje.EN_CURSO).stream()
                .map(v -> v.getBicicleta().getId()).collect(Collectors.toSet());
        var flota = bicicletas.findAll();
        Map<String, Estacion> ubicaciones = new HashMap<>();
        for (var dato : catalogo.estaciones()) {
            Estacion estacion = estaciones.findById(dato.id()).orElseGet(() ->
                    new Estacion(dato.id(), dato.nombre(), dato.latitud(), dato.longitud()));
            double latAnterior = estacion.getLatitud(), lngAnterior = estacion.getLongitud();
            boolean cambio = Geo.distanciaMetros(latAnterior, lngAnterior, dato.latitud(), dato.longitud()) > 0.1;
            if (cambio) {
                // Mover únicamente bicicletas estacionadas junto al punto anterior.
                // No alterar viajes activos, devoluciones lejanas ni datos históricos.
                for (Bicicleta b : flota) {
                    if (b.getEstacion() != null && dato.id().equals(b.getEstacion().getId())
                            && b.getEstado() != EstadoBicicleta.EN_USO && !activas.contains(b.getId())
                            && Geo.distanciaMetros(latAnterior, lngAnterior, b.getLatitud(), b.getLongitud()) <= 100) {
                        double distancia = Geo.distanciaMetros(latAnterior, lngAnterior, b.getLatitud(), b.getLongitud());
                        double escala = distancia > 8 ? 8 / distancia : 1;
                        b.reubicar(dato.latitud() + (b.getLatitud() - latAnterior) * escala,
                                dato.longitud() + (b.getLongitud() - lngAnterior) * escala);
                    }
                }
            }
            estacion.reubicar(dato.nombre(), dato.latitud(), dato.longitud());
            estacion.referencia(dato.referencia());
            ubicaciones.put(dato.id(), estaciones.save(estacion));
        }
        for (var dato : catalogo.bicicletas()) {
            if (!bicicletas.existsById(dato.id())) {
                Estacion estacion = ubicaciones.get(dato.estacionId());
                bicicletas.save(new Bicicleta(dato.id(), dato.estado(), dato.bateria(), estacion,
                        estacion.getLatitud() + dato.deltaLatitud(),
                        estacion.getLongitud() + dato.deltaLongitud(), dato.candadoConFalla()));
            }
        }
    }
}
