package com.bikego;
import com.bikego.config.DataSeeder;
import com.bikego.domain.*;
import com.bikego.repository.*;
import com.bikego.service.*;
import com.bikego.web.dto.*;
import com.bikego.integration.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import java.time.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={TestDb.URL,"bikego.iot-timeout-ms=20"})
@AutoConfigureMockMvc
class RequisitosRegistroNotificacionesTest {
    @Autowired AutenticacionService auth;
    @Autowired DataSeeder seeder;
    @Autowired UsuarioRepository usuarios;
    @Autowired MensajeRepository mensajes;
    @Autowired AlquilerService alquiler;
    @Autowired OperacionService operaciones;
    @Autowired NotificadorOperaciones notificador;
    @Autowired MockMvc mvc;
    private RegistroRequest registro() {return new RegistroRequest("Ana Torres","1094000999","ana@correo.com","3001234567",LocalDate.of(2000,1,1),"Calle 20 Armenia","ClaveSegura123",true);}
    @BeforeEach void preparar() {mensajes.deleteAll();seeder.cargar();}
    @Test void codigoSeEntregaYTerminosQuedanRegistrados() {
        var req=registro();var r=auth.registrar(req);
        var u=usuarios.findByCorreoIgnoreCase(req.correo()).orElseThrow();
        assertThat(u.getFechaAceptacionTerminos()).isNotNull();assertThat(u.getVersionTerminos()).isEqualTo("2026-09");
        assertThat(u.getTokenPago()).isNull();assertThat(u.getPasswordHash()).doesNotContain(req.password());
        assertThat(mensajes.findByDestinatarioOrderByFechaDesc(req.correo())).hasSize(1);
        assertThat(mensajes.findByDestinatarioOrderByFechaDesc(req.correo()).get(0).contenido).contains(r.codigoDesarrollo());
        assertThatThrownBy(() -> auth.iniciarSesion(new LoginRequest(req.correo(),req.password()))).extracting("codigo").isEqualTo(CodigoError.CUENTA_NO_VERIFICADA);
        auth.verificar(new VerificacionRequest(req.correo(),r.codigoDesarrollo()));
        assertThatThrownBy(() -> alquiler.iniciar(u.getId(),"BG-001","BG-001")).extracting("codigo").isEqualTo(CodigoError.SIN_METODO_PAGO);
    }
    @Test void codigoIncorrectoYVencidoNoActivanCuentaYSePuedeReenviar() {
        var req=registro();var r=auth.registrar(req);
        assertThatThrownBy(() -> auth.verificar(new VerificacionRequest(req.correo(),"no-valido"))).extracting("codigo").isEqualTo(CodigoError.CODIGO_VERIFICACION_INVALIDO);
        var u=usuarios.findByCorreoIgnoreCase(req.correo()).orElseThrow();u.renovarCodigo(u.getCodigoVerificacionHash(),LocalDateTime.now().minusMinutes(1));usuarios.save(u);
        assertThatThrownBy(() -> auth.verificar(new VerificacionRequest(req.correo(),r.codigoDesarrollo()))).extracting("codigo").isEqualTo(CodigoError.CODIGO_VERIFICACION_INVALIDO);
        assertThat(usuarios.findById(u.getId()).orElseThrow().getEstadoCuenta()).isEqualTo(EstadoCuenta.PENDIENTE_VERIFICACION);
        var nuevo=auth.reenviar(new LoginRequest(req.correo(),req.password()));auth.verificar(new VerificacionRequest(req.correo(),nuevo.codigoDesarrollo()));
        assertThat(usuarios.findById(u.getId()).orElseThrow().getEstadoCuenta()).isEqualTo(EstadoCuenta.ACTIVA);
    }
    @Test void apiRechazaCamposInvalidosYTerminosNoAceptados() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"\",\"correo\":\"invalido\",\"documento\":\"1\",\"telefono\":\"abc\",\"fechaNacimiento\":\"2099-01-01\",\"direccion\":\"\",\"password\":\"a\",\"aceptaTerminos\":false}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));
        assertThat(usuarios.findByCorreoIgnoreCase("invalido")).isEmpty();
    }
    @Test void reciboSeEntregaEnBandejaPrivadaConTodosLosDatos() throws Exception {
        long id=alquiler.iniciar("U-001","BG-001","BG-001").idViaje();
        operaciones.finalizar(id,"U-001",new PosicionRequest(4.53389,-75.68111,1.0));var r=operaciones.pagar(id,"U-001");
        var entregados=mensajes.findByDestinatarioOrderByFechaDesc("laura@correo.com");
        assertThat(entregados).hasSize(1);assertThat(entregados.get(0).contenido).contains(r.numero(),"Duración:","Tarifa base:","Recargo:","Total:","PROCESADO");
        String token=auth.iniciarSesion(new LoginRequest("mateo@correo.com","BikeGo123!")).token();
        mvc.perform(get("/api/mensajes").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }
    @Test void alertaDeOperadorSeEntregaInmediatamente() {
        long inicio=System.nanoTime();notificador.operador("Incidente BG-001: Daño grave en 4.53,-75.68");
        assertThat((System.nanoTime()-inicio)/1_000_000).isLessThan(60000);
        assertThat(mensajes.findByDestinatarioOrderByFechaDesc("OPERADORES")).hasSize(1);
    }
}
