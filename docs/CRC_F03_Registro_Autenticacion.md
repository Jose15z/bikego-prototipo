# Tablas CRC — F-03 Registrar usuario y autenticar cuenta

## Contexto y alcance

BikeGo es un sistema de alquiler de bicicletas eléctricas en Armenia. El Plan de Requisitos V3 contiene ocho funcionalidades y 42 requisitos de software. El primer prototipo implementaba F-01 (consultar bicicletas cercanas) y F-02 (iniciar alquiler por QR), pero elegía un usuario de prueba y reiniciaba todos los datos al arrancar.

Se seleccionó F-03 porque cierra una condición necesaria de F-02: solo un usuario registrado, identificado, verificado y con una sesión válida puede iniciar un alquiler. El incremento cubre:

- SWR-03.1: datos obligatorios y validación de formato.
- SWR-03.2: rechazo de correo o documento duplicados.
- SWR-03.3: código de verificación de seis dígitos con vigencia de 15 minutos.
- SWR-03.4: aceptación, fecha y versión de términos.
- SWR-03.5: método de pago opcional al registrarse; sin uno no se puede alquilar.
- Extensión necesaria para una aplicación operativa: inicio/cierre de sesión, tokens revocables y autorización de alquileres y viajes.

## Tarjetas CRC

### CRC-01 — Usuario

| Responsabilidades | Colaboradores |
|---|---|
| Mantener identidad: nombre, documento, correo, teléfono, fecha de nacimiento y dirección. | AutenticacionService |
| Mantener el estado de la cuenta: pendiente de verificación, activa o suspendida. | UsuarioRepository |
| Conservar el hash de la contraseña, nunca la contraseña en texto plano. | SeguridadHash |
| Registrar fecha y versión de términos aceptados. | RegistroRequest |
| Determinar si el código de verificación sigue vigente y activar la cuenta al confirmarlo. | NotificadorVerificacion |
| Mantener deuda, bloqueo temporal y referencia tokenizada del método de pago. | UsuarioService, AlquilerService, PasarelaPagoClient |

### CRC-02 — AutenticacionService

| Responsabilidades | Colaboradores |
|---|---|
| Orquestar el registro y normalizar el correo. | RegistroRequest, Usuario |
| Verificar unicidad de correo y documento. | UsuarioRepository |
| Generar el código de verificación y definir su vencimiento. | SeguridadHash, Clock, BikeGoProperties |
| Solicitar el envío del código sin acoplar el dominio a un proveedor concreto. | NotificadorVerificacion |
| Confirmar el código y activar la cuenta. | Usuario, UsuarioRepository |
| Validar credenciales y estado de la cuenta. | SeguridadHash, UsuarioRepository |
| Crear tokens aleatorios, persistir sesiones, autenticar solicitudes y cerrar sesión. | Sesion, SesionRepository |

### CRC-03 — Sesion

| Responsabilidades | Colaboradores |
|---|---|
| Asociar una sesión con el usuario autenticado. | Usuario |
| Conservar únicamente el hash del token. | SeguridadHash |
| Registrar creación, vencimiento y revocación. | AutenticacionService, Clock |
| Informar si la sesión continúa activa. | SesionRepository |

### CRC-04 — UsuarioRepository

| Responsabilidades | Colaboradores |
|---|---|
| Persistir y recuperar usuarios. | Usuario |
| Buscar una cuenta por correo sin distinguir mayúsculas. | AutenticacionService |
| Detectar duplicados de correo y documento antes de registrar. | AutenticacionService |
| Entregar el usuario a los servicios de alquiler y perfil. | AlquilerService, UsuarioService |

### CRC-05 — SesionRepository

| Responsabilidades | Colaboradores |
|---|---|
| Persistir sesiones revocables y con vencimiento. | Sesion |
| Recuperar una sesión a partir del hash de su token. | AutenticacionService |

### CRC-06 — SeguridadHash

| Responsabilidades | Colaboradores |
|---|---|
| Derivar contraseñas con PBKDF2-HMAC-SHA256, salt aleatorio y 210.000 iteraciones. | AutenticacionService, Usuario |
| Comparar contraseñas sin guardar ni exponer el texto original. | AutenticacionService |
| Calcular SHA-256 de códigos y tokens antes de persistirlos. | AutenticacionService, Sesion |

### CRC-07 — NotificadorVerificacion

| Responsabilidades | Colaboradores |
|---|---|
| Definir el contrato para enviar códigos de verificación. | AutenticacionService |
| Permitir cambiar el adaptador local por correo, SMS u otro proveedor sin alterar el registro. | NotificadorVerificacionConsola, futuro adaptador de correo/SMS |

### CRC-08 — AutenticacionController

| Responsabilidades | Colaboradores |
|---|---|
| Publicar los casos de uso REST: registro, verificación, login, perfil y logout. | AutenticacionService |
| Validar la forma de las solicitudes antes de invocar el dominio. | RegistroRequest, VerificacionRequest, LoginRequest |
| Entregar respuestas sin exponer hashes, contraseña ni token de pago. | AuthDto, RegistroDto, UsuarioDto |

## Flujo de colaboración

1. `AutenticacionController` valida `RegistroRequest` y llama a `AutenticacionService`.
2. `AutenticacionService` consulta `UsuarioRepository`, deriva la contraseña con `SeguridadHash`, crea `Usuario` y solicita el código a `NotificadorVerificacion`.
3. Al verificar, el servicio compara el hash y ordena a `Usuario` pasar a estado `ACTIVA`.
4. Al iniciar sesión, el servicio valida contraseña y estado, crea una `Sesion` y devuelve el token una sola vez.
5. En cada alquiler, el backend obtiene el usuario desde el token; el cliente ya no puede escoger ni falsificar un `usuarioId`.

## Decisiones y límites

- La base H2 ahora es persistente (`backend/data/`) y puede sustituirse mediante `BIKEGO_DB_URL`. Para un despliegue multiinstancia se recomienda PostgreSQL y migraciones con Flyway.
- El adaptador incluido entrega el código en el registro del servidor. En entorno local también puede mostrarse en pantalla. En producción debe configurarse `BIKEGO_MOSTRAR_CODIGO=false` y reemplazar el adaptador por un proveedor de correo o SMS.
- La pasarela de pago y el candado siguen siendo adaptadores simulados, tal como estaba previsto en el Student Book. Sus interfaces permiten sustituirlos sin modificar `AlquilerService`.
- Las sesiones son opacas, revocables y de 24 horas. Para producción también se deben agregar límites de intentos, recuperación de contraseña, auditoría y gestión segura de secretos.
