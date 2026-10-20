package com.bikego;

import com.bikego.config.DataSeeder;
import com.bikego.domain.EstadoBicicleta;
import com.bikego.domain.EstadoViaje;
import com.bikego.repository.BicicletaRepository;
import com.bikego.repository.RechazoPagoRepository;
import com.bikego.repository.UsuarioRepository;
import com.bikego.repository.ViajeRepository;
import com.bikego.service.AlquilerService;
import com.bikego.service.CodigoError;
import com.bikego.service.DisponibilidadService;
import com.bikego.service.NegocioException;
import com.bikego.service.UsuarioService;
import com.bikego.web.dto.MetodoPagoRequest;
import com.bikego.web.dto.ViajeDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.ConcurrencyFailureException;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de F-02 alineadas con los criterios de aceptacion CA-1 a CA-7 de PBI-01 (BG-1). */
@SpringBootTest(properties = {TestDb.URL, "bikego.iot-timeout-ms=200", "bikego.bloqueo-pago-minutos=30"})
class AlquilerServiceTest {

    @Autowired AlquilerService alquiler;
    @Autowired UsuarioService usuarioService;
    @Autowired DisponibilidadService disponibilidad;
    @Autowired DataSeeder seeder;
    @Autowired BicicletaRepository bicicletas;
    @Autowired ViajeRepository viajes;
    @Autowired UsuarioRepository usuarios;
    @Autowired RechazoPagoRepository rechazos;

    @BeforeEach
    void reiniciar() { seeder.cargar(); }

    private CodigoError codigoDe(Runnable r) {
        try {
            r.run();
            return null;
        } catch (NegocioException e) {
            return e.getCodigo();
        }
    }

    @Test
    @DisplayName("CA-1 / SWR-02.6 camino feliz: desbloquea, crea viaje En curso y la bici pasa a En uso")
    void caminoFeliz() {
        ViajeDto v = alquiler.iniciar("U-001", "BG-001", "BIKEGO:BG-001");
        assertThat(v.estado()).isEqualTo("EN_CURSO");
        assertThat(v.idBicicleta()).isEqualTo("BG-001");
        assertThat(v.horaInicio()).isNotNull();
        assertThat(bicicletas.findById("BG-001").orElseThrow().getEstado()).isEqualTo(EstadoBicicleta.EN_USO);
        assertThat(viajes.existsByUsuarioIdAndEstado("U-001", EstadoViaje.EN_CURSO)).isTrue();
    }

    @Test
    @DisplayName("CA-7 tras el desbloqueo la bicicleta ya no aparece en el mapa")
    void desapareceDelMapa() {
        alquiler.iniciar("U-001", "BG-001", "BIKEGO:BG-001");
        assertThat(disponibilidad.buscarCercanas(4.5325, -75.6736, 1000).bicicletas())
                .noneMatch(b -> b.id().equals("BG-001"));
    }

    @Test
    @DisplayName("CA-4 / SWR-02.1 QR ilegible o ajeno se rechaza; el ID manual si funciona")
    void qr() {
        assertThat(codigoDe(() -> alquiler.iniciar("U-001", "BG-001", "https://otra-app.com/xyz")))
                .isEqualTo(CodigoError.QR_INVALIDO);
        assertThat(codigoDe(() -> alquiler.iniciar("U-001", null, "BIKEGO:BG-999")))
                .isEqualTo(CodigoError.QR_INVALIDO);
        assertThat(alquiler.iniciar("U-001", "BG-004", "bg-004").idBicicleta()).isEqualTo("BG-004");
    }

    @Test
    @DisplayName("CA-5 / SWR-02.2 bici distinta a la seleccionada, en uso o en mantenimiento se rechaza")
    void validaBicicleta() {
        assertThat(codigoDe(() -> alquiler.iniciar("U-001", "BG-001", "BIKEGO:BG-004")))
                .isEqualTo(CodigoError.BICICLETA_NO_COINCIDE);
        assertThat(codigoDe(() -> alquiler.iniciar("U-001", "BG-003", "BIKEGO:BG-003")))
                .isEqualTo(CodigoError.BICICLETA_EN_MANTENIMIENTO);
        assertThat(codigoDe(() -> alquiler.iniciar("U-001", "BG-005", "BIKEGO:BG-005")))
                .isEqualTo(CodigoError.BICICLETA_EN_USO);
        assertThat(codigoDe(() -> alquiler.iniciar("U-001", "BG-002", "BIKEGO:BG-002")))
                .isEqualTo(CodigoError.BATERIA_INSUFICIENTE);
    }

    @Test
    @DisplayName("CA-2 / SWR-02.3 cuenta con deuda, suspendida o con viaje activo se rechaza")
    void validaCuenta() {
        assertThat(codigoDe(() -> alquiler.iniciar("U-002", "BG-001", "BIKEGO:BG-001")))
                .isEqualTo(CodigoError.DEUDA_PENDIENTE);
        assertThat(codigoDe(() -> alquiler.iniciar("U-004", "BG-001", "BIKEGO:BG-001")))
                .isEqualTo(CodigoError.CUENTA_SUSPENDIDA);
        alquiler.iniciar("U-001", "BG-001", "BIKEGO:BG-001");
        assertThat(codigoDe(() -> alquiler.iniciar("U-001", "BG-004", "BIKEGO:BG-004")))
                .isEqualTo(CodigoError.VIAJE_EN_CURSO);
    }

    @Test
    @DisplayName("CA-3 / SWR-02.4 / SWR-02.7 pago rechazado: motivo, accion sugerida, registro y bloqueo")
    void pagoRechazado() {
        assertThatThrownBy(() -> alquiler.iniciar("U-003", "BG-001", "BIKEGO:BG-001"))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("vencida")
                .extracting("accionSugerida").isEqualTo("ACTUALIZAR_METODO_PAGO");
        assertThat(rechazos.findByUsuarioId("U-003")).hasSize(1);
        assertThat(usuarios.findById("U-003").orElseThrow().getBloqueadoHasta()).isNotNull();
        // Mientras dure el bloqueo no puede alquilar
        assertThat(codigoDe(() -> alquiler.iniciar("U-003", "BG-001", "BIKEGO:BG-001")))
                .isEqualTo(CodigoError.USUARIO_BLOQUEADO);
        // La bicicleta no se toco
        assertThat(bicicletas.findById("BG-001").orElseThrow().getEstado()).isEqualTo(EstadoBicicleta.DISPONIBLE);
    }

    @Test
    @DisplayName("SWR-02.4 flujo alterno 1: actualizar el metodo de pago permite continuar el alquiler")
    void actualizarMetodoPago() {
        codigoDe(() -> alquiler.iniciar("U-003", "BG-001", "BIKEGO:BG-001"));
        usuarioService.actualizarMetodoPago("U-003", new MetodoPagoRequest("4111 1111 1111 1111", "12/29", "Valentina Ríos"));
        assertThat(usuarios.findById("U-003").orElseThrow().getUltimos4()).isEqualTo("1111");
        assertThat(usuarios.findById("U-003").orElseThrow().getTokenPago()).doesNotContain("41111111");
        assertThat(alquiler.iniciar("U-003", "BG-001", "BIKEGO:BG-001").estado()).isEqualTo("EN_CURSO");
    }

    @Test
    @DisplayName("CA-6 / SWR-02.5 candado sin respuesta: 3 intentos, no se crea viaje")
    void candadoSinRespuesta() {
        long t0 = System.currentTimeMillis();
        assertThatThrownBy(() -> alquiler.iniciar("U-001", "BG-008", "BIKEGO:BG-008"))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("3 intentos")
                .extracting("codigo").isEqualTo(CodigoError.DESBLOQUEO_FALLIDO);
        assertThat(System.currentTimeMillis() - t0).isGreaterThanOrEqualTo(3 * 200L);
        assertThat(viajes.count()).isZero();
        assertThat(bicicletas.findById("BG-008").orElseThrow().getEstado()).isEqualTo(EstadoBicicleta.DISPONIBLE);
    }

    @Test
    @DisplayName("SWR-01.6 / CA-7 dos usuarios escanean la misma bicicleta: solo uno la obtiene")
    void concurrencia() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Callable<String> u1 = () -> resultadoDe(() -> alquiler.iniciar("U-001", "BG-006", "BIKEGO:BG-006"));
        Callable<String> u6 = () -> resultadoDe(() -> alquiler.iniciar("U-006", "BG-006", "BIKEGO:BG-006"));
        List<Future<String>> r = pool.invokeAll(List.of(u1, u6));
        pool.shutdown();
        List<String> resultados = List.of(r.get(0).get(), r.get(1).get());
        assertThat(resultados).containsOnlyOnce("OK");
        assertThat(resultados).containsAnyOf(CodigoError.BICICLETA_EN_USO.name(), "CONCURRENCIA");
        assertThat(viajes.count()).isEqualTo(1);
    }

    private String resultadoDe(Runnable r) {
        try {
            r.run();
            return "OK";
        } catch (NegocioException e) {
            return e.getCodigo().name();
        } catch (ConcurrencyFailureException e) {
            return "CONCURRENCIA";
        }
    }

    @Test
    @DisplayName("SWR-02.8 el viaje en curso informa tiempo, distancia y costo acumulado")
    void viajeEnCurso() {
        ViajeDto v = alquiler.iniciar("U-001", "BG-001", "BIKEGO:BG-001");
        ViajeDto consulta = alquiler.consultarViaje(v.idViaje());
        assertThat(consulta.segundosTranscurridos()).isGreaterThanOrEqualTo(0);
        assertThat(consulta.costoAcumulado()).isGreaterThanOrEqualTo(consulta.tarifaBase());
        assertThat(consulta.distanciaKm()).isGreaterThanOrEqualTo(0);
    }
}
