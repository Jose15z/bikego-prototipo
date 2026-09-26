# BikeGo prototipo

Prototipo del Parcial 1 de Ingeniería de Software II (Universidad del Quindío). Implementa dos de las ocho funcionalidades del Plan de Requisitos v0.3 de BikeGo, el sistema de alquiler de bicicletas eléctricas mediante aplicación móvil:

- F-01 Consultar bicicletas disponibles cercanas (PBI-02, Jira BG-2)
- F-02 Iniciar alquiler de bicicleta mediante código QR (PBI-01, Jira BG-1)

Autores: Jose Manuel Rodriguez Ospina y Juan Esteban Gutierrez Higuita

## Estructura

```
bikego-prototipo/
├── backend/    API REST en Java 17 + Spring Boot 3.3 + H2 en memoria
├── frontend/   Aplicación React 18 + TypeScript + Vite (vista móvil)
└── docs/       QR de prueba y colección de peticiones HTTP
```

El backend incluye dos simuladores para los sistemas externos del Student Book: la pasarela de pago (modo sandbox) y la plataforma IoT del candado inteligente. Los dos están detrás de interfaces (`PasarelaPagoClient` y `CandadoIotClient`), así que cambiarlos por integraciones reales no toca la lógica de negocio.

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

El API queda en http://localhost:8080. La consola de H2 está en http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:bikego`, usuario `sa`, sin contraseña).

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

## Datos de prueba

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

No hay inicio de sesión en este alcance: el usuario de prueba se elige arriba en la pantalla. El botón "Reiniciar demo" deja los datos como al arrancar.

Para probar el QR hay tres caminos: la cámara del celular o del portátil, "Subir imagen" con una foto o captura del código, o "Ingresar ID" (el flujo alterno de SWR-02.1). Los códigos de prueba están en la pantalla "QR de prueba" y en `docs/qr-prueba/`. El contenido de cada código es `BIKEGO:<ID>`, por ejemplo `BIKEGO:BG-001`.

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
| POST | `/api/alquileres` | Inicia el alquiler: `{usuarioId, bicicletaSeleccionadaId, codigoQr}` |
| GET | `/api/viajes/{id}` | Estado del viaje en curso |
| GET | `/api/usuarios` | Usuarios de prueba |
| PUT | `/api/usuarios/{id}/metodo-pago` | Nueva tarjeta (se tokeniza, solo se guardan los últimos 4 dígitos) |
| GET | `/api/config` | Parámetros de negocio vigentes |
| POST | `/api/demo/reiniciar` | Restaura los datos de prueba |

Los errores de negocio responden con `{codigo, mensaje, accionSugerida}`. Por ejemplo, un pago rechazado devuelve 402 con `accionSugerida: "ACTUALIZAR_METODO_PAGO"`, que el frontend usa para mostrar el formulario de tarjeta sin salir del flujo. En `docs/peticiones.http` hay ejemplos listos para el cliente HTTP de IntelliJ o la extensión REST Client de VS Code.

## Parámetros

Están en `backend/src/main/resources/application.properties` con el prefijo `bikego`: radio inicial y máximo, batería mínima, tiempo de espera del candado y número de reintentos, minutos de bloqueo por pago rechazado (RN-008) y tarifas. Las tarifas son valores de prueba; el Plan de Requisitos no fija montos.

## Decisiones y límites del prototipo

- Conflicto entre SWR-02.4 y SWR-02.7: el primero pide ofrecer la actualización del método de pago sin salir del flujo y el segundo aplica un bloqueo temporal tras el rechazo. En v0.3 se decidió que el bloqueo se levanta cuando la pasarela autoriza el nuevo método. Queda registrado en el Plan de Requisitos como pendiente de validar con el cliente.
- La distancia del viaje en curso es una estimación con velocidad promedio. El seguimiento GPS real y el cierre del viaje hacen parte de F-05.
- No hay autenticación; el usuario se elige de una lista de prueba.
- La base de datos es H2 en memoria y se reinicia con cada arranque.

## Capturas

En `docs/capturas/` están las pantallas del prototipo. El mapa de esas capturas es un fondo esquemático porque se tomaron en un entorno sin acceso a las teselas de OpenStreetMap; al correr la aplicación con internet se ve el mapa real.
