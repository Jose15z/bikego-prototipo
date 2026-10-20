# BikeGo

Aplicación de Ingeniería de Software II (Universidad del Quindío) para el alquiler de bicicletas eléctricas. Partió del prototipo del Parcial 1 y ahora incorpora registro y autenticación persistentes:

- F-01 Consultar bicicletas disponibles cercanas (PBI-02, Jira BG-2)
- F-02 Iniciar alquiler de bicicleta mediante código QR (PBI-01, Jira BG-1)
- F-03 Registrar usuario en la plataforma (PBI-03, Jira BG-3), con verificación, reenvío de código, inicio/cierre de sesión y tarjeta opcional
- F-04 Validar método de pago, rechazos, bloqueos y reintentos
- F-05 Finalizar viaje en zona habilitada con confirmación del candado
- F-06 Calcular y procesar pago, deuda, recargo y recibo digital
- F-07 Reportar incidentes y revisar/anular cobros como operador
- F-08 Registrar y cerrar mantenimientos con permisos de técnico/operador

La matriz completa de los **42 SWR** está en [docs/TRAZABILIDAD_REQUISITOS.md](docs/TRAZABILIDAD_REQUISITOS.md), con código, pruebas y límites de las integraciones simuladas. La interfaz se adapta a móvil y escritorio e incluye historial, recuperación del viaje activo, cuenta/pagos, notificaciones y panel de flota.

Autores: Jose Manuel Rodriguez Ospina y Juan Esteban Gutierrez Higuita

## Estructura

```
bikego-prototipo/
├── backend/    API REST en Java 17 + Spring Boot 3.3 + H2 persistente
├── frontend/   Aplicación React 18 + TypeScript + Vite (vista móvil)
└── docs/       QR de prueba y colección de peticiones HTTP
```

El backend incluye adaptadores locales para los sistemas externos del Student Book: pasarela de pago, plataforma IoT y envío del código de verificación. Están detrás de interfaces para poder sustituirlos por proveedores reales sin cambiar la lógica de negocio. Las tablas CRC de F-03 están en `docs/CRC_F03_Registro_Autenticacion.md`.

## Requisitos para correrlo

- JDK 17 o superior
- Maven 3.9 o superior (o abrir `backend/` en IntelliJ IDEA, que trae Maven)
- Node.js 18 o superior con npm
- Conexión a internet para descargar dependencias y para las teselas del mapa (OpenStreetMap)

## Cómo correrlo

En una terminal:

```bash
cd backend
mvn spring-boot:run
```

Si el puerto 8080 ya está ocupado, puedes usar otro puerto sin tocar el código:

```bash
cd backend
BIKEGO_PORT=8081 mvn spring-boot:run
```

El API queda en http://localhost:${BIKEGO_PORT:-8080}. Los datos se guardan en `backend/data/` y sobreviven a los reinicios. La consola de H2 está en http://localhost:${BIKEGO_PORT:-8080}/h2-console (JDBC URL `jdbc:h2:file:./data/bikego`, usuario `sa`, sin contraseña).

En otra terminal:

```bash
cd frontend
npm install
npm run dev
```

Abrir http://localhost:5173. Vite redirige las llamadas `/api` al backend.

Para correr las pruebas automatizadas del backend:

```bash
cd backend
mvn test
```

## Registro e inicio de sesión

La pantalla inicial permite crear una cuenta, verificarla, iniciar sesión y cerrarla. En desarrollo el código de verificación se muestra en pantalla, se registra en la bandeja local y en el log del backend. Tras verificar se ofrece registrar una tarjeta o posponerlo. En un despliegue se debe configurar `BIKEGO_MOSTRAR_CODIGO=false` y conectar una implementación real de `NotificadorVerificacion`.

Las cuentas iniciales usan la contraseña `BikeGo123!`; sirven para reproducir los escenarios del plan. Por ejemplo, `laura@correo.com` cubre el camino feliz. La aplicación no permite seleccionar otro usuario: la identidad siempre se obtiene de la sesión.

## Datos iniciales

Las estaciones están en Armenia (Quindío) con coordenadas aproximadas. Si el navegador no está cerca de Armenia, o si se niega el permiso de ubicación, se elige una estación como punto de búsqueda (es lo que pide SWR-01.1).

| Bicicleta | Estación | Estado | Batería | Para qué sirve |
|---|---|---|---|---|
| BG-001 | Plaza de Bolívar | Disponible | 85 % | Camino feliz |
| BG-002 | Plaza de Bolívar | Disponible | 15 % | No aparece en el mapa (RN-005) |
| BG-003 | Plaza de Bolívar | En mantenimiento | 90 % | No aparece (RN-003); si se escanea, rechazo |
| BG-004 | C.C. Unicentro | Disponible | 62 % | Segunda opción |
| BG-005 | C.C. Unicentro | En uso | 70 % | No aparece (RN-002) |
| BG-006 | Universidad del Quindío | Disponible | 100 % | Prueba de concurrencia |
| BG-007 | Universidad del Quindío | Disponible | 45 % | |
| BG-008 | Parque de la Vida | Disponible | 33 % | El candado no responde (SWR-02.5) |
| BG-009 | Estadio Centenario | Disponible | 78 % | |
| BG-010 | Terminal de Transportes | Disponible | 20 % | Límite exacto, sí aparece |
| BG-011 | Terminal de Transportes | Disponible | 19 % | No aparece |
| BG-012 | Parque de la Vida | En mantenimiento | 50 % | No aparece |

| Usuario | Escenario |
|---|---|
| U-001 Laura Gómez | Cuenta activa, sin deuda, tarjeta vigente |
| U-002 Carlos Pérez | Deuda pendiente de $8.500 (RN-001) |
| U-003 Valentina Ríos | Tarjeta vencida, la pasarela rechaza (RN-008) |
| U-004 Andrés Castaño | Cuenta suspendida |
| U-005 Sofía Marín | Fondos insuficientes |
| U-006 Mateo Londoño | Segundo usuario válido |

Para probar el QR hay tres caminos: la cámara, una imagen o el ID impreso (flujo alterno de SWR-02.1). Los códigos están en `docs/qr-prueba/`. Su contenido es `BIKEGO:<ID>`, por ejemplo `BIKEGO:BG-001`.

Para una tarjeta nueva en "Actualizar método de pago" sirve `4111 1111 1111 1111` con un vencimiento futuro. Una tarjeta terminada en `0002` simula fondos insuficientes y un vencimiento pasado simula tarjeta vencida.

## Trazabilidad requisito → código → prueba

| SWR | Dónde se implementa | Prueba automatizada |
|---|---|---|
| SWR-01.1 | `MapaScreen.tsx` (geolocalización, selector de estación), `GET /api/estaciones` | `ApiIntegrationTest.estaciones` |
| SWR-01.2 | `DisponibilidadService.buscarCercanas`, `Bicicleta.estaDisponible` | `DisponibilidadServiceTest.filtraEstadoYBateria`, `radioInicial`, `radioMaximo` |
| SWR-01.3 | `BicicletaDto`, `DetalleScreen.tsx` | `DisponibilidadServiceTest.datosDeCadaBicicleta` |
| SWR-01.4 | `MapaScreen.tsx` (botón Actualizar ubicación y medición del tiempo) | `DisponibilidadServiceTest.rendimiento` |
| SWR-01.5 | `DisponibilidadService` (mensaje y `puedeAmpliar`), botón Ampliar búsqueda | `DisponibilidadServiceTest.sinResultados` |
| SWR-01.6 | `DisponibilidadService.detalleDisponible`, `DetalleScreen.tsx` | `DisponibilidadServiceTest.revalidaDetalle`, `ApiIntegrationTest.detalleNoDisponible` |
| SWR-02.1 | `QrParser`, `QrScanner.tsx`, `EscanearScreen.tsx` | `AlquilerServiceTest.qr` |
| SWR-02.2 | `AlquilerService.validarEstadoBicicleta` | `AlquilerServiceTest.validaBicicleta` |
| SWR-02.3 | `AlquilerService.validarCuenta` | `AlquilerServiceTest.validaCuenta` |
| SWR-02.4 | `PasarelaPagoClient`, `UsuarioService.actualizarMetodoPago`, `MetodoPagoForm.tsx` | `AlquilerServiceTest.pagoRechazado`, `actualizarMetodoPago` |
| SWR-02.5 | `AlquilerService.desbloquearConReintentos`, `CandadoIotSimulado` | `AlquilerServiceTest.candadoSinRespuesta` |
| SWR-02.6 | `Viaje`, `Bicicleta.marcarEnUso` | `AlquilerServiceTest.caminoFeliz`, `desapareceDelMapa` |
| SWR-02.7 | `AlquilerService.iniciar` (registro `RechazoPago` y bloqueo) | `AlquilerServiceTest.pagoRechazado` |
| SWR-02.8 | `ViajeScreen.tsx`, `GET /api/viajes/{id}` | `AlquilerServiceTest.viajeEnCurso` |
| CA-7 | Bloqueo pesimista en `BicicletaRepository.findByIdParaActualizar` | `AlquilerServiceTest.concurrencia` |

## API

| Método | Ruta | Uso |
|---|---|---|
| GET | `/api/estaciones` | Estaciones para el selector manual |
| GET | `/api/bicicletas/cercanas?lat=&lng=&radio=` | Bicicletas disponibles en el radio (1000 a 5000 m) |
| GET | `/api/bicicletas/{id}?lat=&lng=` | Detalle con revalidación; 409 si ya no está disponible |
| POST | `/api/alquileres` | Inicia el alquiler autenticado: `{bicicletaSeleccionadaId, codigoQr}` |
| GET | `/api/viajes/{id}` | Estado del viaje en curso |
| POST | `/api/auth/registro` | Crear una cuenta pendiente de verificación |
| POST | `/api/auth/verificacion` | Confirmar el código de seis dígitos |
| POST | `/api/auth/login` | Crear una sesión de 24 horas |
| GET | `/api/auth/me` | Consultar el perfil autenticado |
| POST | `/api/auth/logout` | Revocar la sesión actual |
| PUT | `/api/usuarios/me/metodo-pago` | Nueva tarjeta del usuario autenticado |
| GET | `/api/config` | Parámetros de negocio vigentes |

Los errores de negocio responden con `{codigo, mensaje, accionSugerida}`. Por ejemplo, un pago rechazado devuelve 402 con `accionSugerida: "ACTUALIZAR_METODO_PAGO"`, que el frontend usa para mostrar el formulario de tarjeta sin salir del flujo. En `docs/peticiones.http` hay ejemplos listos para el cliente HTTP de IntelliJ o la extensión REST Client de VS Code.

## Parámetros

Están en `backend/src/main/resources/application.properties` con el prefijo `bikego`: radio inicial y máximo, batería mínima, tiempo de espera del candado y número de reintentos, minutos de bloqueo por pago rechazado (RN-008) y tarifas. Las tarifas son valores de prueba; el Plan de Requisitos no fija montos.

## Decisiones y límites del prototipo

- Conflicto entre SWR-02.4 y SWR-02.7: el primero pide ofrecer la actualización del método de pago sin salir del flujo y el segundo aplica un bloqueo temporal tras el rechazo. En v0.3 se decidió que el bloqueo se levanta cuando la pasarela autoriza el nuevo método. Queda registrado en el Plan de Requisitos como pendiente de validar con el cliente.
- La distancia recorrida sigue siendo una estimación. El cierre está implementado: valida GPS del navegador o posición expresamente simulada contra polígonos de prueba de las estaciones, con tolerancia de 30 m. El GPS real de la bicicleta requiere un adaptador IoT.
- La base local es H2 persistente y admite otra URL mediante `BIKEGO_DB_URL`; para producción se recomienda PostgreSQL con migraciones.
- El envío de verificación y recibos se entrega a una bandeja local persistente y al log; no utiliza correo por Internet. La interfaz `NotificadorVerificacion` está preparada para un proveedor real de correo o SMS.

## Capturas

En `docs/capturas/` están las pantallas del prototipo. El mapa de esas capturas es un fondo esquemático porque se tomaron en un entorno sin acceso a las teselas de OpenStreetMap; al correr la aplicación con internet se ve el mapa real.


## Roles y cuentas de demostración

| Rol | Correo | Contraseña local |
|---|---|---|
| Cliente | `laura@correo.com` | `BikeGo123!` |
| Operador de flota | `operador@bikego.local` | `BikeGo123!` |
| Técnico | `tecnico@bikego.local` | `BikeGo123!` |

Los registros públicos siempre crean clientes. Los roles se comprueban en el servidor; ocultar un botón no es la barrera de autorización. Las cuentas de personal se crean al iniciar la aplicación si aún no existen.

## API de cierre, cobro y operaciones

Todas las rutas, salvo zonas, requieren `Authorization: Bearer <token>`.

| Método | Ruta | Uso |
|---|---|---|
| POST | `/api/auth/reenviar` | Reenviar código con correo y contraseña |
| GET | `/api/viajes` | Historial propio, incluyendo viaje activo |
| GET | `/api/zonas?lat=&lng=` | Zonas ordenadas por distancia, con indicador dentro/fuera |
| POST | `/api/viajes/{id}/finalizar` | `{latitud, longitud, precision}`; precisión máxima 30 m |
| POST | `/api/viajes/{id}/pago` | Cobro idempotente o reintento de deuda del viaje |
| GET | `/api/viajes/{id}/recibo` | Recibo propio y estado del cobro |
| POST | `/api/usuarios/me/validar-pago` | Validar tarjeta y ausencia de deuda |
| GET | `/api/usuarios/me/rechazos` | Historial privado de rechazos |
| GET | `/api/mensajes` | Bandeja privada; el operador recibe además alertas de flota |
| POST | `/api/incidentes` | `{bicicletaId, tipo, descripcion, latitud, longitud}` |
| GET | `/api/eventos` | Incidentes propios; personal autorizado ve operaciones de flota |
| GET | `/api/flota` | Inventario para técnico/operador |
| POST | `/api/mantenimientos` | Mismo formato que incidente, tipo Preventivo/Correctivo |
| POST | `/api/mantenimientos/{id}/cerrar` | `{bateria}`; no permite menos de 20 % |
| POST | `/api/incidentes/{id}/revision` | Operador: `{accion: "REVISAR"}`, `ANULAR` o `COBRAR` |

Los tipos de incidente son `Daño grave`, `Candado bloqueado`, `Batería`, `Accidente` y `Otro`. La descripción requiere entre 10 y 2000 caracteres. No se inicia mantenimiento si la bicicleta tiene un viaje activo. Un incidente grave durante el viaje conserva el estado de mantenimiento después de la devolución.

## Tarifas, deuda y recibos

El costo se calcula con minutos iniciados: base + ceil(segundos / 60) × tarifa por minuto. Una duración estrictamente mayor de cuatro horas activa `conRecargo` y añade `bikego.recargo-largo` (por defecto $10.000; configurable mediante `BIKEGO_RECARGO_LARGO`). Los importes se conservan al cerrar el viaje y no cambian al consultar el historial. Los recibos tienen consecutivo `BG-00000001`, fecha, bicicleta, duración, tarifas, recargo, total y estado.

Un rechazo deja el pago pendiente, registra la deuda una sola vez y aplica el bloqueo temporal. Cambiar a una tarjeta autorizada permite reintentar el cobro desde el recibo; pagar salda esa deuda. Los cobros en revisión se mantienen sin procesar hasta que el operador resuelva el incidente.

## Entorno local y límites

Las pasarelas, candados y correos son simulados: no hay cargos bancarios, bloqueos físicos ni emails externos. Los recibos se entregan en la bandeja de Cuenta y pagos; las alertas aparecen en Centro de flota. Para una instalación real se deben conectar los puertos de pago/IoT/notificación y reemplazar las zonas de prueba por polígonos aprobados. Ver la matriz de trazabilidad para el detalle de cada requisito.

### Vista previa conservada de esta implementación

La vista previa de desarrollo usa `backend/data/bikego-preview.mv.db`; se conservaron las cuentas, sesiones y viajes que estaban en la base temporal. Esta base es independiente de `data/bikego.mv.db`, que sigue siendo la predeterminada. Para retomar los datos de la vista previa:

```bash
cd backend
BIKEGO_DB_URL=jdbc:h2:file:./data/bikego-preview mvn spring-boot:run
```

Los archivos de datos y respaldos están excluidos de Git.


## Entrega del Parcial 2

Los entregables de esta revisión se encuentran en `../seguimiento/Parcial 2/`. Incluyen arquitectura C4, plan V4, informe IEEE, Student Book, gestión Jira preparada y un producto ejecutable con interfaz integrada.

Para crear el ejecutable desde fuente:

```bash
cd frontend
npm install
npm run build
cd ../backend
mvn test package
java -jar target/bikego-backend-0.3.0.jar
```

El JAR incorpora `frontend/dist` como recursos estáticos. Abrir `http://127.0.0.1:8080`. La verificación del Parcial 2 aprobó 50 pruebas JUnit y 19 comprobaciones HTTP sobre el ejecutable integrado. La gestión remota en Jira requiere confirmar acceso y sincronizar las claves del proyecto; los CSV de la entrega no acreditan cambios remotos.

## Pruebas funcionales de interfaz

La suite reproducible de [tests/e2e](tests/e2e/README.md) recorre las ocho funcionalidades con 25 escenarios en navegador y genera capturas. Se ejecuta con bases temporales independientes de los datos de la vista previa. La revisión actual aprobó además las 50 pruebas JUnit y 19 verificaciones HTTP del JAR. Incluye una regresión del cambio rápido de pantalla después de ajustar el radio del mapa.

## Red de estaciones y códigos

52 estaciones y 520 bicicletas iniciales, diez por estación. La última ampliación agrega Aeropuerto El Edén (acceso terrestre de terminal nacional), Mall Paraíso · Club Campestre, Soledén, Universidad La Gran Colombia · Sede campestre La Santa María y Parque de Recreación Popular. Las nuevas son EST-48 a EST-52 y BG-471 a BG-520; las 47 estaciones y 470 bicicletas anteriores se conservan.

En **Explorar → Códigos de bicicletas**, filtra por estación y descarga los PNG; también están en docs/qr-prueba, con galería e índice. Los QR previos permanecen idénticos. Hay presencia en las diez comunas urbanas, además de los accesos del corredor del aeropuerto; no se certifica cobertura caminable universal ni infraestructura física instalada. Consulta [ubicaciones y cobertura](docs/ESTACIONES_Y_FLOTA_PRUEBA.md).

## Registro, pago y documentos legales

El registro requiere aceptar términos y autorizar datos por separado. Se guardan fecha y versión 2026-10-20 de ambas aceptaciones; los usuarios anteriores no se modifican retroactivamente. Los enlaces públicos /legal/terminos.html, /legal/privacidad.html y /legal/aviso.html están en registro y pie de página. Los contactos @bikego.example son ilustrativos y no reciben mensajes.

El número de tarjeta admite espacios o guiones y vencimiento MM/AA o MM/AAAA. El teléfono admite formato internacional con espacios, paréntesis y guiones. La tarjeta está oculta por defecto y se limpia al guardar. Los pagos, candados y correos siguen siendo simulados; usa 4111 1111 1111 1111 y una fecha futura, sin datos bancarios reales. Los errores indican cómo corregir o reintentar.

Verificaciones reproducibles: 65 JUnit; geometría de 52 estaciones y 520 bicicletas; lectura de 1042 PNG; registro y primer pago, consentimiento, recuperación de errores y mapa/QR en navegador. Consulta [tests/e2e](tests/e2e/README.md).

## Interfaz y accesos exteriores

Portada y acceso a cuenta con fotografías reales de Armenia, navegación adaptable y mapa con listado lateral. Los créditos de las fotografías están disponibles en la aplicación y en docs/FOTOGRAFIAS.md. Los puntos se sitúan junto a las vías de acceso y fuera de canchas y edificios. Pulsa un marcador para acercarte al acceso.
