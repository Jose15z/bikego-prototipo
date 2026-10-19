package com.bikego.integration;

import java.util.concurrent.CompletableFuture;

/** Puerto hacia la plataforma IoT del candado inteligente. */
public interface CandadoIotClient {

    /**
     * Envia la orden de desbloqueo. El futuro se completa cuando el candado confirma.
     * Si el candado no responde el futuro nunca se completa y quien llama aplica el timeout.
     */
    CompletableFuture<Boolean> enviarDesbloqueo(String bicicletaId);
    default CompletableFuture<Boolean> enviarBloqueo(String bicicletaId) {
        return CompletableFuture.completedFuture(false);
    }
}
