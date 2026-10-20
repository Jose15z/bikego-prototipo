---
version: alpha
name: BikeGo
description: Movilidad eléctrica en Armenia con fotografía urbana y un mapa operativo.
colors:
  primary: "#174e43"
  background: "#f8f9f5"
  text: "#202f2a"
  muted: "#738079"
  border: "#e3e7e1"
  focus: "#91bc9d"
typography:
  sans:
    fontFamily: 'Inter, ui-sans-serif, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif'
  display:
    fontFamily: 'Georgia, serif'
rounded:
  card: "18px"
  action: "50px"
spacing:
  content: "32px"
  page-max: "1384px"
components:
  qr:
    width: "160px"
    height: "160px"
---

# BikeGo: contexto de diseño

## Overview
La identidad vigente usa las calles reales de Armenia como referencia, con fotografías de Cielos Abiertos y avenida Bolívar. Público: personas que consultan, alquilan y devuelven bicicletas en Armenia, Colombia; personal de operación y mantenimiento. Idioma español, locale es-CO, horario America/Bogota. Uso en teléfono junto a una bicicleta y en computador para gestionar la flota. Registro híbrido: portada fotográfica; herramientas de mapa, cuenta y flota orientadas a tareas. Evitar etiquetas de prototipo en el recorrido del usuario y decoración que compita con los datos del mapa.

## Colors
Los valores del frontmatter reflejan las variables de frontend/src/product.css. La fuente canónica es el CSS existente (modelo B); este documento explica el sistema, no genera una segunda copia de tokens. primary corresponde a --verde; background a --fondo; text a --tinta; muted a --gris; border a --borde. El foco usa el contorno compartido definido en ese archivo. Esta ampliación no cambia estos tokens.

## Typography
La interfaz utiliza la pila sans del CSS; Georgia se reserva al énfasis del titular fotográfico. No se descarga una fuente externa. Nombres de estaciones, porcentajes de batería y controles conservan la escala de las pantallas existentes.

## Layout
Contenido de hasta 1384 px y 32 px de separación en escritorio; los media queries de product.css adaptan las vistas a 820 y 640 px. Mapa con resultados laterales en escritorio y apilados en móvil. Navegación superior en escritorio e inferior en teléfono. La galería mantiene cuatro columnas en escritorio y dos en móvil, filtradas por estación.

## Elevation & Depth
Superficies blancas con borde suave y sombras discretas. La fotografía concentra el contraste de la portada. Los códigos QR siempre tienen fondo blanco y celdas negras.

## Shapes
Tarjetas de 18 px y acciones redondeadas de 50 px según product.css. Iconos SVG existentes; no se añade otra biblioteca.

## Components
Acciones y enlaces conservan sus estados hover, foco, deshabilitado y carga del CSS compartido. Los botones mantienen etiquetas en español. El componente CodigoBicicleta es dueño de la presentación del QR: imagen con texto alternativo, dimensiones reservadas y enlace de descarga. La galería y la descarga usan el mismo PNG verificado para impedir discrepancias de codificación. No se inventa otro diseño para la imagen.

## Motion
Transiciones breves existentes, con respeto a prefers-reduced-motion. La nueva imagen QR no tiene animaciones.

## Verification
58 JUnit, escenarios de mapa/QR y pruebas generales de interfaz en H2 temporal; validación automática de todos los PNG con el lector usado por la aplicación. Inspección de la galería en escritorio y móvil. No se declara una auditoría retroactiva de todas las pantallas por esta corrección de recursos.

## Formularios y confianza · 2026-10-20
Se conserva la identidad fotográfica y la paleta vigente. EnlacesLegales comparte enlaces discretos subrayados en registro y pie de página; los documentos públicos usan una columna legible y adaptable. MetodoPagoForm mantiene el diseño compartido en todas las pantallas. La validación se presenta junto al formulario y anuncia errores con role=alert. Los consentimientos son independientes. No se redefine la paleta.
