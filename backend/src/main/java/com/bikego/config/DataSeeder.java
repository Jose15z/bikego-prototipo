package com.bikego.config;

import com.bikego.domain.Estacion;
import com.bikego.domain.EstadoCuenta;
import com.bikego.domain.Usuario;
import com.bikego.repository.BicicletaRepository;
import com.bikego.repository.EstacionRepository;
import com.bikego.repository.RechazoPagoRepository;
import com.bikego.repository.SesionRepository;
import com.bikego.repository.UsuarioRepository;
import com.bikego.repository.ViajeRepository;
import com.bikego.service.SeguridadHash;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


/**
 * Datos de prueba en Armenia (Quindio). Ubicaciones contrastadas con OpenStreetMap; flota simulada de diez bicicletas por estación.
 * Cada bicicleta y cada usuario cubre un caso de verificacion del Plan de Requisitos.
 */
@Component
public class DataSeeder implements ApplicationRunner {
    private static final String PASSWORD_INICIAL_HASH = SeguridadHash.password("BikeGo123!");

    private final CatalogoDemostracion catalogo;
    private final EstacionRepository estaciones;
    private final BicicletaRepository bicicletas;
    private final UsuarioRepository usuarios;
    private final ViajeRepository viajes;
    private final RechazoPagoRepository rechazos;
    private final SesionRepository sesiones;

    public DataSeeder(EstacionRepository estaciones, BicicletaRepository bicicletas, UsuarioRepository usuarios,
                      ViajeRepository viajes, RechazoPagoRepository rechazos, SesionRepository sesiones, CatalogoDemostracion catalogo) {
        this.catalogo = catalogo;
        this.estaciones = estaciones;
        this.bicicletas = bicicletas;
        this.usuarios = usuarios;
        this.viajes = viajes;
        this.rechazos = rechazos;
        this.sesiones = sesiones;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (estaciones.count() == 0) cargar();
        else catalogo.sincronizar();
        crearPersonal();
    }

    @Transactional
    public void cargar() {
        sesiones.deleteAllInBatch();
        viajes.deleteAllInBatch();
        rechazos.deleteAllInBatch();
        bicicletas.deleteAllInBatch();
        usuarios.deleteAllInBatch();
        estaciones.deleteAllInBatch();

        for (var e : catalogo.estacionesIniciales()) {
            estaciones.save(new Estacion(e.id(), e.nombre(), e.latitud(), e.longitud()));
        }
        catalogo.sincronizar();

        usuario("U-001", "10000001", "Laura Gómez", "laura@correo.com", EstadoCuenta.ACTIVA, 0,
                "tok_ok_4242", "Visa", "4242", "Cuenta activa, sin deuda y tarjeta vigente");
        usuario("U-002", "10000002", "Carlos Pérez", "carlos@correo.com", EstadoCuenta.ACTIVA, 8500,
                "tok_ok_5100", "Mastercard", "5100", "Deuda pendiente de $8.500 (RN-001)");
        usuario("U-003", "10000003", "Valentina Ríos", "valentina@correo.com", EstadoCuenta.ACTIVA, 0,
                "tok_vencida_1881", "Visa", "1881", "Tarjeta vencida: la pasarela rechaza (RN-008)");
        usuario("U-004", "10000004", "Andrés Castaño", "andres@correo.com", EstadoCuenta.SUSPENDIDA, 0,
                "tok_ok_3005", "Visa", "3005", "Cuenta suspendida");
        usuario("U-005", "10000005", "Sofía Marín", "sofia@correo.com", EstadoCuenta.ACTIVA, 0,
                "tok_fondos_0002", "Visa", "0002", "Fondos insuficientes en la pasarela");
        usuario("U-006", "10000006", "Mateo Londoño", "mateo@correo.com", EstadoCuenta.ACTIVA, 0,
                "tok_ok_7777", "Mastercard", "7777", "Segundo usuario válido");
    }

    private void crearPersonal() {
        if (!usuarios.existsById("STAFF-01")) {
            Usuario u = new Usuario("STAFF-01", "Operador BikeGo", "operador@bikego.local", EstadoCuenta.ACTIVA, 0, null, null, null, "Operación local de flota");
            u.configurarCredenciales("90000001", PASSWORD_INICIAL_HASH); u.asignarRol("OPERADOR"); usuarios.save(u);
        }
        if (!usuarios.existsById("STAFF-02")) {
            Usuario u = new Usuario("STAFF-02", "Técnico BikeGo", "tecnico@bikego.local", EstadoCuenta.ACTIVA, 0, null, null, null, "Mantenimiento local");
            u.configurarCredenciales("90000002", PASSWORD_INICIAL_HASH); u.asignarRol("TECNICO"); usuarios.save(u);
        }
    }

    private void usuario(String id, String documento, String nombre, String correo, EstadoCuenta estado, int deuda,
                         String token, String marca, String ultimos4, String escenario) {
        Usuario usuario = new Usuario(id, nombre, correo, estado, deuda, token, marca, ultimos4, escenario);
        usuario.configurarCredenciales(documento, PASSWORD_INICIAL_HASH);
        usuarios.save(usuario);
    }

}
