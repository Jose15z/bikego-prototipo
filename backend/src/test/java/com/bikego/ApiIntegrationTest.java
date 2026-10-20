package com.bikego;

import com.bikego.config.DataSeeder;
import com.bikego.service.AutenticacionService;
import com.bikego.web.dto.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Verifica el contrato HTTP que consume el frontend. */
@SpringBootTest(properties = {TestDb.URL, "bikego.iot-timeout-ms=200"})
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired DataSeeder seeder;
    @Autowired AutenticacionService autenticacion;

    @BeforeEach
    void reiniciar() { seeder.cargar(); }

    @Test
    @DisplayName("GET /api/bicicletas/cercanas devuelve la lista ordenada por distancia")
    void cercanas() throws Exception {
        mvc.perform(get("/api/bicicletas/cercanas").param("lat", "4.5325").param("lng", "-75.6736"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.radioMetros").value(1000))
                .andExpect(jsonPath("$.bicicletas[0].id").value("BG-001"));
    }

    @Test
    @DisplayName("GET /api/estaciones sirve el selector manual cuando no hay GPS (SWR-01.1)")
    void estaciones() throws Exception {
        mvc.perform(get("/api/estaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(52));
    }

    @Test
    @DisplayName("POST /api/alquileres responde 201 en el camino feliz y 402 con accion sugerida si el pago falla")
    void alquileres() throws Exception {
        String tokenLaura = autenticacion.iniciarSesion(new LoginRequest("laura@correo.com", "BikeGo123!")).token();
        mvc.perform(post("/api/alquileres").header("Authorization", "Bearer " + tokenLaura)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bicicletaSeleccionadaId\":\"BG-001\",\"codigoQr\":\"BIKEGO:BG-001\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("EN_CURSO"));

        String tokenSofia = autenticacion.iniciarSesion(new LoginRequest("sofia@correo.com", "BikeGo123!")).token();
        mvc.perform(post("/api/alquileres").header("Authorization", "Bearer " + tokenSofia)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bicicletaSeleccionadaId\":\"BG-004\",\"codigoQr\":\"BIKEGO:BG-004\"}"))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.codigo").value("PAGO_RECHAZADO"))
                .andExpect(jsonPath("$.accionSugerida").value("ACTUALIZAR_METODO_PAGO"));
    }

    @Test
    @DisplayName("GET /api/bicicletas/{id} responde 409 si la bicicleta ya no esta disponible (SWR-01.6)")
    void detalleNoDisponible() throws Exception {
        mvc.perform(get("/api/bicicletas/BG-005"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("BICICLETA_NO_DISPONIBLE"));
    }
}
