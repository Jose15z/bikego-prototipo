package com.bikego.integration;

import com.bikego.repository.BicicletaRepository;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Simulador IoT. Un candado sano confirma en unos 600 ms.
 * Una bicicleta marcada con candadoConFalla nunca confirma (prueba de SWR-02.5).
 */
@Component
public class CandadoIotSimulado implements CandadoIotClient {

    private final BicicletaRepository bicicletas;

    public CandadoIotSimulado(BicicletaRepository bicicletas) {
        this.bicicletas = bicicletas;
    }

    @Override
    public CompletableFuture<Boolean> enviarDesbloqueo(String bicicletaId) {
        boolean falla = bicicletas.findById(bicicletaId).map(b -> b.isCandadoConFalla()).orElse(true);
        if (falla) {
            return new CompletableFuture<>();
        }
        return CompletableFuture.supplyAsync(() -> Boolean.TRUE,
                CompletableFuture.delayedExecutor(600, TimeUnit.MILLISECONDS));
    }
}
