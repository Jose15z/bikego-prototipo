package com.bikego.service;

import com.bikego.integration.ResultadoPago;
import java.util.concurrent.*;
import java.util.function.Supplier;

/** Cada intento tiene un límite de cinco segundos; solo se reintentan fallos técnicos. */
public final class PagoSeguro {
    private PagoSeguro() {}
    private static final ExecutorService EXECUTOR = new ThreadPoolExecutor(0, 8, 30, TimeUnit.SECONDS,
        new SynchronousQueue<>(), r -> { Thread t = new Thread(r, "pasarela"); t.setDaemon(true); return t; });
    public static ResultadoPago ejecutar(Supplier<ResultadoPago> accion) {
        for (int i = 0; i < 3; i++) {
            Future<ResultadoPago> futuro = null;
            try {
                futuro = EXECUTOR.submit(accion::get);
                ResultadoPago r = futuro.get(5, TimeUnit.SECONDS);
                if (r != null) return r;
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
            catch (ExecutionException | TimeoutException | RejectedExecutionException e) {
                org.slf4j.LoggerFactory.getLogger(PagoSeguro.class).warn("Pasarela sin respuesta: intento {} de 3", i + 1);
            } finally { if (futuro != null && !futuro.isDone()) futuro.cancel(true); }
        }
        throw new NegocioException(CodigoError.PAGO_NO_DISPONIBLE, "Servicio de pago no disponible", "ACTUALIZAR_METODO_PAGO");
    }
}
