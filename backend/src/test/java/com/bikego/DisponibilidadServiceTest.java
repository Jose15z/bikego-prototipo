package com.bikego;

import com.bikego.config.DataSeeder;
import com.bikego.service.CodigoError;
import com.bikego.service.DisponibilidadService;
import com.bikego.service.NegocioException;
import com.bikego.web.dto.BicicletaDto;
import com.bikego.web.dto.BusquedaDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de F-01. Cada metodo cita el SWR que verifica. */
@SpringBootTest(properties = {TestDb.URL, "bikego.iot-timeout-ms=200"})
class DisponibilidadServiceTest {

    static final double PLAZA_LAT = 4.53389;
    static final double PLAZA_LNG = -75.68111;

    @Autowired DisponibilidadService servicio;
    @Autowired DataSeeder seeder;

    @BeforeEach
    void reiniciar() { seeder.cargar(); }

    private List<String> ids(BusquedaDto r) {
        return r.bicicletas().stream().map(BicicletaDto::id).toList();
    }

    @Test
    @DisplayName("SWR-01.2 solo aparecen bicicletas Disponibles con bateria >= 20 %")
    void filtraEstadoYBateria() {
        BusquedaDto r = servicio.buscarCercanas(PLAZA_LAT, PLAZA_LNG, 5000);
        assertThat(ids(r))
                .doesNotContain("BG-002", "BG-003", "BG-005", "BG-011", "BG-012")
                .contains("BG-001", "BG-004", "BG-010");
    }

    @Test
    @DisplayName("SWR-01.2 radio inicial de 1 km limita los resultados")
    void radioInicial() {
        BusquedaDto r = servicio.buscarCercanas(PLAZA_LAT, PLAZA_LNG, null);
        assertThat(r.radioMetros()).isEqualTo(1000);
        assertThat(r.bicicletas()).allMatch(b -> b.distanciaMetros() <= 1000);
        assertThat(ids(r)).contains("BG-001").doesNotContain("BG-006");
    }

    @Test
    @DisplayName("SWR-01.2 no se acepta un radio mayor a 5 km")
    void radioMaximo() {
        assertThatThrownBy(() -> servicio.buscarCercanas(PLAZA_LAT, PLAZA_LNG, 6000))
                .isInstanceOf(NegocioException.class)
                .extracting("codigo").isEqualTo(CodigoError.RADIO_INVALIDO);
    }

    @Test
    @DisplayName("SWR-01.3 cada bicicleta trae ID, bateria, estacion, distancia y autonomia")
    void datosDeCadaBicicleta() {
        BicicletaDto b = servicio.buscarCercanas(PLAZA_LAT, PLAZA_LNG, 1000).bicicletas().get(0);
        assertThat(b.id()).isEqualTo("BG-001");
        assertThat(b.bateria()).isEqualTo(85);
        assertThat(b.estacion()).isEqualTo("Plaza de Bolívar");
        assertThat(b.distanciaMetros()).isBetween(0L, 60L);
        assertThat(b.autonomiaKm()).isEqualTo(51.0);
    }

    @Test
    @DisplayName("SWR-01.4 el 95 % de 20 consultas responde en 3 s o menos")
    void rendimiento() {
        int rapidas = 0;
        for (int i = 0; i < 20; i++) {
            long t0 = System.nanoTime();
            servicio.buscarCercanas(PLAZA_LAT, PLAZA_LNG, 5000);
            if ((System.nanoTime() - t0) / 1_000_000 <= 3000) rapidas++;
        }
        assertThat(rapidas).isGreaterThanOrEqualTo(19);
    }

    @Test
    @DisplayName("SWR-01.5 sin bicicletas en el radio se informa y se ofrece ampliar")
    void sinResultados() {
        // Punto al norte de Armenia, lejos de todas las estaciones
        BusquedaDto r = servicio.buscarCercanas(4.5900, -75.6400, 1000);
        assertThat(r.bicicletas()).isEmpty();
        assertThat(r.mensaje()).contains("No hay bicicletas disponibles");
        assertThat(r.puedeAmpliar()).isTrue();
    }

    @Test
    @DisplayName("SWR-01.6 el detalle revalida y rechaza una bicicleta que ya no esta disponible")
    void revalidaDetalle() {
        assertThat(servicio.detalleDisponible("BG-001", PLAZA_LAT, PLAZA_LNG).id()).isEqualTo("BG-001");
        assertThatThrownBy(() -> servicio.detalleDisponible("BG-005", null, null))
                .isInstanceOf(NegocioException.class)
                .extracting("codigo").isEqualTo(CodigoError.BICICLETA_NO_DISPONIBLE);
    }
}
