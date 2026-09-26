package com.bikego.config;

import com.bikego.domain.Bicicleta;
import com.bikego.domain.Estacion;
import com.bikego.domain.EstadoBicicleta;
import com.bikego.domain.EstadoCuenta;
import com.bikego.domain.Usuario;
import com.bikego.repository.BicicletaRepository;
import com.bikego.repository.EstacionRepository;
import com.bikego.repository.RechazoPagoRepository;
import com.bikego.repository.UsuarioRepository;
import com.bikego.repository.ViajeRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.bikego.domain.EstadoBicicleta.DISPONIBLE;
import static com.bikego.domain.EstadoBicicleta.EN_MANTENIMIENTO;
import static com.bikego.domain.EstadoBicicleta.EN_USO;

/**
 * Datos de prueba en Armenia (Quindio). Las coordenadas de las estaciones son aproximadas.
 * Cada bicicleta y cada usuario cubre un caso de verificacion del Plan de Requisitos.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private final EstacionRepository estaciones;
    private final BicicletaRepository bicicletas;
    private final UsuarioRepository usuarios;
    private final ViajeRepository viajes;
    private final RechazoPagoRepository rechazos;

    public DataSeeder(EstacionRepository estaciones, BicicletaRepository bicicletas, UsuarioRepository usuarios,
                      ViajeRepository viajes, RechazoPagoRepository rechazos) {
        this.estaciones = estaciones;
        this.bicicletas = bicicletas;
        this.usuarios = usuarios;
        this.viajes = viajes;
        this.rechazos = rechazos;
    }

    @Override
    public void run(ApplicationArguments args) {
        cargar();
    }

    @Transactional
    public void cargar() {
        viajes.deleteAllInBatch();
        rechazos.deleteAllInBatch();
        bicicletas.deleteAllInBatch();
        usuarios.deleteAllInBatch();
        estaciones.deleteAllInBatch();

        Estacion plaza = estaciones.save(new Estacion("EST-01", "Plaza de Bolívar", 4.53389, -75.68111));
        Estacion uq = estaciones.save(new Estacion("EST-02", "Universidad del Quindío", 4.55530, -75.66050));
        Estacion vida = estaciones.save(new Estacion("EST-03", "Parque de la Vida", 4.54740, -75.66520));
        Estacion unicentro = estaciones.save(new Estacion("EST-04", "C.C. Unicentro", 4.54060, -75.66940));
        Estacion centenario = estaciones.save(new Estacion("EST-05", "Estadio Centenario", 4.52560, -75.69180));
        Estacion terminal = estaciones.save(new Estacion("EST-06", "Terminal de Transportes", 4.52070, -75.68000));

        bici("BG-001", DISPONIBLE, 85, plaza, 0.00020, 0.00015, false);
        bici("BG-002", DISPONIBLE, 15, plaza, -0.00018, 0.00022, false);      // bateria < 20 %: oculta (RN-005)
        bici("BG-003", EN_MANTENIMIENTO, 90, plaza, 0.00012, -0.00025, false); // oculta (RN-003)
        bici("BG-004", DISPONIBLE, 62, unicentro, 0.00010, 0.00012, false);
        bici("BG-005", EN_USO, 70, unicentro, -0.00015, -0.00010, false);      // oculta (RN-002)
        bici("BG-006", DISPONIBLE, 100, uq, 0.00015, 0.00020, false);
        bici("BG-007", DISPONIBLE, 45, uq, -0.00020, -0.00012, false);
        bici("BG-008", DISPONIBLE, 33, vida, 0.00010, -0.00018, true);         // candado sin respuesta (SWR-02.5)
        bici("BG-009", DISPONIBLE, 78, centenario, 0.00014, 0.00010, false);
        bici("BG-010", DISPONIBLE, 20, terminal, 0.00012, 0.00016, false);     // limite exacto: visible
        bici("BG-011", DISPONIBLE, 19, terminal, -0.00010, 0.00008, false);    // 19 %: oculta
        bici("BG-012", EN_MANTENIMIENTO, 50, vida, -0.00016, 0.00014, false);

        usuarios.save(new Usuario("U-001", "Laura Gómez", "laura@correo.com", EstadoCuenta.ACTIVA, 0,
                "tok_ok_4242", "Visa", "4242", "Cuenta activa, sin deuda y tarjeta vigente (camino feliz)"));
        usuarios.save(new Usuario("U-002", "Carlos Pérez", "carlos@correo.com", EstadoCuenta.ACTIVA, 8500,
                "tok_ok_5100", "Mastercard", "5100", "Deuda pendiente de $8.500 (RN-001)"));
        usuarios.save(new Usuario("U-003", "Valentina Ríos", "valentina@correo.com", EstadoCuenta.ACTIVA, 0,
                "tok_vencida_1881", "Visa", "1881", "Tarjeta vencida: la pasarela rechaza (RN-008)"));
        usuarios.save(new Usuario("U-004", "Andrés Castaño", "andres@correo.com", EstadoCuenta.SUSPENDIDA, 0,
                "tok_ok_3005", "Visa", "3005", "Cuenta suspendida"));
        usuarios.save(new Usuario("U-005", "Sofía Marín", "sofia@correo.com", EstadoCuenta.ACTIVA, 0,
                "tok_fondos_0002", "Visa", "0002", "Fondos insuficientes en la pasarela"));
        usuarios.save(new Usuario("U-006", "Mateo Londoño", "mateo@correo.com", EstadoCuenta.ACTIVA, 0,
                "tok_ok_7777", "Mastercard", "7777", "Segundo usuario válido (prueba de concurrencia)"));
    }

    private void bici(String id, EstadoBicicleta estado, int bateria, Estacion est,
                      double dLat, double dLng, boolean candadoConFalla) {
        bicicletas.save(new Bicicleta(id, estado, bateria, est,
                est.getLatitud() + dLat, est.getLongitud() + dLng, candadoConFalla));
    }
}
