# Pruebas de funcionalidades de BikeGo

La suite E2E contiene 25 escenarios que recorren las ocho funcionalidades desde la interfaz, autenticación, permisos y navegación móvil. Genera Resultados_UI.json y capturas por escenario. Las pruebas HTTP generan Resultados_HTTP.json. Ambas suites crean cuentas y modifican viajes y bicicletas: se ejecutan contra bases temporales independientes.

## 1 Iniciar el producto con base temporal para la interfaz

Desde la raíz bikego-prototipo, compila frontend y empaqueta el backend. Con los recursos disponibles:

```sh
npm --prefix frontend install
npm --prefix frontend run build
mvn -f backend/pom.xml test package
java -jar backend/target/bikego-backend-0.3.0.jar --server.port=8082 '--spring.datasource.url=jdbc:h2:mem:qa_ui;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=20000' --spring.h2.console.enabled=false
```

Espera el mensaje Tomcat started on port 8082. Mantén el servidor iniciado en una terminal y usa otra para las pruebas. En Windows adapta el entrecomillado a tu terminal.

## 2 Preparar el navegador de pruebas y ejecutar

```sh
cd tests/e2e
npm install
npx playwright install chromium
BIKEGO_TEST_URL=http://127.0.0.1:8082 node funcionalidades.cjs
```

Requiere Node.js compatible con Playwright. Si ya tienes un navegador compatible, puedes indicar su ejecutable en `BIKEGO_CHROME_PATH` en lugar de descargar Chromium. `BIKEGO_TEST_OUTPUT` cambia el directorio de resultados; por defecto es `/tmp/bikego-qa-actual/resultados`. `BIKEGO_QR_DIR` cambia la ubicación de las imágenes; por defecto usa `docs/qr-prueba` de este repositorio. Define una carpeta de salida válida si tu sistema no utiliza `/tmp`.

Para repetir la suite, detén y reinicia el servidor temporal. No reutilices una base que ya tenga los viajes o mantenimientos de una ejecución anterior. La cuenta inicial de demostración y la flota deben conservar su estado inicial.

## 3 Ejecutar las pruebas HTTP con otra base temporal

Desde la raíz, en una terminal independiente:

```sh
java -jar backend/target/bikego-backend-0.3.0.jar --server.port=8083 '--spring.datasource.url=jdbc:h2:mem:qa_http;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=20000' --spring.h2.console.enabled=false
```

Cuando termine de arrancar, en otra terminal:

```sh
python3 tests/e2e/http_funcionalidades.py
```

El script usa solo la biblioteca estándar de Python. `BIKEGO_TEST_URL` permite cambiar la URL (predeterminada http://127.0.0.1:8083). `BIKEGO_HTTP_OUTPUT` cambia la ruta del JSON de resultados (predeterminada `/tmp/bikego-qa-actual/Resultados_HTTP.json`); su carpeta debe existir. No ejecutes HTTP y UI sobre la misma base porque sus viajes cambian la disponibilidad de las bicicletas.

## Alcance

Las coordenadas GPS de las pruebas son controladas y los proveedores de pago, candado y correo son simulados. Se comprueba lectura QR desde una imagen e ingreso manual; no se certifica una cámara física. Las teselas externas se excluyen en la suite UI. La compilación y las aserciones del navegador verifican el producto real sin sustituir sus respuestas API por mocks.

## Mapa, QR y cobertura por comuna

La flota inicial contiene 520 bicicletas en 52 estaciones. Con el servidor temporal de puerto 8082 recién iniciado, `node mapa-estaciones.cjs` comprueba inventario, los 52 marcadores, diez códigos por estación, descarga/alquiler/devolución de BG-520 en Parque de Recreación Popular y compatibilidad de BG-154. Después puede ejecutarse la suite general de 25 escenarios sobre esa misma base.

`node verificar-qr.cjs` decodifica los 521 PNG de docs y los 421 de frontend/public y compara su contenido con el catálogo. `python3 accesos_estaciones.py` verifica accesos exteriores, canchas, edificios, puente La Florida y presencia de estaciones en las diez comunas, usando los polígonos independientes en ../fixtures. Ver ../../docs/ESTACIONES_Y_FLOTA_PRUEBA.md.

## Registro, primer pago y avisos legales

Con el servidor temporal en 8082, `node registro-pago-legal.cjs` prueba errores y foco, consentimiento de privacidad independiente, documentos públicos en pestaña aparte, registro con documento con guion y teléfono internacional, tarjeta oculta, recuperación de error 500 y primer pago con guiones y MM/AAAA. Verifica persistencia de sesión y cuenta en móvil y escritorio. Usa BIKEGO_LEGAL_OUTPUT para cambiar la carpeta de evidencias. Los documentos describen la simulación real y contactos ilustrativos .example.
