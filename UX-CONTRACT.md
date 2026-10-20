# Contrato de interfaz de BikeGo

## Fuentes de negocio
- README.md: funcionalidades F-01 a F-08 y contratos API.
- backend/src/main/java/com/bikego/config/CatalogoDemostracion.java: sincronización no destructiva de la red.
- backend/src/main/resources/demo/catalogo.json: estaciones e identificadores persistentes.
- frontend/src/api.ts y backend/src/main/java/com/bikego/web: sesiones y permisos.
- docs/ESTACIONES_Y_FLOTA_PRUEBA.md: ubicaciones y distribución aprobadas para la ampliación.

Los permisos, tarifas y pagos simulados siguen sus servicios existentes. Esta revisión normaliza la entrada de tarjetas y añade consentimiento explícito de privacidad para registros nuevos.

## Canonical UI Resolution
| Capability | Canonical owner | Source of truth | Allowed variants | Verification |
|---|---|---|---|---|
| Select/Listbox | Select nativo de la galería y mapa | API de estaciones y contrato actual | native, popup del sistema aceptado | mapa-estaciones.cjs, teclado y selección móvil |
| Form | validarFormulario.ts y MetodoPagoForm.tsx | DTO RegistroRequest y MetodoPagoRequest; contrato API | alta y actualización; errores propios sin burbujas nativas | registro-pago-legal.cjs y funcionalidades.cjs |
| Date | input nativo de fecha de nacimiento | fecha ISO del registro | nativo, locale y popup del sistema aceptados | registro-pago-legal.cjs |
| Scrollbar | product.css y styles.css | tokens CSS existentes | documento, resultados laterales y diálogos ya existentes | pruebas de escritorio y móvil |

## Flujo de códigos
Explorar → Códigos de bicicletas → filtrar por estación → descargar QR. Volver al mapa conserva la ubicación de búsqueda en App. QrPruebaScreen es dueño de la carga del catálogo; muestra Cargando códigos y errores en role=alert. No crea ni cambia bicicletas. CodigoBicicleta presenta el PNG cuyo contenido es BIKEGO seguido del identificador persistente. La descarga lleva el nombre del identificador. Galería y archivo descargado usan el mismo recurso validado. La imagen tiene nombre accesible y dimensiones reservadas; los enlaces funcionan con teclado. El filtro nativo mantiene su etiqueta visible y acepta el popup de cada sistema operativo.

## Estado y navegación
Pantallas gestionadas por el estado de App; esta arquitectura no mantiene filtros en URL. No se añade navegación por rutas en una ampliación de datos. La galería usa desplazamiento del documento y un filtro por estación. No hay paginación remota: es un catálogo fijo de 520 identificadores de esta entrega. Los controles y etiquetas siguen los propietarios actuales; no se introducen notificaciones o diálogos adicionales.

## Verificación y límites de alcance
Las pruebas se ejecutan contra H2 temporal en 8082. La aplicación persistente en 8080 recibe solamente la migración autorizada y verificaciones de lectura. El manifiesto de auditoría de catálogo se limita al nuevo componente que presenta el recurso; no constituye una revisión de formularios de autenticación o pagos. La identidad visual y sus valores siguen product.css, descritos en DESIGN.md.

## Registro, pagos y documentos legales · 2026-10-20

| Capability | Canonical owner | Source of truth | Allowed variants | Verification |
|---|---|---|---|---|


La aceptación de términos y autorización de datos se solicitan por separado y sin preselección. Los nuevos registros conservan fecha y versión de cada aceptación; no se modifica retroactivamente a usuarios anteriores. Los documentos públicos son frontend/public/legal, abiertos en pestaña aparte mediante EnlacesLegales para conservar el formulario. El usuario autorizó correos genéricos: los dominios .example se identifican como ilustrativos, sin atención externa operativa. Se describe el proyecto académico y la simulación existente; no se afirma habilitación comercial ni infraestructura instalada.

MetodoPagoForm es el único propietario del ingreso de tarjeta en registro, cuenta y alquiler. Acepta espacios/guiones y MM/AA o MM/AAAA; el DTO normaliza antes de validar. El PAN está oculto por defecto, se limpia al guardar y no se persiste en el navegador. El éxito se anuncia solo después de confirmar el servidor. Errores conservan la entrada para corregir y reintentar. No se introducen reintentos automáticos de pagos.
