package com.bikego;

import com.bikego.config.*;
import com.bikego.domain.*;
import com.bikego.repository.*;
import com.bikego.service.*;
import com.bikego.web.dto.LoginRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {TestDb.URL, "bikego.iot-timeout-ms=200"})
@Transactional
class CatalogoDemostracionTest {
    @Autowired DataSeeder seeder;
    @Autowired CatalogoDemostracion catalogo;
    @Autowired EstacionRepository estaciones;
    @Autowired BicicletaRepository bicicletas;
    @Autowired UsuarioRepository usuarios;
    @Autowired ViajeRepository viajes;
    @Autowired SesionRepository sesiones;
    @Autowired AlquilerService alquiler;
    @Autowired AutenticacionService auth;
    @BeforeEach void preparar() { seeder.cargar(); }

    @Test void creaDiezBicicletasPorEstacionConIdentificadoresUnicos() {
        assertThat(estaciones.count()).isEqualTo(catalogo.estacionesIniciales().size());
        assertThat(bicicletas.count()).isEqualTo(catalogo.estacionesIniciales().size()*10L);
        assertThat(bicicletas.findAll()).extracting(Bicicleta::getId).doesNotHaveDuplicates();
        var grupos = bicicletas.findAll().stream().collect(Collectors.groupingBy(b -> b.getEstacion().getId()));
        assertThat(grupos).hasSize(catalogo.estacionesIniciales().size());
        grupos.values().forEach(g -> assertThat(g).hasSize(10));
        assertThat(bicicletas.findById("BG-060")).isPresent();
    }

    @Test void lasEstacionesQuedanEnLosLugaresContrastados() {
        // Puntos representativos de las geometrías OSM consultadas, no los antiguos desplazados.
        assertThat(Geo.distanciaMetros(4.5325,-75.6736,estaciones.findById("EST-01").orElseThrow().getLatitud(),estaciones.findById("EST-01").orElseThrow().getLongitud())).isLessThan(1);
        assertThat(Geo.distanciaMetros(4.51535,-75.699660,estaciones.findById("EST-05").orElseThrow().getLatitud(),estaciones.findById("EST-05").orElseThrow().getLongitud())).isLessThan(1);
        for (var b : bicicletas.findAll()) {
            assertThat(Geo.distanciaMetros(b.getLatitud(),b.getLongitud(),b.getEstacion().getLatitud(),b.getEstacion().getLongitud())).isLessThan(50);
        }
        assertThat(bicicletas.findById("BG-002").orElseThrow().getBateria()).isEqualTo(15);
        assertThat(bicicletas.findById("BG-003").orElseThrow().getEstado()).isEqualTo(EstadoBicicleta.EN_MANTENIMIENTO);
        assertThat(bicicletas.findById("BG-008").orElseThrow().isCandadoConFalla()).isTrue();
    }

    @Test void ampliacionCumpleLaDistribucionSolicitada() {
        var nuevas = estaciones.findAll().stream().filter(e -> Integer.parseInt(e.getId().substring(4)) > 6 && Integer.parseInt(e.getId().substring(4)) <= 16).toList();
        assertThat(nuevas).hasSize(10);
        assertThat(nuevas.stream().filter(e -> e.getNombre().startsWith("Avenida Centenario")).count()).isEqualTo(3);
        assertThat(nuevas.stream().filter(e -> e.getNombre().startsWith("Carrera 19")).count()).isEqualTo(3);
        assertThat(nuevas.stream().filter(e -> e.getNombre().startsWith("Calle 50")).count()).isEqualTo(2);
        assertThat(nuevas.stream().filter(e -> e.getNombre().startsWith("Calle 23")).count()).isEqualTo(2);
        assertThat(estaciones.count()).isEqualTo(52);
        assertThat(bicicletas.count()).isEqualTo(520);
        for (var e : nuevas) {
            assertThat(e.getReferencia()).isNotBlank();
            var locales=bicicletas.findAll().stream().filter(b -> b.getEstacion().getId().equals(e.getId())).toList();
            assertThat(locales).hasSize(10);
            assertThat(locales).allMatch(b -> b.getEstado()==EstadoBicicleta.DISPONIBLE && b.getBateria()>=20);
        }
        assertThat(bicicletas.findById("BG-160")).isPresent();
    }

    @Test void agregaLosDiezLugaresSolicitadosConBicicletasDisponibles() {
        var nuevas = estaciones.findAll().stream().filter(e -> Integer.parseInt(e.getId().substring(4)) > 16 && Integer.parseInt(e.getId().substring(4)) <= 26).toList();
        assertThat(nuevas).extracting(Estacion::getNombre).containsExactlyInAnyOrder(
            "Granada · Carrera 23", "Colegio San José", "Coliseo del Café", "Barrio Laureles", "Portal del Quindío",
            "Museo del Oro Quimbaya", "Oro Negro", "Mercar", "C.C. Veranera Plaza", "Avenida Tigreros");
        for (var e : nuevas) {
            assertThat(e.getReferencia()).isNotBlank();
            var locales = bicicletas.findAll().stream().filter(b -> b.getEstacion().getId().equals(e.getId())).toList();
            assertThat(locales).hasSize(10);
            assertThat(locales).allMatch(b -> b.getEstado()==EstadoBicicleta.DISPONIBLE && b.getBateria()>=20);
        }
        assertThat(estaciones.count()).isEqualTo(52);
        assertThat(bicicletas.count()).isEqualTo(520);
        assertThat(bicicletas.findById("BG-260")).isPresent();
    }

    @Test void agregaAmericasEstadioSanJoseYTramoFloridaTorresDelRio() {
        var nuevas=estaciones.findAll().stream().filter(e -> Integer.parseInt(e.getId().substring(4))>26 && Integer.parseInt(e.getId().substring(4))<=29).toList();
        assertThat(nuevas).extracting(Estacion::getNombre).containsExactlyInAnyOrder("Las Américas","Estadio San José","La Florida · Torres del Río");
        for(var e:nuevas) {
            var locales=bicicletas.findAll().stream().filter(b -> b.getEstacion().getId().equals(e.getId())).toList();
            assertThat(locales).hasSize(10);
            assertThat(locales).allMatch(b -> b.getEstado()==EstadoBicicleta.DISPONIBLE && b.getBateria()>=20);
            assertThat(e.getReferencia()).isNotBlank();
        }
        assertThat(estaciones.count()).isEqualTo(52);
        assertThat(bicicletas.count()).isEqualTo(520);
        assertThat(bicicletas.findById("BG-290")).isPresent();
    }

    @Test void ampliaBarriosConDiezBicicletasDisponiblesPorNuevaEstacion() {
        var nuevas=estaciones.findAll().stream().filter(e -> Integer.parseInt(e.getId().substring(4))>=30 && Integer.parseInt(e.getId().substring(4))<=42).toList();
        assertThat(nuevas).extracting(Estacion::getNombre).containsExactlyInAnyOrder(
            "Cancha Ciudad Dorada", "Bosques de Pinares", "Puerto Espejo", "La Fachada", "La Adiela", "Barrio Santander",
            "Parque El Bosque", "La Patria", "Parque Valencia", "Los Álamos", "La Mariela", "Las Colinas", "Barrio Libertadores");
        for(var e:nuevas) {
            var locales=bicicletas.findAll().stream().filter(b -> b.getEstacion().getId().equals(e.getId())).toList();
            assertThat(locales).hasSize(10);
            assertThat(locales).allMatch(b -> b.getEstado()==EstadoBicicleta.DISPONIBLE && b.getBateria()>=75 && b.getBateria()<=100);
        }
        assertThat(bicicletas.findById("BG-420")).isPresent();
    }

    @Test void americasQuedaEnTesalonicaSeparadaDelEstadio() {
        var americas=estaciones.findById("EST-27").orElseThrow();var estadio=estaciones.findById("EST-28").orElseThrow();
        assertThat(americas.getReferencia()).contains("Tesalónica");
        assertThat(Geo.distanciaMetros(americas.getLatitud(),americas.getLongitud(),4.5430401,-75.6887367)).isLessThan(100);
        assertThat(Geo.distanciaMetros(americas.getLatitud(),americas.getLongitud(),estadio.getLatitud(),estadio.getLongitud())).isGreaterThan(400);
    }

    @Test void agregaCuatroPuntosEnLaAvenidaYUnoEnJardinesConDiezBicisCadaUno() {
        var nuevas=estaciones.findAll().stream().filter(e -> Integer.parseInt(e.getId().substring(4))>=43 && Integer.parseInt(e.getId().substring(4))<=47).toList();
        assertThat(nuevas).extracting(Estacion::getNombre).containsExactlyInAnyOrder(
            "Avenida 19 de Enero · Calle 10 Norte", "Avenida 19 de Enero · Calle 19 Norte",
            "Avenida 19 de Enero · Calle 23A Norte", "Avenida 19 de Enero · Calle 30 Norte", "Jardines de Armenia");
        for(var e:nuevas) {
            var locales=bicicletas.findAll().stream().filter(b -> b.getEstacion().getId().equals(e.getId())).toList();
            assertThat(locales).hasSize(10);
            assertThat(locales).allMatch(b -> b.getEstado()==EstadoBicicleta.DISPONIBLE && b.getBateria()>=75);
        }
        var jardines=estaciones.findById("EST-47").orElseThrow();
        assertThat(Geo.distanciaMetros(jardines.getLatitud(),jardines.getLongitud(),4.5072408,-75.6906614)).isLessThan(30);
        assertThat(bicicletas.findById("BG-470")).isPresent();
    }

    @Test void corredorSurConDiezBicicletasPorAcceso() {
        var nuevas=estaciones.findAll().stream().filter(e->Integer.parseInt(e.getId().substring(4))>=48).toList();
        assertThat(nuevas).extracting(Estacion::getNombre).containsExactlyInAnyOrder("Aeropuerto El Edén", "Mall Paraíso · Club Campestre", "Soledén", "Universidad La Gran Colombia · Sede campestre", "Parque de Recreación Popular");
        for(var e:nuevas)assertThat(bicicletas.findAll().stream().filter(b->b.getEstacion().getId().equals(e.getId())).count()).isEqualTo(10);
        assertThat(bicicletas.findById("BG-520")).isPresent();
    }

    private void simularCatalogoAnterior() {
        bicicletas.deleteAll(bicicletas.findAll().stream().filter(b -> Integer.parseInt(b.getId().substring(3))>12).toList());
        for (var e : catalogo.estacionesIniciales()) {
            Estacion s = estaciones.findById(e.id()).orElseThrow();
            double dLat=e.coordenadasAnteriores().latitud()-s.getLatitud();
            double dLng=e.coordenadasAnteriores().longitud()-s.getLongitud();
            for(var b:bicicletas.findAll()) if(b.getEstacion().getId().equals(s.getId())) b.reubicar(b.getLatitud()+dLat,b.getLongitud()+dLng);
            s.reubicar(e.nombre(),e.coordenadasAnteriores().latitud(),e.coordenadasAnteriores().longitud());
        }
    }

    @Test void migraBaseAnteriorSinBorrarSesionViajeNiModificarViajeActivo() {
        simularCatalogoAnterior();
        var sesion=auth.iniciarSesion(new LoginRequest("laura@correo.com","BikeGo123!"));
        var viaje=alquiler.iniciar("U-001","BG-001","BIKEGO:BG-001");
        Bicicleta activa=bicicletas.findById("BG-001").orElseThrow();
        // Incidente: puede haber viaje activo aun cuando la bicicleta esté en mantenimiento.
        activa.mantenimiento();
        double lat=activa.getLatitud(),lng=activa.getLongitud();
        long cuentas=usuarios.count();
        catalogo.sincronizar();
        assertThat(bicicletas.count()).isEqualTo(catalogo.estacionesIniciales().size()*10L);
        assertThat(usuarios.count()).isEqualTo(cuentas);
        assertThat(sesiones.count()).isEqualTo(1);
        assertThat(auth.autenticar("Bearer "+sesion.token()).getId()).isEqualTo("U-001");
        assertThat(viajes.findById(viaje.idViaje()).orElseThrow().getEstado()).isEqualTo(EstadoViaje.EN_CURSO);
        assertThat(activa.getLatitud()).isEqualTo(lat);assertThat(activa.getLongitud()).isEqualTo(lng);
        Bicicleta estacionada=bicicletas.findById("BG-003").orElseThrow();
        assertThat(estacionada.getEstado()).isEqualTo(EstadoBicicleta.EN_MANTENIMIENTO);
        assertThat(Geo.distanciaMetros(estacionada.getLatitud(),estacionada.getLongitud(),4.5325,-75.6736)).isLessThan(50);
    }

    @Test void reiniciarNoDuplicaNiRestauraEstadosBateriaODevoluciones() {
        var b=bicicletas.findById("BG-013").orElseThrow();
        var otra=estaciones.findById("EST-06").orElseThrow();
        b.devolver(otra,otra.getLatitud(),otra.getLongitud());b.habilitar(37);b.mantenimiento();
        catalogo.sincronizar();catalogo.sincronizar();
        assertThat(bicicletas.count()).isEqualTo(catalogo.estacionesIniciales().size()*10L);
        assertThat(b.getEstacion().getId()).isEqualTo("EST-06");
        assertThat(b.getBateria()).isEqualTo(37);
        assertThat(b.getEstado()).isEqualTo(EstadoBicicleta.EN_MANTENIMIENTO);
        assertThat(b.getLatitud()).isEqualTo(otra.getLatitud());
    }

    @Test void noTeletransportaUnaBicicletaQueYaEstabaLejosDelPuntoAnterior() {
        simularCatalogoAnterior();
        var b=bicicletas.findById("BG-004").orElseThrow();b.reubicar(4.56,-75.67);
        catalogo.sincronizar();
        assertThat(b.getLatitud()).isEqualTo(4.56);assertThat(b.getLongitud()).isEqualTo(-75.67);
    }
}
