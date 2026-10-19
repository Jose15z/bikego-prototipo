import { useEffect, useState } from 'react'
import { api, ApiError, formatoDistancia } from '../api'
import IncidenteForm from '../components/IncidenteForm'
import type { Bicicleta, Ubicacion } from '../types'

interface Props {
  bicicletaId: string
  ubicacion: Ubicacion | null
  onVolver: () => void
  onEscanear: (b: Bicicleta) => void
}

/** SWR-01.3 y SWR-01.6: el detalle se pide al servidor, que revalida la disponibilidad. */
export default function DetalleScreen({ bicicletaId, ubicacion, onVolver, onEscanear }: Props) {
  const [bici, setBici] = useState<Bicicleta | null>(null)
  const [error, setError] = useState<ApiError | null>(null)

  useEffect(() => {
    api
      .detalle(bicicletaId, ubicacion?.lat, ubicacion?.lng)
      .then(setBici)
      .catch((e) => setError(e instanceof ApiError ? e : null))
  }, [bicicletaId, ubicacion])

  // Flujo alterno 2: si ya no esta disponible se informa y se regresa al mapa
  useEffect(() => {
    if (error?.codigo === 'BICICLETA_NO_DISPONIBLE') {
      const t = setTimeout(onVolver, 3500)
      return () => clearTimeout(t)
    }
  }, [error, onVolver])

  if (error) {
    return (
      <div className="pantalla">
        <div className="tarjeta error">
          <h2>Bicicleta no disponible</h2>
          <p>{error.message}</p>
          <p className="sub">Te llevamos de vuelta al mapa...</p>
          <button className="primario" onClick={onVolver}>Volver al mapa</button>
        </div>
      </div>
    )
  }

  if (!bici) return <div className="pantalla"><p className="sub">Verificando disponibilidad...</p></div>

  return (
    <div className="pantalla">
      <button className="enlace volver" onClick={onVolver}>← Mapa</button>
      <h1>Detalle de bicicleta</h1>
      <div className="tarjeta detalle">
        <div className="detalle-cabecera">
          <span className="detalle-id">{bici.id}</span>
          <span className="estado-ok">{bici.estado}</span>
        </div>
        <div className="bateria-barra" aria-label={`Batería ${bici.bateria} %`}>
          <div style={{ width: `${bici.bateria}%` }} className={bici.bateria < 40 ? 'relleno bajo' : 'relleno'} />
        </div>
        <dl className="datos">
          <dt>Batería</dt>
          <dd>{bici.bateria} %</dd>
          <dt>Estación</dt>
          <dd>{bici.estacion}</dd>
          <dt>Distancia aproximada</dt>
          <dd>{bici.distanciaMetros >= 0 ? formatoDistancia(bici.distanciaMetros) : 'Sin ubicación'}</dd>
          <dt>Autonomía estimada</dt>
          <dd>{bici.autonomiaKm.toString().replace('.', ',')} km</dd>
        </dl>
      </div>
      <button className="primario grande" onClick={() => onEscanear(bici)}>
        Escanear QR para desbloquear
      </button>
      <IncidenteForm bicicletaId={bici.id} onEnviado={onVolver} />
    </div>
  )
}
