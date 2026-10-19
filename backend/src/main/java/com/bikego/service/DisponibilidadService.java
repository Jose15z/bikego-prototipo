package com.bikego.service;

import com.bikego.config.BikeGoProperties;
import com.bikego.domain.Bicicleta;
import com.bikego.domain.EstadoBicicleta;
import com.bikego.repository.BicicletaRepository;
import com.bikego.web.dto.BicicletaDto;
import com.bikego.web.dto.BusquedaDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/** F-01 Consultar bicicletas disponibles cercanas. */
@Service
public class DisponibilidadService {

    private final BicicletaRepository bicicletas;
    private final BikeGoProperties props;

    public DisponibilidadService(BicicletaRepository bicicletas, BikeGoProperties props) {
        this.bicicletas = bicicletas;
        this.props = props;
    }

    /** SWR-01.2, SWR-01.3, SWR-01.5. */
    @Transactional(readOnly = true)
    public BusquedaDto buscarCercanas(double lat, double lng, Integer radioSolicitado) {
        if (!Double.isFinite(lat) || !Double.isFinite(lng) || Math.abs(lat)>90 || Math.abs(lng)>180) throw new NegocioException(CodigoError.DATOS_INVALIDOS,"Coordenadas inválidas");
        int radio = radioSolicitado == null ? props.getRadioInicialMetros() : radioSolicitado;
        if (radio <= 0 || radio > props.getRadioMaximoMetros()) {
            throw new NegocioException(CodigoError.RADIO_INVALIDO,
                    "El radio debe estar entre 1 y " + props.getRadioMaximoMetros() + " metros");
        }

        List<BicicletaDto> resultado = bicicletas
                .findByEstadoAndBateriaGreaterThanEqual(EstadoBicicleta.DISPONIBLE, props.getBateriaMinima())
                .stream()
                .map(b -> toDto(b, Geo.distanciaMetros(lat, lng, b.getLatitud(), b.getLongitud())))
                .filter(dto -> dto.distanciaMetros() <= radio)
                .sorted(Comparator.comparingDouble(BicicletaDto::distanciaMetros))
                .toList();

        boolean puedeAmpliar = radio < props.getRadioMaximoMetros();
        String mensaje = null;
        if (resultado.isEmpty()) {
            mensaje = "No hay bicicletas disponibles en un radio de " + formatoKm(radio) + "."
                    + (puedeAmpliar ? " Puedes ampliar la búsqueda." : "");
        }
        return new BusquedaDto(lat, lng, radio, props.getRadioMaximoMetros(), resultado, mensaje, puedeAmpliar);
    }

    /** SWR-01.6: revalida la disponibilidad en el servidor antes de mostrar el detalle. */
    @Transactional(readOnly = true)
    public BicicletaDto detalleDisponible(String id, Double lat, Double lng) {
        Bicicleta b = bicicletas.findById(id)
                .orElseThrow(() -> new NegocioException(CodigoError.BICICLETA_NO_ENCONTRADA,
                        "La bicicleta " + id + " no existe"));
        if (!b.estaDisponible(props.getBateriaMinima())) {
            throw new NegocioException(CodigoError.BICICLETA_NO_DISPONIBLE,
                    "La bicicleta " + id + " ya no está disponible", "VOLVER_AL_MAPA");
        }
        double distancia = (lat != null && lng != null)
                ? Geo.distanciaMetros(lat, lng, b.getLatitud(), b.getLongitud()) : -1;
        return toDto(b, distancia);
    }

    private BicicletaDto toDto(Bicicleta b, double distancia) {
        double autonomia = Math.round(props.getAutonomiaMaximaKm() * b.getBateria() / 100.0 * 10) / 10.0;
        return new BicicletaDto(
                b.getId(),
                b.getBateria(),
                b.getEstado().getEtiqueta(),
                b.getEstacion() != null ? b.getEstacion().getNombre() : "Ubicación libre",
                b.getLatitud(),
                b.getLongitud(),
                Math.round(distancia),
                autonomia);
    }

    private static String formatoKm(int metros) {
        return metros % 1000 == 0 ? (metros / 1000) + " km" : String.format("%.1f km", metros / 1000.0);
    }
}
