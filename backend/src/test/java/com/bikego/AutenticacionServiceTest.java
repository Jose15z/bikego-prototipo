package com.bikego;

import com.bikego.config.DataSeeder;
import com.bikego.domain.EstadoCuenta;
import com.bikego.repository.UsuarioRepository;
import com.bikego.service.AutenticacionService;
import com.bikego.service.CodigoError;
import com.bikego.service.NegocioException;
import com.bikego.web.dto.LoginRequest;
import com.bikego.web.dto.RegistroRequest;
import com.bikego.web.dto.VerificacionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {TestDb.URL, "bikego.mostrar-codigo-verificacion=true"})
class AutenticacionServiceTest {
    @Autowired AutenticacionService autenticacion;
    @Autowired UsuarioRepository usuarios;
    @Autowired DataSeeder seeder;

    @BeforeEach void reiniciar() { seeder.cargar(); }

    @Test
    void registroVerificacionLoginYCierreDeSesion() {
        RegistroRequest req = new RegistroRequest("Ana Torres", "1094000999", "ana@correo.com",
                "+57 3001234567", LocalDate.of(2000, 4, 8), "Carrera 10 # 20-30", "ClaveSegura123", true, true);
        var registro = autenticacion.registrar(req);
        assertThat(usuarios.findByCorreoIgnoreCase(req.correo()).orElseThrow().getEstadoCuenta())
                .isEqualTo(EstadoCuenta.PENDIENTE_VERIFICACION);
        autenticacion.verificar(new VerificacionRequest(req.correo(), registro.codigoDesarrollo()));
        var auth = autenticacion.iniciarSesion(new LoginRequest(req.correo(), req.password()));
        assertThat(autenticacion.autenticar("Bearer " + auth.token()).getCorreo()).isEqualTo(req.correo());
        autenticacion.cerrarSesion("Bearer " + auth.token());
        assertThatThrownBy(() -> autenticacion.autenticar("Bearer " + auth.token()))
                .isInstanceOf(NegocioException.class).extracting("codigo").isEqualTo(CodigoError.NO_AUTENTICADO);
    }

    @Test
    void rechazaCorreoYDocumentoDuplicados() {
        RegistroRequest correoDuplicado = new RegistroRequest("Otra Laura", "1094000888", "laura@correo.com",
                "3001234567", LocalDate.of(2000, 1, 1), "Armenia", "ClaveSegura123", true, true);
        assertThatThrownBy(() -> autenticacion.registrar(correoDuplicado))
                .extracting("codigo").isEqualTo(CodigoError.CORREO_DUPLICADO);
        RegistroRequest documentoDuplicado = new RegistroRequest("Otra persona", "10000001", "otra@correo.com",
                "3001234567", LocalDate.of(2000, 1, 1), "Armenia", "ClaveSegura123", true, true);
        assertThatThrownBy(() -> autenticacion.registrar(documentoDuplicado))
                .extracting("codigo").isEqualTo(CodigoError.DOCUMENTO_DUPLICADO);
    }
}
