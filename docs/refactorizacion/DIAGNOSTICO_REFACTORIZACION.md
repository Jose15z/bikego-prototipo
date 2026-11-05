# Diagnóstico y refactorización del cierre de viajes en BikeGo

Jose Manuel Rodriguez Ospina

## Resumen

Se auditó el cierre de viajes de BikeGo, desde la validación de la solicitud hasta la devolución de la bicicleta. La revisión identificó tres hallazgos: dependencia de la aplicación respecto de los DTO web, espera del candado con filas bloqueadas y ausencia de evidencia GPS independiente en la devolución manual. Se propone desacoplar el contrato HTTP mediante un cambio acotado. La línea base ejecutada fue de 65 pruebas aprobadas. No se modificó el código de la aplicación.

Palabras clave—Arquitectura por capas, refactorización, transacciones, pruebas de regresión.

## I. Contexto y alcance

BikeGo es una aplicación de alquiler de bicicletas eléctricas. El proyecto usa React, TypeScript y Vite en el cliente; Java 17 como objetivo de compilación, Spring Boot 3.3.5 y JPA sobre H2 en el servidor [1], [2]. El equipo original del proyecto está conformado por José Manuel Rodríguez Ospina y Juan Gutiérrez; este informe se presenta a nombre de Jose Manuel Rodriguez Ospina.

El caso auditado es F-05, cierre y devolución de un viaje. Comprende SWR-05.1 y 05.2 (zona, tolerancia y orientación), SWR-05.3 (confirmación del candado), SWR-05.4 (cierre y devolución) y SWR-05.5 (umbral de recargo) [3]. El cálculo congelado enlaza con SWR-06.1; el cobro posterior no se incluye como objeto de intervención.

## II. Arquitectura y flujo real

1) Presentación. FinalizarViaje.tsx, líneas 14–27, consulta zonas, toma la posición y envía la solicitud de cierre. El cobro se hace por otra llamada. OperacionController.finalizar (31–33) valida PosicionRequest con @Valid, autentica y delega.

2) Aplicación. OperacionService.finalizar (56–80) comprueba propiedad, devuelve el recibo si el viaje ya cerró, selecciona la primera zona válida por orden de distancia, bloquea la bicicleta y espera al candado. En caso de fallo no finaliza; envía la alerta al operador y devuelve el error al cliente.

3) Dominio y persistencia. Viaje.finalizar (55–60) fija hora, ubicación, tarifas y total. Bicicleta.devolver (51–54) cambia estación y coordenadas y preserva EN_MANTENIMIENTO. ViajeRepository (13–15) y BicicletaRepository (19–21) obtienen bloqueos pesimistas; EstacionRepository suministra las coordenadas de cada estación; zonas (47–54) construye la geometría.

4) Integración. CandadoIotClient abstrae el bloqueo. El límite y los reintentos se configuran en application.properties (20–21). BandejaLocal.entregar (13–16) usa REQUIRES_NEW para persistir el aviso de forma independiente del cierre.

## III. Contraste documentado

El código coincide con el monolito por capas y los puertos de integración descritos en el documento de arquitectura [2]. Este documento reconoce que los servicios usan DTO web y que OperacionService agrupa cierre, pagos y flota. H-01 concreta esa dependencia conocida. La concentración de casos de uso, por sí sola, no demuestra una violación de SRP; la evidencia se apoya en dependencias y fronteras específicas.

Versión inspeccionada: a1962ab29d72b19bed9fb1aa18ec51984d98e40b. Las líneas remiten a ese estado local del repositorio [1].

## IV. Matriz técnica de hallazgos

TABLA I. EVIDENCIA, IMPACTO Y VERIFICACIÓN

| ID y estado | Evidencia y diagnóstico | Propuesta y verificación |
|---|---|---|
| H-01<br>Deuda técnica<br>Seleccionado | backend/src/main/java/com/bikego/service/OperacionService.java, finalizar (7, 56–59 y 80).<br>«public ReciboDto finalizar(Long id, String usuario, PosicionRequest p)»<br>La aplicación importa com.bikego.web.dto.* y usa tipos de entrada y salida del adaptador HTTP. El contrato del caso de uso queda acoplado a cambios de representación web. Criterio: dirección de dependencias y modificabilidad. | Definir entrada y resultado propios de aplicación para el cierre y mapearlos en OperacionController; conservar PosicionRequest y ReciboDto en HTTP. Mantener validaciones, propiedad, transacción y orden de efectos. Verificar respuestas y códigos HTTP, recibo, cierre repetido y rechazo de viaje ajeno. |
| H-02<br>Riesgo arquitectónico<br>Pendiente | OperacionService.finalizar (15, 37–39, 57 y 63–74); ViajeRepository.bloquear (13–15) y BicicletaRepository.findByIdParaActualizar (19–21).<br>«f.get(props.getIotTimeoutMs(),​TimeUnit.MILLISECONDS)»<br>La espera del IoT ocurre en la transacción con ambas filas bloqueadas. Tres esperas de hasta 5 s pueden sumar 15 s, sin contar otros costos. Es una cota de esperas configuradas, no latencia medida. Afecta concurrencia y resiliencia. | Instrumentar el tiempo de bloqueo y probar dos cierres concurrentes con candado lento. Si se confirma el impacto, diseñar confirmación en transacciones cortas con estado intermedio y comando idempotente; no retirar bloqueos sin rediseñar la consistencia. Verificar confirmación tardía, fallo, doble envío y reintento. |
| H-03<br>Defecto frente a SWR-05.1<br>Pendiente | frontend/src/components/FinalizarViaje.tsx, selección manual (34) y envío (25–26); web/dto/PosicionRequest.java (3–6); OperacionService.finalizar (59–62).<br>«lat: est.latitud, lng: est.longitud, precision: 0»<br>El modo manual envía la posición de la estación sin lectura GPS. El servidor valida rangos y cercanía de las coordenadas recibidas, sin distinguir procedencia. La aceptación en el simulador no prueba presencia física. Afecta integridad de ubicación. | Separar explícitamente el modo de demostración del modo operativo y exigir una fuente confiable de ubicación para este último. Tratarlo como corrección funcional separada. Caracterizar primero la devolución manual; probar que la ruta operativa rechace coordenadas sin respaldo, sin retirar la simulación académica. |

Rutas Java abreviadas en la matriz: backend/src/main/java/com/bikego/. H-03 se verificó por inspección; no se ejecutó una prueba de presencia física ni se evaluó un entorno operativo.

## V. Priorización e intervención

Escala de 1 (bajo) a 3 (alto). Impacto valora el efecto técnico; riesgo valora la probabilidad de alterar el comportamiento al intervenir; esfuerzo estima el alcance del cambio.

H-01: 2 / 1 / 2. —Primera intervención: corrige una frontera de capas con un cambio local y un contrato HTTP estable.

H-02: 3 / 3 / 3. —Medir antes de intervenir: afecta concurrencia y coordinación con el candado.

H-03: 3 / 3 / 3. —Prioritario para operación real. Requiere una decisión sobre telemetría y separar la demostración; cambiaría el comportamiento.

Antes: OperacionController pasa PosicionRequest a OperacionService, que devuelve ReciboDto. Después propuesto: el controlador convierte la solicitud en una entrada de aplicación y traduce el resultado a la misma respuesta HTTP. El caso de uso conserva el control de propiedad, la selección de zona y la misma transacción. No se moverán las esperas, bloqueos ni reglas de cobro en este cambio.

Archivos previstos: OperacionController.java, OperacionService.java, tipos de aplicación para el cierre y pruebas afectadas por la firma. ReciboDto y PosicionRequest conservan el contrato externo. La migración se limita a finalizar; las otras dependencias de OperacionService hacia web no quedarán resueltas con este primer paso.

## VI. Verificación y límites

Línea base ejecutada el 5 de noviembre de 2026: mvn -o -f backend/pom.xml test. Resultado: 65 pruebas, 0 fallos, 0 errores, 0 omitidas; 22 corresponden a OperacionIntegrationTest. Las pruebas usaron H2 en memoria y adaptadores de prueba. No se ejecutaron ensayos con GPS físico, candados reales ni mediciones de contención.

Comparación posterior: repetir la misma suite y caracterizar el JSON y los errores del endpoint antes de intervenir. Mantener total y cierre repetido (OperacionIntegrationTest, 59–71); recargo y límite de cuatro horas (72–82); zona, tolerancia y fallo IoT (83–99); precisión inválida (125–131); y rechazo de cierre ajeno (179–185). Agregar la comprobación de preservación del estado de mantenimiento donde corresponda.

El resultado posterior y el diff de la intervención quedan pendientes. Los 65 resultados corresponden al estado anterior, no a una demostración de equivalencia después del cambio.

## VII. Cierre y trazabilidad

Una revisión de dependencias antes de integrar el caso de uso habría detectado el acoplamiento entre aplicación y web. Una prueba de contrato del cierre con dos solicitudes concurrentes y candado lento, junto con pruebas que distingan GPS y modo manual, habría hecho visibles los otros dos riesgos antes de la entrega.

Repositorio: github.com/Jose15z/bikego-prototipo. La ruta documental del taller se deja en docs/refactorizacion/DIAGNOSTICO_REFACTORIZACION.md. La revisión parte del commit a1962ab; quedan pendientes los commits de diagnóstico, intervención y verificación posterior. No se publicó ningún cambio.

## Referencias

[1] BikeGo, repositorio bikego-prototipo, revisión a1962ab, 2026. github.com/Jose15z/bikego-prototipo.

[2] BikeGo, Arquitectura de Software, documento de entrega Parcial 2, descripción de capas y decisiones de diseño.

[3] BikeGo, Matriz de trazabilidad de 42 requisitos, SWR-05.1 a SWR-05.5 y SWR-06.1, soporte de Parcial 2.

[4] Universidad del Quindío, Símbolos, logotipo institucional. www.uniquindio.edu.co/publicaciones/224/simbolos/.
