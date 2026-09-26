import { QRCodeSVG } from 'qrcode.react'

const BICICLETAS = [
  { id: 'BG-001', nota: 'Disponible, 85 %' },
  { id: 'BG-004', nota: 'Disponible, 62 %' },
  { id: 'BG-006', nota: 'Disponible, 100 %' },
  { id: 'BG-008', nota: 'Candado sin respuesta' },
  { id: 'BG-003', nota: 'En mantenimiento' },
  { id: 'BG-005', nota: 'En uso' },
]

/** Codigos para probar SWR-02.1 y SWR-02.2 desde otro dispositivo o con "Subir imagen". */
export default function QrPruebaScreen({ onVolver }: { onVolver: () => void }) {
  return (
    <div className="pantalla">
      <button className="enlace volver" onClick={onVolver}>← Mapa</button>
      <h1>QR de prueba</h1>
      <p className="sub">Abre esta página en otro dispositivo y escanea con la cámara, o toma una captura y usa "Subir imagen".</p>
      <div className="rejilla-qr">
        {BICICLETAS.map((b) => (
          <figure key={b.id}>
            <QRCodeSVG value={`BIKEGO:${b.id}`} size={120} />
            <figcaption>
              <strong>{b.id}</strong>
              <span>{b.nota}</span>
            </figcaption>
          </figure>
        ))}
        <figure>
          <QRCodeSVG value="https://ejemplo.com/cupon-123" size={120} />
          <figcaption>
            <strong>QR ajeno</strong>
            <span>Debe mostrar error</span>
          </figcaption>
        </figure>
      </div>
    </div>
  )
}
