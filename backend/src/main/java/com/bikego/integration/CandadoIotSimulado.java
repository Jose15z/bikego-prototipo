package com.bikego.integration;

import com.bikego.config.BikeGoProperties;
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
    private final BikeGoProperties props;

    public CandadoIotSimulado(BicicletaRepository bicicletas, BikeGoProperties props) {
        this.bicicletas = bicicletas;
        this.props = props;
    }

    @Override
    public CompletableFuture<Boolean> enviarDesbloqueo(String bicicletaId) {
        boolean falla = bicicletas.findById(bicicletaId).map(b -> b.isCandadoConFalla()).orElse(true);
        if (falla) {
            return new CompletableFuture<>();
        }
        long demora = Math.min(600, Math.max(1, props.getIotTimeoutMs() / 2));
        return CompletableFuture.supplyAsync(() -> Boolean.TRUE,
                CompletableFuture.delayedExecutor(demora, TimeUnit.MILLISECONDS));
    }
    @Override
    public CompletableFuture<Boolean> enviarBloqueo(String bicicletaId) { return enviarDesbloqueo(bicicletaId); }
}
