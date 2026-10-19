package com.bikego.web;

import com.bikego.domain.Usuario;
import com.bikego.service.AutenticacionService;
import com.bikego.web.dto.AuthDto;
import com.bikego.web.dto.LoginRequest;
import com.bikego.web.dto.RegistroDto;
import com.bikego.web.dto.RegistroRequest;
import com.bikego.web.dto.UsuarioDto;
import com.bikego.web.dto.VerificacionRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
public class AutenticacionController {
    private final AutenticacionService autenticacion;
    private final Clock clock;

    public AutenticacionController(AutenticacionService autenticacion, Clock clock) {
        this.autenticacion = autenticacion;
        this.clock = clock;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistroDto registrar(@Valid @RequestBody RegistroRequest req) { return autenticacion.registrar(req); }

    @PostMapping("/verificacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verificar(@Valid @RequestBody VerificacionRequest req) { autenticacion.verificar(req); }

    @PostMapping("/reenviar")
    public RegistroDto reenviar(@Valid @RequestBody LoginRequest req) { return autenticacion.reenviar(req); }

    @PostMapping("/login")
    public AuthDto login(@Valid @RequestBody LoginRequest req) { return autenticacion.iniciarSesion(req); }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader(value = "Authorization", required = false) String auth) {
        autenticacion.cerrarSesion(auth);
    }

    @GetMapping("/me")
    public UsuarioDto me(@RequestHeader(value = "Authorization", required = false) String auth) {
        Usuario usuario = autenticacion.autenticar(auth);
        return UsuarioDto.de(usuario, LocalDateTime.now(clock));
    }
}
