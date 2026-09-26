import { useEffect, useRef, useState } from 'react'
import { Html5Qrcode } from 'html5-qrcode'

interface Props {
  onLeido: (contenido: string) => void
}

/** SWR-02.1: lee el QR con la camara del dispositivo. */
export default function QrScanner({ onLeido }: Props) {
  const contenedorId = 'lector-qr'
  const lector = useRef<Html5Qrcode | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const qr = new Html5Qrcode(contenedorId)
    lector.current = qr
    let leido = false
    const inicio = qr.start(
      { facingMode: 'environment' },
      { fps: 10, qrbox: { width: 220, height: 220 } },
      (texto) => {
        if (leido) return
        leido = true
        qr.stop().catch(() => undefined)
        onLeido(texto)
      },
      () => undefined,
    ).catch(() => setError('No se pudo abrir la cámara. Usa "Subir imagen" o "Ingresar ID".'))

    // Espera a que termine el arranque antes de liberar la camara
    return () => {
      inicio
        .then(() => (qr.isScanning ? qr.stop() : undefined))
        .then(() => qr.clear())
        .catch(() => undefined)
    }
  }, [onLeido])

  return (
    <div>
      <div id={contenedorId} className="lector" />
      {error && <p className="alerta-texto">{error}</p>}
    </div>
  )
}

/** Alternativa para la demostracion en computador: leer el QR desde una imagen. */
export async function leerQrDeImagen(archivo: File): Promise<string> {
  const qr = new Html5Qrcode('lector-imagen-oculto')
  try {
    return await qr.scanFile(archivo, false)
  } finally {
    qr.clear()
  }
}
