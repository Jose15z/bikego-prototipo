# Trazabilidad de los 42 requisitos de BikeGo

Fuente: `seguimiento/Parcial 1/Plan_de_Requisitos_V3_BikeGo.xlsx`, hoja **Priorización de Requerimientos**. Se revisaron también Resumen V3, Definicion, PBI Jira e Historial de versiones. Fecha de revisión: 2026-10-19.

Los 42 SWR tienen implementación en el prototipo local. Esto no certifica operación con hardware o proveedores reales: pago, candado y entrega de correo usan adaptadores de demostración. La columna de verificación distingue pruebas automáticas de inspección manual y límites de integración. El libro original se conserva sin alterar.

## Alcance de los adaptadores y decisiones verificables

- **Pagos:** simulador tokenizado sin cargos bancarios. Cada intento de verificación/cobro tiene límite de cinco segundos y hasta dos reintentos ante fallo técnico. Rechazos comerciales no se reintentan automáticamente. Clave de idempotencia persistida por viaje y bloqueo de fila para impedir dobles cargos concurrentes.
- **Correo:** entrega persistente a una bandeja local, visible en Cuenta y pagos. Los operadores reciben alertas en Centro de flota y en la consola. La bandeja se actualiza cada 30 segundos. No se envían correos por Internet; para eso se sustituye el adaptador NotificadorVerificacion/NotificadorOperaciones.
- **Ubicación:** GPS del navegador como sustituto del GPS de la bicicleta en el prototipo. El mapa exige precisión ≤50 m; devolución ≤30 m. Existe un selector explícito de posición simulada para pruebas. En producción las coordenadas deben provenir de telemetría IoT autenticada.
- **Zonas:** polígonos rectangulares de 40 × 40 m centrados en las seis estaciones de demostración; aceptación dentro del polígono o hasta 30 m de su borde. No representan delimitaciones municipales oficiales.
- **Tarifas:** valores de prueba configurables: $1.500 base, $250 por minuto iniciado y $10.000 por duración estrictamente mayor a cuatro horas. Los valores del recibo se congelan al finalizar; el indicador conRecargo conserva la condición de duración.
- **Estado con incidente:** el viaje conserva EN_CURSO/FINALIZADO para poder cerrarse de forma segura, con conIncidente=true y cobro EN_REVISION. Solo el operador puede revisar, anular o autorizar el cobro.
- **Rendimiento:** pruebas y recorrido manual locales. Las metas de tiempo de red, correo externo y hardware requieren validación con los proveedores del despliegue. No se verificó una cámara física ni un candado real.

## Matriz requisito → implementación → evidencia

| SWR | Requisito del plan | Implementación local | Verificación |
|---|---|---|---|
| SWR-01.1 | El software debe solicitar el permiso de ubicación y obtener las coordenadas GPS del dispositivo con precisión ≤ 50 m. Si el permiso es denegado, debe permitir seleccionar manualmente una estación como punto de búsqueda. | MapaScreen; GET /api/estaciones | ApiIntegrationTest.estaciones + inspección UI del selector sin permiso GPS |
| SWR-01.2 | El software debe mostrar en el mapa solo las bicicletas con estado 'Disponible' y nivel de batería ≥ 20 %, dentro de un radio inicial de 1 km, ampliable por el usuario hasta 5 km (RN-002, RN-003, RN-005). | DisponibilidadService.buscarCercanas | DisponibilidadServiceTest.filtraEstadoYBateria, radioInicial, radioMaximo |
| SWR-01.3 | Para cada bicicleta el software debe mostrar: ID, nivel de batería (%), estación o ubicación, distancia aproximada al usuario y autonomía estimada. La autonomía estimada se calcula como porcentaje de batería × autonomía máxima de la flota (parámetro configurable). | BicicletaDto; DetalleScreen | DisponibilidadServiceTest.datosDeCadaBicicleta + inspección UI |
| SWR-01.4 | El software debe actualizar la disponibilidad al abrir el mapa y al pulsar 'Actualizar ubicación', con un tiempo de respuesta ≤ 3 s en condiciones normales de red. | MapaScreen; DisponibilidadService | DisponibilidadServiceTest.rendimiento |
| SWR-01.5 | Cuando no exista ninguna bicicleta disponible en el radio de búsqueda, el software debe informarlo con un mensaje claro y ofrecer ampliar el radio. | DisponibilidadService; MapaScreen | DisponibilidadServiceTest.sinResultados |
| SWR-01.6 | Al seleccionar una bicicleta, el software debe volver a validar su disponibilidad en el servidor antes de mostrar el detalle; si ya no está disponible, debe informarlo y regresar al mapa (Flujo alterno 2). | DisponibilidadService.detalleDisponible; DetalleScreen | DisponibilidadServiceTest.revalidaDetalle; ApiIntegrationTest.detalleNoDisponible |
| SWR-02.1 | El software debe leer el código QR con la cámara del dispositivo y extraer el ID de la bicicleta. Si el QR es ilegible o no corresponde a una bicicleta registrada, debe mostrar un error y permitir reintentar o ingresar el ID manualmente. El QR contiene el texto BIKEGO:<ID de bicicleta>. | QrParser; QrScanner; EscanearScreen | AlquilerServiceTest.qr + recorrido UI por ID manual; cámara depende del dispositivo |
| SWR-02.2 | El software debe verificar que el ID leído coincida con la bicicleta seleccionada en el mapa y que su estado sea 'Disponible'; de lo contrario debe rechazar el inicio e informar el motivo (RN-002, RN-003). | AlquilerService.validarEstadoBicicleta | AlquilerServiceTest.validaBicicleta |
| SWR-02.3 | El software debe verificar que la cuenta del usuario esté activa y que no tenga viajes en curso ni deudas pendientes (RN-001). | AlquilerService.validarCuenta | AlquilerServiceTest.validaCuenta; OperacionIntegrationTest.dosAlquileresSimultaneosDelMismoUsuarioSoloCreanUnViaje |
| SWR-02.4 | El software debe consultar a la pasarela de pago que el método registrado esté vigente y autorizado. Si es rechazado, debe informar el motivo y ofrecer actualizar el método de pago sin salir del flujo (Flujo alterno 1). | PagoSeguro; UsuarioService; MetodoPagoForm | AlquilerServiceTest.pagoRechazado, actualizarMetodoPago |
| SWR-02.5 | Solo cuando las validaciones 02.2 a 02.4 sean exitosas, el software debe enviar la orden de desbloqueo al candado IoT y esperar confirmación en ≤ 5 s; si no la recibe, debe reintentar hasta 2 veces y, de persistir, notificar el fallo y no iniciar el viaje. | AlquilerService.desbloquearConReintentos | AlquilerServiceTest.candadoSinRespuesta |
| SWR-02.6 | Al recibir la confirmación de desbloqueo, el software debe crear el registro del viaje (idViaje, idUsuario, idBicicleta, horaInicio, estado 'En curso') y cambiar el estado de la bicicleta a 'En uso' (RN-006). | AlquilerService.iniciar; Viaje; Bicicleta | AlquilerServiceTest.caminoFeliz, desapareceDelMapa |
| SWR-02.7 | Si la pasarela rechaza el pago, el software debe registrar el rechazo y aplicar al usuario el bloqueo temporal definido por RN-008 (duración parametrizable por el administrador). Si el usuario registra un nuevo método de pago y la pasarela lo autoriza, el bloqueo se levanta y puede continuar el alquiler (v0.3). | RechazoPago; UsuarioService; AlquilerService | AlquilerServiceTest.pagoRechazado, actualizarMetodoPago |
| SWR-02.8 | Tras iniciar el viaje, el software debe mostrar la pantalla 'Viaje en curso' con tiempo transcurrido, distancia y costo acumulado, en un máximo de 2 s después de la confirmación del candado. | ViajeScreen; App (recuperación de viaje) | AlquilerServiceTest.viajeEnCurso; OperacionIntegrationTest.historialRecuperaViajeActivoDelUsuario + recarga en navegador |
| SWR-03.1 | El software debe solicitar nombre completo, documento de identidad, correo, teléfono, fecha de nacimiento, dirección y contraseña, y validar formato y obligatoriedad de cada campo antes de enviar. | AuthScreen; RegistroRequest | RequisitosRegistroNotificacionesTest.apiRechazaCamposInvalidosYTerminosNoAceptados |
| SWR-03.2 | El software debe rechazar el registro cuando el correo o el documento ya existan en la plataforma, indicando el campo duplicado. | AutenticacionService; restricciones únicas Usuario | AutenticacionServiceTest.rechazaCorreoYDocumentoDuplicados |
| SWR-03.3 | El software debe enviar un código de verificación al correo o teléfono y activar la cuenta solo cuando el código sea confirmado en un plazo máximo de 15 minutos. | AutenticacionService; NotificadorVerificacion; BandejaLocal | RequisitosRegistroNotificacionesTest.codigoIncorrectoYVencidoNoActivanCuentaYSePuedeReenviar |
| SWR-03.4 | El software debe exigir la aceptación de términos y condiciones y registrar fecha y versión aceptada. | Usuario.fechaAceptacionTerminos; versionTerminos | RequisitosRegistroNotificacionesTest.codigoSeEntregaYTerminosQuedanRegistrados |
| SWR-03.5 | El software debe permitir registrar un método de pago durante el registro o posponerlo, y marcar la cuenta 'Sin método de pago' hasta que exista uno válido (RN-001). | AuthScreen (tarjeta opcional); MetodoPagoForm; CuentaScreen | RequisitosRegistroNotificacionesTest.codigoSeEntregaYTerminosQuedanRegistrados; AlquilerServiceTest.actualizarMetodoPago |
| SWR-04.1 | El software debe enviar a la pasarela una solicitud de verificación (tokenizada, sin almacenar el número completo de la tarjeta) y recibir respuesta en ≤ 5 s. | PagoSeguro; PasarelaPagoClient; UsuarioService | AlquilerServiceTest.actualizarMetodoPago; PagoSeguro aplica timeout de 5 s |
| SWR-04.2 | El software debe rechazar la validación cuando el usuario tenga pagos pendientes, aunque el método sea válido (RN-001). | UsuarioService.validar; AlquilerService.validarCuenta | OperacionIntegrationTest.deudaImpideValidacionYTarjetaRechazadaNoSeGuarda |
| SWR-04.3 | El software debe mostrar al usuario el motivo del rechazo (vencida, fondos insuficientes, deuda, error de pasarela) y la opción 'Actualizar método de pago'. | CuentaScreen; EscanearScreen; MetodoPagoForm | AlquilerServiceTest.pagoRechazado; OperacionIntegrationTest.deudaImpideValidacionYTarjetaRechazadaNoSeGuarda |
| SWR-04.4 | Ante un rechazo, el software debe registrar el evento y aplicar el bloqueo temporal de RN-008 por un tiempo parametrizable por el administrador. | RechazoPago; UsuarioService; GET /usuarios/me/rechazos | AlquilerServiceTest.pagoRechazado; OperacionIntegrationTest.rechazoRegistraDeudaUnaVezYPermiteSaldarla |
| SWR-04.5 | Si la pasarela no responde, el software debe reintentar 2 veces y luego informar 'Servicio de pago no disponible' sin autorizar el alquiler. | PagoSeguro (5 s por intento, tres intentos) | OperacionIntegrationTest.pasarelaSinServicioReintentaTresVecesSinAlquilar |
| SWR-05.1 | El software debe comparar la ubicación GPS de la bicicleta con los polígonos de zonas autorizadas y permitir 'Finalizar viaje' solo si está dentro (tolerancia ≤ 30 m). | OperacionService.zonas/finalizar; PosicionRequest; FinalizarViaje | OperacionIntegrationTest.fueraDeZonaNoBloqueaNiFinaliza, toleranciaDeZonaNoExcede30MetrosDelBorde, validaPrecisionYCoordenadasEnApi |
| SWR-05.2 | Cuando la bicicleta esté fuera de zona, el software debe indicar la zona autorizada más cercana y su distancia (RN-004). | OperacionService.zonas; FinalizarViaje | OperacionIntegrationTest.fueraDeZonaNoBloqueaNiFinaliza |
| SWR-05.3 | El software debe enviar la orden de bloqueo al candado IoT y esperar confirmación en ≤ 5 s; sin confirmación tras 3 intentos, el viaje sigue 'En curso' y se notifica al usuario y al operador. | CandadoIotClient.enviarBloqueo; OperacionService; BandejaLocal | OperacionIntegrationTest.fallaCandadoMantieneViajeYNotifica |
| SWR-05.4 | Al confirmar el bloqueo, el software debe registrar horaFin, ubicación final y duración en minutos, y cambiar el viaje a 'Finalizado' y la bicicleta a 'Disponible' (RN-006). | Viaje.finalizar; Bicicleta.devolver | OperacionIntegrationTest.finalizaCongelaCostoYNoCobraDosVeces |
| SWR-05.5 | Si la duración supera 4 horas, el software debe marcar el viaje con recargo antes de enviarlo al cálculo de pago (RN-007). | Viaje.conRecargo; OperacionService; BikeGoProperties | OperacionIntegrationTest.calculaTarifasDe45Y250Minutos, exactamenteCuatroHorasNoTieneRecargo |
| SWR-06.1 | El software debe calcular el total como tarifa base + (minutos de uso × tarifa por minuto) + recargos, usando las tarifas vigentes configuradas por el administrador. | Viaje.finalizar; tarifas aplicadas persistidas | OperacionIntegrationTest.finalizaCongelaCostoYNoCobraDosVeces, calculaTarifasDe45Y250Minutos |
| SWR-06.2 | El software debe enviar el cobro a la pasarela y registrar el pago como 'Procesado' o 'Pendiente' según la respuesta, sin volver a cobrar un viaje ya procesado. | OperacionService.pagar; bloqueo de fila; clave idempotente | OperacionIntegrationTest.cobrosConcurrentesSoloLlamanUnaVezALaPasarela |
| SWR-06.3 | Ante un rechazo de la pasarela, el software debe registrar la deuda en la cuenta del usuario y aplicar RN-008. | OperacionService.registrarDeuda; RechazoPago | OperacionIntegrationTest.rechazoRegistraDeudaUnaVezYPermiteSaldarla |
| SWR-06.4 | El software debe generar un recibo digital con número consecutivo, fecha, ID de viaje, tiempo total, tarifa base, recargos y total, enviarlo al correo y dejarlo disponible en la app. | ReciboDto; ReciboScreen; NotificadorOperaciones; BandejaMensajes | RequisitosRegistroNotificacionesTest.reciboSeEntregaEnBandejaPrivadaConTodosLosDatos |
| SWR-06.5 | El software debe mostrar el resumen del cobro en la app en ≤ 3 s tras la confirmación de la pasarela. | FinalizarViaje; ReciboScreen | Inspección del recorrido UI: devolución → pago procesado → recibo; rendimiento externo requiere medición en despliegue |
| SWR-07.1 | El software debe permitir crear un reporte con ID consecutivo, fecha y hora, ID de bicicleta, ubicación GPS, tipo de incidente (lista) y descripción, desde la pantalla 'Viaje en curso' o desde el detalle de la bicicleta. | IncidenteForm; EventoFlota; POST /incidentes | OperacionIntegrationTest.incidenteRetiraBiciYOperadorPuedeAnularCobro |
| SWR-07.2 | El software debe notificar al operador de flota en ≤ 1 minuto con el ID de bicicleta, tipo de incidente y ubicación. | BandejaLocal; BandejaMensajes; FlotaScreen (actualización cada 30 s) | RequisitosRegistroNotificacionesTest.alertaDeOperadorSeEntregaInmediatamente |
| SWR-07.3 | Cuando el tipo de incidente sea 'Daño grave' o 'Candado bloqueado', el software debe cambiar la bicicleta a 'En mantenimiento' y retirarla del mapa (RN-003). | OperacionService.crear; Bicicleta.mantenimiento | OperacionIntegrationTest.incidenteRetiraBiciYOperadorPuedeAnularCobro |
| SWR-07.4 | Si el incidente se reporta durante un viaje, el software debe permitir al operador marcar el viaje como 'Con incidente' para revisar o anular el cobro. | OperacionService.revisar; Viaje.conIncidente; FlotaScreen | OperacionIntegrationTest.incidenteRetiraBiciYOperadorPuedeAnularCobro, revisionPuedeAutorizarCobroDespuesDeFinalizar |
| SWR-08.1 | El software debe permitir al técnico crear un mantenimiento con ID, fecha, tipo (preventivo/correctivo), descripción y bicicleta asociada, cambiando la bicicleta a 'En mantenimiento'. | OperacionService.crear; FlotaScreen | OperacionIntegrationTest.mantenimientoValidaBateriaYGuardaResponsable, noSePuedeMantenerUnaBiciConViajeInclusoSiTieneIncidente |
| SWR-08.2 | El software debe permitir cerrar el mantenimiento y devolver la bicicleta a 'Disponible' solo si su nivel de batería es ≥ 20 % (RN-005). | OperacionService.cerrarMantenimiento | OperacionIntegrationTest.mantenimientoValidaBateriaYGuardaResponsable |
| SWR-08.3 | El software debe mostrar el historial de mantenimientos de cada bicicleta ordenado por fecha, con técnico responsable. | EventoFlotaRepository; FlotaScreen (filtro por bicicleta) | OperacionIntegrationTest.historialDeMantenimientoEstaOrdenadoPorFechaYConResponsable |
| SWR-08.4 | El software debe restringir el registro de mantenimientos a usuarios con rol 'Técnico' u 'Operador de flota'. | OperacionService.personal; Usuario.rol; App | OperacionIntegrationTest.clienteNoPuedeAdministrarNiLeerViajesAjenos, tecnicoPuedeMantenerPeroNoRevisarCobros |

## Ejecución de pruebas

```bash
cd backend
mvn test
cd ../frontend
npm install
npm run build
```

Verificación final: **50 pruebas JUnit, 0 fallos, 0 errores**; `npm run build` completado sin advertencias de tamaño tras separar mapa y lector QR en módulos de carga diferida.

La suite incluye autenticación, disponibilidad, alquiler, cierre, pagos, incidentes, mantenimiento, aislamiento por usuario, roles, notificaciones y concurrencia. Los informes JUnit se generan en `backend/target/surefire-reports/`.

## Recorrido manual

1. Iniciar sesión como `laura@correo.com`, contraseña `BikeGo123!`.
2. Explorar → seleccionar una estación → elegir bicicleta → Ingresar ID → desbloquear.
3. Recargar la página y comprobar que reaparece el viaje activo.
4. En devolución, comprobar GPS o usar el simulador local → elegir estación → Finalizar viaje y pagar.
5. Revisar recibo, Mis viajes y la notificación en Cuenta y pagos.
6. Crear una cuenta nueva, verificar el código local y elegir tarjeta ahora o posponer.
7. Reportar un incidente desde detalle/viaje usando GPS. Con operador, revisar o anular su cobro.
8. Como técnico u operador, registrar mantenimiento y comprobar que 15 % impide el cierre y 60 % habilita la bicicleta.
9. Filtrar el historial de operaciones por bicicleta y comprobar fechas y responsable.
