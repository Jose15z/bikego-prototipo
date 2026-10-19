package com.bikego.service;

import com.bikego.config.BikeGoProperties;
import com.bikego.domain.Bicicleta;
import com.bikego.domain.EstadoBicicleta;
import com.bikego.domain.EstadoCuenta;
import com.bikego.domain.EstadoViaje;
import com.bikego.domain.RechazoPago;
import com.bikego.domain.Usuario;
import com.bikego.domain.Viaje;
import com.bikego.integration.CandadoIotClient;
import com.bikego.integration.PasarelaPagoClient;
import com.bikego.integration.ResultadoPago;
import com.bikego.repository.BicicletaRepository;
import com.bikego.repository.RechazoPagoRepository;
import com.bikego.repository.UsuarioRepository;
import com.bikego.repository.ViajeRepository;
import com.bikego.web.dto.ViajeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** F-02 Iniciar alquiler de bicicleta mediante codigo QR. */
@Service
public class AlquilerService {

    private static final Logger log = LoggerFactory.getLogger(AlquilerService.class);

    private final BicicletaRepository bicicletas;
    private final UsuarioRepository usuarios;
    private final ViajeRepository viajes;
    private final RechazoPagoRepository rechazos;
    private final PasarelaPagoClient pasarela;
    private final CandadoIotClient candado;
    private final BikeGoProperties props;
    private final Clock clock;

    public AlquilerService(BicicletaRepository bicicletas, UsuarioRepository usuarios, ViajeRepository viajes,
                           RechazoPagoRepository rechazos, PasarelaPagoClient pasarela, CandadoIotClient candado,
                           BikeGoProperties props, Clock clock) {
        this.bicicletas = bicicletas;
        this.usuarios = usuarios;
        this.viajes = viajes;
        this.rechazos = rechazos;
        this.pasarela = pasarela;
        this.candado = candado;
        this.props = props;
        this.clock = clock;
    }

    /**
     * Orden de validacion: QR (02.1), bicicleta (02.2), cuenta (02.3), pago (02.4),
     * desbloqueo IoT (02.5) y registro del viaje (02.6).
     * noRollbackFor: el rechazo de pago y el bloqueo (02.7) deben quedar guardados aunque se lance el error.
     */
    @Transactional(noRollbackFor = NegocioException.class)
    public ViajeDto iniciar(String usuarioId, String bicicletaSeleccionadaId, String contenidoQr) {
        LocalDateTime ahora = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);

        // SWR-02.1
        String idLeido = QrParser.extraerIdBicicleta(contenidoQr)
                .orElseThrow(() -> new NegocioException(CodigoError.QR_INVALIDO,
                        "El código QR no es legible o no pertenece a BikeGo", "REINTENTAR_O_INGRESAR_ID"));
        Bicicleta bici = bicicletas.findByIdParaActualizar(idLeido)
                .orElseThrow(() -> new NegocioException(CodigoError.QR_INVALIDO,
                        "El código " + idLeido + " no corresponde a una bicicleta registrada", "REINTENTAR_O_INGRESAR_ID"));

        // SWR-02.2
        if (bicicletaSeleccionadaId != null && !bicicletaSeleccionadaId.equalsIgnoreCase(idLeido)) {
            throw new NegocioException(CodigoError.BICICLETA_NO_COINCIDE,
                    "Escaneaste la bicicleta " + idLeido + " pero seleccionaste la " + bicicletaSeleccionadaId);
        }
        validarEstadoBicicleta(bici);

        // SWR-02.3
        Usuario usuario = usuarios.bloquear(usuarioId)
                .orElseThrow(() -> new NegocioException(CodigoError.USUARIO_NO_ENCONTRADO, "Usuario no encontrado"));
        validarCuenta(usuario, ahora);

        // SWR-02.4 y SWR-02.7
        ResultadoPago pago = PagoSeguro.ejecutar(() -> pasarela.verificar(usuario.getTokenPago()));
        if (!pago.autorizado()) {
            LocalDateTime hasta = ahora.plusMinutes(props.getBloqueoPagoMinutos());
            usuario.bloquearHasta(hasta);
            rechazos.save(new RechazoPago(usuario, pago.getMotivo(), ahora, hasta));
            throw new NegocioException(CodigoError.PAGO_RECHAZADO,
                    pago.getMotivo() + ". Tu cuenta queda bloqueada para alquilar por "
                            + props.getBloqueoPagoMinutos() + " minutos (RN-008).",
                    "ACTUALIZAR_METODO_PAGO");
        }

        // SWR-02.5
        desbloquearConReintentos(bici.getId());

        // SWR-02.6: el tiempo facturable comienza tras la confirmación del candado.
        ahora = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        bici.marcarEnUso();
        Viaje viaje = viajes.save(new Viaje(usuario, bici, ahora));
        log.info("Viaje {} iniciado por {} con bicicleta {}", viaje.getIdViaje(), usuarioId, bici.getId());
        return toDto(viaje, ahora);
    }

    @Transactional(readOnly = true)
    public ViajeDto consultarViaje(Long idViaje) {
        Viaje v = viajes.findById(idViaje)
                .orElseThrow(() -> new NegocioException(CodigoError.VIAJE_NO_ENCONTRADO, "Viaje no encontrado"));
        return toDto(v, LocalDateTime.now(clock));
    }

    @Transactional(readOnly = true)
    public ViajeDto consultarViaje(Long idViaje, String usuarioId) {
        Viaje v = viajes.findById(idViaje)
                .orElseThrow(() -> new NegocioException(CodigoError.VIAJE_NO_ENCONTRADO, "Viaje no encontrado"));
        if (!v.getUsuario().getId().equals(usuarioId)) {
            throw new NegocioException(CodigoError.VIAJE_NO_ENCONTRADO, "Viaje no encontrado");
        }
        return toDto(v, LocalDateTime.now(clock));
    }

    private void validarEstadoBicicleta(Bicicleta bici) {
        if (bici.getEstado() == EstadoBicicleta.EN_MANTENIMIENTO) {
            throw new NegocioException(CodigoError.BICICLETA_EN_MANTENIMIENTO,
                    "La bicicleta " + bici.getId() + " está en mantenimiento");
        }
        if (bici.getEstado() == EstadoBicicleta.EN_USO) {
            throw new NegocioException(CodigoError.BICICLETA_EN_USO,
                    "La bicicleta " + bici.getId() + " ya está en uso por otro usuario");
        }
        if (bici.getBateria() < props.getBateriaMinima()) {
            throw new NegocioException(CodigoError.BATERIA_INSUFICIENTE,
                    "La bicicleta " + bici.getId() + " tiene " + bici.getBateria() + " % de batería (mínimo "
                            + props.getBateriaMinima() + " %)");
        }
    }

    private void validarCuenta(Usuario u, LocalDateTime ahora) {
        if (u.getEstadoCuenta() == EstadoCuenta.SUSPENDIDA) {
            throw new NegocioException(CodigoError.CUENTA_SUSPENDIDA, "Tu cuenta está suspendida");
        }
        if (u.getEstadoCuenta() == EstadoCuenta.PENDIENTE_VERIFICACION) {
            throw new NegocioException(CodigoError.CUENTA_NO_VERIFICADA, "Debes verificar tu cuenta");
        }
        if (u.estaBloqueado(ahora)) {
            throw new NegocioException(CodigoError.USUARIO_BLOQUEADO,
                    "Tienes un bloqueo temporal por un pago rechazado hasta las "
                            + u.getBloqueadoHasta().toLocalTime().withNano(0) + " (RN-008)");
        }
        if (viajes.existsByUsuarioIdAndEstado(u.getId(), EstadoViaje.EN_CURSO)) {
            throw new NegocioException(CodigoError.VIAJE_EN_CURSO, "Ya tienes un viaje en curso");
        }
        if (u.getDeudaPendiente() > 0) {
            throw new NegocioException(CodigoError.DEUDA_PENDIENTE,
                    "Tienes una deuda pendiente de $" + String.format(Locale.forLanguageTag("es-CO"), "%,d", u.getDeudaPendiente()) + " COP (RN-001)");
        }
        if (!u.tieneMetodoPago()) {
            throw new NegocioException(CodigoError.SIN_METODO_PAGO,
                    "No tienes un método de pago registrado", "ACTUALIZAR_METODO_PAGO");
        }
    }

    /** SWR-02.5: hasta 1 + iotReintentos intentos, cada uno con espera maxima iotTimeoutMs. */
    private void desbloquearConReintentos(String bicicletaId) {
        int intentos = 1 + props.getIotReintentos();
        for (int i = 1; i <= intentos; i++) {
            try {
                Boolean ok = candado.enviarDesbloqueo(bicicletaId).get(props.getIotTimeoutMs(), TimeUnit.MILLISECONDS);
                if (Boolean.TRUE.equals(ok)) {
                    log.info("Candado {} confirmo desbloqueo en el intento {}", bicicletaId, i);
                    return;
                }
            } catch (TimeoutException e) {
                log.warn("Candado {} sin confirmacion en el intento {} de {}", bicicletaId, i, intentos);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (ExecutionException e) {
                log.warn("Error del candado {} en el intento {}: {}", bicicletaId, i, e.getMessage());
            }
        }
        throw new NegocioException(CodigoError.DESBLOQUEO_FALLIDO,
                "No fue posible desbloquear la bicicleta después de " + intentos
                        + " intentos. No se inició el viaje ni se hizo ningún cobro.");
    }

    private ViajeDto toDto(Viaje v, LocalDateTime ahora) {
        long segundos = Math.max(0, Duration.between(v.getHoraInicio(), v.getHoraFin() == null ? ahora : v.getHoraFin()).getSeconds());
        long minutosCobrables = (long) Math.ceil(segundos / 60.0);
        long costo = v.getHoraFin() == null ? props.getTarifaBase() + minutosCobrables * props.getTarifaMinuto() : v.getTotal();
        double distanciaKm = Math.round(props.getVelocidadPromedioKmh() * segundos / 3600.0 * 100) / 100.0;
        return new ViajeDto(v.getIdViaje(), v.getUsuario().getId(), v.getBicicleta().getId(),
                v.getHoraInicio(), v.getEstado().name(), segundos, distanciaKm, costo,
                v.getHoraFin() == null ? props.getTarifaBase() : v.getTarifaBaseAplicada(),
                v.getHoraFin() == null ? props.getTarifaMinuto() : v.getTarifaMinutoAplicada(),
                props.getVelocidadPromedioKmh(), v.isConIncidente());
    }
}
