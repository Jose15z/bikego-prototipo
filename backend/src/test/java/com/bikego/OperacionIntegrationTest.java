package com.bikego;

import com.bikego.config.DataSeeder;
import com.bikego.domain.*;
import com.bikego.integration.*;
import com.bikego.repository.*;
import com.bikego.service.*;
import com.bikego.web.dto.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import java.time.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={TestDb.URL,"bikego.iot-timeout-ms=20"})
@AutoConfigureMockMvc
class OperacionIntegrationTest {
    @Autowired DataSeeder seeder;
    @Autowired OperacionService operaciones;
    @Autowired AlquilerService alquiler;
    @Autowired UsuarioService usuarioService;
    @Autowired UsuarioRepository usuarios;
    @Autowired ViajeRepository viajes;
    @Autowired BicicletaRepository bicicletas;
    @Autowired EventoFlotaRepository eventos;
    @Autowired RechazoPagoRepository rechazos;
    @Autowired AutenticacionService auth;
    @Autowired MockMvc mvc;
    @MockBean Clock clock;
    @MockBean CandadoIotClient candado;
    @MockBean NotificadorOperaciones notificador;
    @SpyBean PasarelaPagoSimulada pasarela;
    private Instant instante;
    private Usuario operador;
    private final PosicionRequest plaza = new PosicionRequest(4.5325,-75.6736,5.0);

    @BeforeEach void preparar() {
        instante=Instant.parse("2026-10-17T15:00:00Z");
        when(clock.getZone()).thenReturn(ZoneId.of("America/Bogota"));
        when(clock.instant()).thenAnswer(i -> instante);
        when(candado.enviarDesbloqueo(anyString())).thenAnswer(i -> CompletableFuture.completedFuture(true));
        when(candado.enviarBloqueo(anyString())).thenAnswer(i -> CompletableFuture.completedFuture(true));
        eventos.deleteAll();seeder.cargar();
        operador=usuarios.findById("U-006").orElseThrow();operador.asignarRol("OPERADOR");usuarios.save(operador);
    }
    private long iniciar() {return alquiler.iniciar("U-001","BG-001","BIKEGO:BG-001").idViaje();}
    private void avanzar(long minutos) {instante=instante.plusSeconds(minutos*60);}
    private OperacionRequest reporte(String tipo) {return new OperacionRequest("BG-001",tipo,"El freno delantero está averiado",4.5325,-75.6736);}

    @Test void finalizaCongelaCostoYNoCobraDosVeces() {
        long id=iniciar();avanzar(10);
        var recibo=operaciones.finalizar(id,"U-001",plaza);
        assertThat(recibo.total()).isEqualTo(4000);assertThat(recibo.minutos()).isEqualTo(10);
        assertThat(bicicletas.findById("BG-001").orElseThrow().getEstado()).isEqualTo(EstadoBicicleta.DISPONIBLE);
        avanzar(60);
        assertThat(alquiler.consultarViaje(id).costoAcumulado()).isEqualTo(4000);
        assertThat(operaciones.pagar(id,"U-001").estadoPago()).isEqualTo("PROCESADO");
        assertThat(operaciones.pagar(id,"U-001").estadoPago()).isEqualTo("PROCESADO");
        verify(pasarela,times(1)).cobrar(anyString(),eq(4000L),anyString());
        verify(notificador).recibo(eq("laura@correo.com"),any());
        assertThat(operaciones.finalizar(id,"U-001",plaza).total()).isEqualTo(4000);
    }
    @Test void calculaTarifasDe45Y250Minutos() {
        long id=iniciar();avanzar(45);
        assertThat(operaciones.finalizar(id,"U-001",plaza).total()).isEqualTo(12750);
        operaciones.pagar(id,"U-001");id=iniciar();avanzar(250);
        var r=operaciones.finalizar(id,"U-001",plaza);
        assertThat(r.recargo()).isEqualTo(10000);assertThat(r.total()).isEqualTo(74000);
    }
    @Test void exactamenteCuatroHorasNoTieneRecargo() {
        long id=iniciar();avanzar(240);
        assertThat(operaciones.finalizar(id,"U-001",plaza).recargo()).isZero();
    }
    @Test void fueraDeZonaNoBloqueaNiFinaliza() {
        long id=iniciar();
        assertThatThrownBy(() -> operaciones.finalizar(id,"U-001",new PosicionRequest(4.6,-75.6,5.0)))
            .isInstanceOf(NegocioException.class).hasMessageContaining("Acércate");
        verify(candado,never()).enviarBloqueo(anyString());
        assertThat(alquiler.consultarViaje(id).estado()).isEqualTo("EN_CURSO");
    }
    @Test void toleranciaDeZonaNoExcede30MetrosDelBorde() {
        assertThat(operaciones.zonas(4.5325+49.0/111320,-75.6736).get(0).dentro()).isTrue();
        assertThat(operaciones.zonas(4.5325+51.0/111320,-75.6736).get(0).dentro()).isFalse();
    }
    @Test void fallaCandadoMantieneViajeYNotifica() {
        long id=iniciar();when(candado.enviarBloqueo(anyString())).thenAnswer(i -> new CompletableFuture<Boolean>());
        assertThatThrownBy(() -> operaciones.finalizar(id,"U-001",plaza)).isInstanceOf(NegocioException.class);
        verify(candado,times(3)).enviarBloqueo("BG-001");verify(notificador).operador(contains("No se pudo bloquear"));
        assertThat(alquiler.consultarViaje(id).estado()).isEqualTo("EN_CURSO");
    }
    @Test void rechazoRegistraDeudaUnaVezYPermiteSaldarla() {
        long id=iniciar();avanzar(10);operaciones.finalizar(id,"U-001",plaza);
        Usuario u=usuarios.findById("U-001").orElseThrow();u.actualizarMetodoPago("tok_fondos_0002","Visa","0002");usuarios.save(u);
        operaciones.pagar(id,"U-001");operaciones.pagar(id,"U-001");
        assertThat(usuarios.findById("U-001").orElseThrow().getDeudaPendiente()).isEqualTo(4000);
        assertThat(rechazos.count()).isEqualTo(2);
        usuarioService.actualizarMetodoPago("U-001",new MetodoPagoRequest("4111111111111111","12/29","Laura Gómez"));
        assertThat(operaciones.pagar(id,"U-001").estadoPago()).isEqualTo("PROCESADO");
        assertThat(usuarios.findById("U-001").orElseThrow().getDeudaPendiente()).isZero();
    }
    @Test void pasarelaSinServicioReintentaTresVecesSinAlquilar() {
        Usuario u=usuarios.findById("U-001").orElseThrow();u.actualizarMetodoPago("tok_sin_servicio","Visa","4242");usuarios.save(u);
        assertThatThrownBy(this::iniciar).isInstanceOf(NegocioException.class).hasMessageContaining("Servicio de pago no disponible");
        verify(pasarela,times(3)).verificar("tok_sin_servicio");assertThat(viajes.count()).isZero();
        verify(candado,never()).enviarDesbloqueo(anyString());
    }
    @Test void clienteNoPuedeAdministrarNiLeerViajesAjenos() throws Exception {
        long id=iniciar();String token=auth.iniciarSesion(new LoginRequest("laura@correo.com","BikeGo123!")).token();
        mvc.perform(get("/api/flota").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
        mvc.perform(post("/api/mantenimientos").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON)
            .content("{\"bicicletaId\":\"BG-004\",\"tipo\":\"Preventivo\",\"descripcion\":\"Revisión general\",\"latitud\":4.53,\"longitud\":-75.6843}"))
            .andExpect(status().isForbidden());
        assertThatThrownBy(() -> operaciones.recibo(id,"U-006")).isInstanceOf(NegocioException.class).hasMessageContaining("no encontrado");
        mvc.perform(get("/api/viajes")).andExpect(status().isUnauthorized());
    }
    @Test void validaPrecisionYCoordenadasEnApi() throws Exception {
        long id=iniciar();String token=auth.iniciarSesion(new LoginRequest("laura@correo.com","BikeGo123!")).token();
        mvc.perform(post("/api/viajes/"+id+"/finalizar").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON)
            .content("{\"latitud\":4.5325,\"longitud\":-75.6736,\"precision\":100}"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/zonas").param("lat","NaN").param("lng","0")).andExpect(status().isBadRequest());
    }
    @Test void incidenteRetiraBiciYOperadorPuedeAnularCobro() {
        long id=iniciar();var e=operaciones.crear(usuarios.findById("U-001").orElseThrow(),reporte("Daño grave"),false);
        assertThat(e.viajeId).isEqualTo(id);verify(notificador).operador(contains("Daño grave"));
        assertThat(bicicletas.findById("BG-001").orElseThrow().getEstado()).isEqualTo(EstadoBicicleta.EN_MANTENIMIENTO);
        operaciones.revisar(operador,e.id,"REVISAR");
        assertThat(operaciones.finalizar(id,"U-001",plaza).estadoPago()).isEqualTo("EN_REVISION");
        assertThat(operaciones.pagar(id,"U-001").estadoPago()).isEqualTo("EN_REVISION");
        verify(pasarela,never()).cobrar(any(),anyLong(),anyString());
        assertThat(operaciones.revisar(operador,e.id,"ANULAR").estadoPago()).isEqualTo("ANULADO");
        assertThat(bicicletas.findById("BG-001").orElseThrow().getEstado()).isEqualTo(EstadoBicicleta.EN_MANTENIMIENTO);
    }
    @Test void mantenimientoValidaBateriaYGuardaResponsable() {
        var e=operaciones.crear(operador,reporte("Preventivo"),true);
        assertThat(e.responsable).isEqualTo(operador.getNombre());
        assertThatThrownBy(() -> operaciones.cerrarMantenimiento(operador,e.id,15)).isInstanceOf(NegocioException.class);
        operaciones.cerrarMantenimiento(operador,e.id,60);
        assertThat(bicicletas.findById("BG-001").orElseThrow().getBateria()).isEqualTo(60);
        assertThat(eventos.findById(e.id).orElseThrow().estado).isEqualTo("CERRADO");
    }
    @Test void noSePuedeMantenerUnaBiciConViajeInclusoSiTieneIncidente() {
        iniciar();operaciones.crear(usuarios.findById("U-001").orElseThrow(),reporte("Daño grave"),false);
        assertThatThrownBy(() -> operaciones.crear(operador,reporte("Correctivo"),true)).isInstanceOf(NegocioException.class).hasMessageContaining("durante un viaje");
    }
    @Test void dosAlquileresSimultaneosDelMismoUsuarioSoloCreanUnViaje() throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(2);
        try {
            var r=pool.invokeAll(java.util.List.of(
                () -> {try {alquiler.iniciar("U-001","BG-001","BG-001");return true;}catch(NegocioException ex){return false;}},
                () -> {try {alquiler.iniciar("U-001","BG-004","BG-004");return true;}catch(NegocioException ex){return false;}}));
            int exitos=0;for(var f:r) if(Boolean.TRUE.equals(f.get())) exitos++;
            assertThat(exitos).isEqualTo(1);assertThat(viajes.count()).isEqualTo(1);
        } finally {pool.shutdownNow();}
    }

    @Test void tecnicoPuedeMantenerPeroNoRevisarCobros() {
        Usuario tecnico=usuarios.findById("U-006").orElseThrow();tecnico.asignarRol("TECNICO");usuarios.save(tecnico);
        var e=operaciones.crear(tecnico,reporte("Preventivo"),true);
        assertThat(operaciones.cerrarMantenimiento(tecnico,e.id,20).estado).isEqualTo("CERRADO");
        assertThatThrownBy(() -> operaciones.revisar(tecnico,e.id,"REVISAR")).extracting("codigo").isEqualTo(CodigoError.SIN_PERMISO);
    }
    @Test void deudaImpideValidacionYTarjetaRechazadaNoSeGuarda() {
        assertThatThrownBy(() -> usuarioService.validar("U-002")).extracting("codigo").isEqualTo(CodigoError.DEUDA_PENDIENTE);
        assertThatThrownBy(() -> usuarioService.actualizarMetodoPago("U-001",new MetodoPagoRequest("4000000000000002","12/29","Laura Gómez")))
            .extracting("codigo").isEqualTo(CodigoError.PAGO_RECHAZADO);
        assertThat(usuarios.findById("U-001").orElseThrow().getUltimos4()).isEqualTo("4242");
        assertThat(rechazos.count()).isEqualTo(1);
    }
    @Test void clienteSoloVeSusIncidentesYNoPuedeFinalizarViajeAjeno() {
        long id=iniciar();operaciones.crear(usuarios.findById("U-001").orElseThrow(),reporte("Otro"),false);
        Usuario otro=usuarios.findById("U-003").orElseThrow();
        assertThat(operaciones.eventos(otro)).isEmpty();
        assertThatThrownBy(() -> operaciones.finalizar(id,otro.getId(),plaza)).extracting("codigo").isEqualTo(CodigoError.VIAJE_NO_ENCONTRADO);
        assertThatThrownBy(() -> operaciones.pagar(id,otro.getId())).extracting("codigo").isEqualTo(CodigoError.VIAJE_NO_ENCONTRADO);
    }
    @Test void historialRecuperaViajeActivoDelUsuario() {
        long id=iniciar();var historial=operaciones.historial("U-001",alquiler);
        assertThat(historial).hasSize(1);assertThat(historial.get(0).idViaje()).isEqualTo(id);
        assertThat(operaciones.historial("U-003",alquiler)).isEmpty();
    }
    @Test void revisionPuedeAutorizarCobroDespuesDeFinalizar() {
        long id=iniciar();var e=operaciones.crear(usuarios.findById("U-001").orElseThrow(),reporte("Otro"),false);
        operaciones.revisar(operador,e.id,"REVISAR");operaciones.finalizar(id,"U-001",plaza);
        assertThat(operaciones.revisar(operador,e.id,"COBRAR").estadoPago()).isEqualTo("PENDIENTE");
        assertThat(operaciones.pagar(id,"U-001").estadoPago()).isEqualTo("PROCESADO");
    }
    @Test void cobrosConcurrentesSoloLlamanUnaVezALaPasarela() throws Exception {
        long id=iniciar();operaciones.finalizar(id,"U-001",plaza);
        ExecutorService pool=Executors.newFixedThreadPool(2);
        try {
            var respuestas=pool.<ReciboDto>invokeAll(java.util.List.of(() -> operaciones.pagar(id,"U-001"),() -> operaciones.pagar(id,"U-001")));
            for(var r:respuestas) assertThat(r.get().estadoPago()).isEqualTo("PROCESADO");
            verify(pasarela,times(1)).cobrar(anyString(),anyLong(),anyString());
        } finally {pool.shutdownNow();}
    }

    @Test void horaDeInicioSeRegistraDespuesDelDesbloqueo() {
        when(candado.enviarDesbloqueo(anyString())).thenAnswer(i -> {
            instante=instante.plusSeconds(8);
            return CompletableFuture.completedFuture(true);
        });
        var v=alquiler.iniciar("U-001","BG-001","BG-001");
        assertThat(v.horaInicio()).isEqualTo(LocalDateTime.ofInstant(instante,ZoneId.of("America/Bogota")));
        assertThat(v.segundosTranscurridos()).isZero();
    }
    @Test void historialDeMantenimientoEstaOrdenadoPorFechaYConResponsable() {
        var primero=operaciones.crear(operador,reporte("Preventivo"),true);
        operaciones.cerrarMantenimiento(operador,primero.id,60);
        avanzar(10);
        var segundo=operaciones.crear(operador,reporte("Correctivo"),true);
        operaciones.cerrarMantenimiento(operador,segundo.id,60);
        avanzar(10);
        var tercero=operaciones.crear(operador,reporte("Preventivo"),true);
        var lista=operaciones.eventos(operador);
        assertThat(lista).hasSize(3);
        assertThat(lista.get(0).id).isEqualTo(tercero.id);
        assertThat(lista.get(1).id).isEqualTo(segundo.id);
        assertThat(lista.get(2).id).isEqualTo(primero.id);
        assertThat(lista).allMatch(e -> operador.getNombre().equals(e.responsable));
    }
}
