package com.bikego.service;

import com.bikego.config.BikeGoProperties;
import com.bikego.domain.EstadoCuenta;
import com.bikego.domain.Sesion;
import com.bikego.domain.Usuario;
import com.bikego.integration.NotificadorVerificacion;
import com.bikego.repository.SesionRepository;
import com.bikego.repository.UsuarioRepository;
import com.bikego.web.dto.AuthDto;
import com.bikego.web.dto.LoginRequest;
import com.bikego.web.dto.RegistroDto;
import com.bikego.web.dto.RegistroRequest;
import com.bikego.web.dto.UsuarioDto;
import com.bikego.web.dto.VerificacionRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

@Service
public class AutenticacionService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String VERSION_TERMINOS = "2026-09";

    private final UsuarioRepository usuarios;
    private final SesionRepository sesiones;
    private final BikeGoProperties props;
    private final Clock clock;
    private final NotificadorVerificacion notificador;

    public AutenticacionService(UsuarioRepository usuarios, SesionRepository sesiones,
                                BikeGoProperties props, Clock clock, NotificadorVerificacion notificador) {
        this.usuarios = usuarios;
        this.sesiones = sesiones;
        this.props = props;
        this.clock = clock;
        this.notificador = notificador;
    }

    @Transactional
    public RegistroDto registrar(RegistroRequest req) {
        String correo = req.correo().trim().toLowerCase(Locale.ROOT);
        String documento = req.documento().trim();
        if (usuarios.existsByCorreoIgnoreCase(correo)) {
            throw new NegocioException(CodigoError.CORREO_DUPLICADO, "El correo ya está registrado");
        }
        if (usuarios.existsByDocumento(documento)) {
            throw new NegocioException(CodigoError.DOCUMENTO_DUPLICADO, "El documento ya está registrado");
        }
        LocalDateTime ahora = LocalDateTime.now(clock);
        String codigo = String.format("%06d", RANDOM.nextInt(1_000_000));
        Usuario usuario = new Usuario("USR-" + UUID.randomUUID(), req.nombre().trim(), correo, documento,
                req.telefono().trim(), req.fechaNacimiento(), req.direccion().trim(),
                SeguridadHash.password(req.password()), ahora, VERSION_TERMINOS,
                SeguridadHash.sha256(codigo), ahora.plusMinutes(props.getVerificacionMinutos()));
        usuarios.save(usuario);
        notificador.enviarCodigo(correo, usuario.getNombre(), codigo, props.getVerificacionMinutos());
        return new RegistroDto(correo, "Cuenta creada. Revisa tu correo para ingresar el código de verificación.",
                props.isMostrarCodigoVerificacion() ? codigo : null);
    }

    @Transactional
    public void verificar(VerificacionRequest req) {
        Usuario usuario = usuarios.findByCorreoIgnoreCase(req.correo().trim())
                .orElseThrow(() -> new NegocioException(CodigoError.CODIGO_VERIFICACION_INVALIDO,
                        "El código es incorrecto o venció"));
        LocalDateTime ahora = LocalDateTime.now(clock);
        if (!usuario.codigoVigente(ahora)
                || !SeguridadHash.sha256(req.codigo()).equals(usuario.getCodigoVerificacionHash())) {
            throw new NegocioException(CodigoError.CODIGO_VERIFICACION_INVALIDO,
                    "El código es incorrecto o venció");
        }
        usuario.activar();
    }

    @Transactional
    public AuthDto iniciarSesion(LoginRequest req) {
        Usuario usuario = usuarios.findByCorreoIgnoreCase(req.correo().trim())
                .orElseThrow(() -> credencialesInvalidas());
        if (!SeguridadHash.verificarPassword(req.password(), usuario.getPasswordHash())) {
            throw credencialesInvalidas();
        }
        if (usuario.getEstadoCuenta() == EstadoCuenta.PENDIENTE_VERIFICACION) {
            throw new NegocioException(CodigoError.CUENTA_NO_VERIFICADA,
                    "Debes verificar tu correo antes de iniciar sesión", "VERIFICAR_CUENTA");
        }
        if (usuario.getEstadoCuenta() == EstadoCuenta.SUSPENDIDA) {
            throw new NegocioException(CodigoError.CUENTA_SUSPENDIDA, "Tu cuenta está suspendida");
        }
        String token = nuevoToken();
        LocalDateTime ahora = LocalDateTime.now(clock);
        sesiones.save(new Sesion(usuario, SeguridadHash.sha256(token), ahora,
                ahora.plusHours(props.getSesionHoras())));
        return new AuthDto(token, UsuarioDto.de(usuario, ahora));
    }

    @Transactional(readOnly = true)
    public Usuario autenticar(String encabezado) {
        String token = extraerToken(encabezado);
        Sesion sesion = sesiones.findByTokenHash(SeguridadHash.sha256(token))
                .orElseThrow(this::noAutenticado);
        if (sesion.getUsuario().getEstadoCuenta() != EstadoCuenta.ACTIVA) throw noAutenticado();
        if (!sesion.estaActiva(LocalDateTime.now(clock))) throw noAutenticado();
        return sesion.getUsuario();
    }

    @Transactional
    public void cerrarSesion(String encabezado) {
        String token = extraerToken(encabezado);
        sesiones.findByTokenHash(SeguridadHash.sha256(token))
                .ifPresent(s -> s.revocar(LocalDateTime.now(clock)));
    }

    @Transactional
    public RegistroDto reenviar(LoginRequest req) {
        Usuario u=usuarios.findByCorreoIgnoreCase(req.correo().trim()).orElseThrow(this::credencialesInvalidas);
        if(!SeguridadHash.verificarPassword(req.password(),u.getPasswordHash())) throw credencialesInvalidas();
        if(u.getEstadoCuenta()!=EstadoCuenta.PENDIENTE_VERIFICACION) throw new NegocioException(CodigoError.DATOS_INVALIDOS,"Esta cuenta ya fue verificada");
        String codigo=String.format("%06d",RANDOM.nextInt(1_000_000));
        u.renovarCodigo(SeguridadHash.sha256(codigo),LocalDateTime.now(clock).plusMinutes(props.getVerificacionMinutos()));
        notificador.enviarCodigo(u.getCorreo(),u.getNombre(),codigo,props.getVerificacionMinutos());
        return new RegistroDto(u.getCorreo(),"Nuevo código enviado",props.isMostrarCodigoVerificacion()?codigo:null);
    }

    private String nuevoToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String extraerToken(String encabezado) {
        if (encabezado == null || !encabezado.startsWith("Bearer ") || encabezado.length() < 20) {
            throw noAutenticado();
        }
        return encabezado.substring(7);
    }

    private NegocioException credencialesInvalidas() {
        return new NegocioException(CodigoError.CREDENCIALES_INVALIDAS, "Correo o contraseña incorrectos");
    }

    private NegocioException noAutenticado() {
        return new NegocioException(CodigoError.NO_AUTENTICADO, "Tu sesión no es válida o venció");
    }
}
