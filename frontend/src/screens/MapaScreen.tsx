import { useCallback, useEffect, useState } from 'react'
import { api, ApiError, formatoDistancia } from '../api'
import type { Busqueda, Estacion, Ubicacion } from '../types'
import MapaBicicletas from '../components/MapaBicicletas'

interface Props {
  ubicacion: Ubicacion | null
  onUbicacion: (u: Ubicacion) => void
  onSeleccionar: (bicicletaId: string) => void
}

const RADIOS = [1000, 2000, 3000, 4000, 5000]

/** F-01 Consultar bicicletas disponibles cercanas (SWR-01.1 a SWR-01.5). */
export default function MapaScreen({ ubicacion, onUbicacion, onSeleccionar }: Props) {
  const [radio, setRadio] = useState(1000)
  const [busqueda, setBusqueda] = useState<Busqueda | null>(null)
  const [cargando, setCargando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [permisoDenegado, setPermisoDenegado] = useState(false)
  const [estaciones, setEstaciones] = useState<Estacion[]>([])
  const [tiempoMs, setTiempoMs] = useState<number | null>(null)

  // SWR-01.1: pedir el permiso de ubicacion y obtener coordenadas GPS
  const pedirUbicacion = useCallback(() => {
    if (!('geolocation' in navigator)) {
      setPermisoDenegado(true)
      return
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        if (pos.coords.accuracy > 50) {
          setPermisoDenegado(true)
          setError('La precisión GPS supera los 50 m. Elige una estación o vuelve a intentar al aire libre.')
          return
        }
        setPermisoDenegado(false)
        onUbicacion({
          lat: pos.coords.latitude,
          lng: pos.coords.longitude,
          origen: 'gps',
          precisionMetros: Math.round(pos.coords.accuracy),
          etiqueta: 'Tu ubicación',
        })
      },
      () => setPermisoDenegado(true),
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 },
    )
  }, [onUbicacion])

  useEffect(() => {
    api.estaciones().then(setEstaciones).catch(() => setEstaciones([]))
    if (!ubicacion) pedirUbicacion()
  }, [])

  // SWR-01.4: se consulta al abrir el mapa y al pulsar "Actualizar ubicacion"
  const buscar = useCallback(async (u: Ubicacion, r: number) => {
    setCargando(true)
    setError(null)
    const t0 = performance.now()
    try {
      setBusqueda(await api.cercanas(u.lat, u.lng, r))
      setTiempoMs(Math.round(performance.now() - t0))
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'No fue posible consultar la disponibilidad')
    } finally {
      setCargando(false)
    }
  }, [])

  useEffect(() => {
    if (ubicacion) buscar(ubicacion, radio)
  }, [ubicacion, radio, buscar])

  const elegirEstacion = (id: string) => {
    const e = estaciones.find((x) => x.id === id)
    if (e) onUbicacion({ lat: e.latitud, lng: e.longitud, origen: 'estacion', etiqueta: e.nombre })
  }

  const actualizar = () => {
    if (ubicacion?.origen === 'gps' || !ubicacion) pedirUbicacion()
    if (ubicacion) buscar(ubicacion, radio)
  }

  return (
    <div className="pantalla">
      <h1>Bicicletas cerca de ti</h1>

      {ubicacion && (
        <p className="sub">
          Punto de búsqueda: <strong>{ubicacion.etiqueta}</strong>
          {ubicacion.origen === 'gps' && ubicacion.precisionMetros !== undefined && (
            <span className={ubicacion.precisionMetros > 50 ? 'alerta-texto' : ''}>
              {' '}· precisión {ubicacion.precisionMetros} m
              {ubicacion.precisionMetros > 50 && ' (baja, puedes elegir una estación)'}
            </span>
          )}
        </p>
      )}

      {(permisoDenegado || !ubicacion || ubicacion.origen === 'estacion' || (ubicacion.precisionMetros ?? 0) > 50) && (
        <div className={permisoDenegado ? 'tarjeta aviso' : 'tarjeta'}>
          {permisoDenegado && <p>No tenemos una ubicación GPS precisa. Elige una estación como punto de búsqueda.</p>}
          <label htmlFor="estacion">Estación</label>
          <select
            id="estacion"
            value={ubicacion?.origen === 'estacion' ? estaciones.find((e) => e.nombre === ubicacion.etiqueta)?.id : ''}
            onChange={(e) => elegirEstacion(e.target.value)}
          >
            <option value="" disabled>
              Selecciona una estación
            </option>
            {estaciones.map((e) => (
              <option key={e.id} value={e.id}>
                {e.nombre}
              </option>
            ))}
          </select>
        </div>
      )}

      <div className="controles">
        <div className="chips" role="group" aria-label="Radio de búsqueda">
          {RADIOS.map((r) => (
            <button key={r} className={r === radio ? 'chip activo' : 'chip'} onClick={() => setRadio(r)}>
              {r / 1000} km
            </button>
          ))}
        </div>
        <button className="secundario" onClick={actualizar} disabled={cargando}>
          {cargando ? 'Consultando...' : 'Actualizar ubicación'}
        </button>
      </div>

      {ubicacion && (
        <MapaBicicletas
          centro={ubicacion}
          radioMetros={radio}
          bicicletas={busqueda?.bicicletas ?? []}
          onSeleccionar={onSeleccionar}
        />
      )}

      {error && <div className="tarjeta error">{error}</div>}

      {busqueda && busqueda.bicicletas.length === 0 && (
        <div className="tarjeta vacio">
          <p>{busqueda.mensaje}</p>
          {busqueda.puedeAmpliar && (
            <button className="primario" onClick={() => setRadio(Math.min(radio + 1000, busqueda.radioMaximoMetros))}>
              Ampliar búsqueda
            </button>
          )}
        </div>
      )}

      {busqueda && busqueda.bicicletas.length > 0 && (
        <ul className="lista">
          {busqueda.bicicletas.map((b) => (
            <li key={b.id}>
              <button className="fila" onClick={() => onSeleccionar(b.id)}>
                <span className="fila-id">{b.id}</span>
                <span className="fila-info">
                  {b.estacion} · {formatoDistancia(b.distanciaMetros)}
                </span>
                <span className={b.bateria < 40 ? 'bateria baja' : 'bateria'}>{b.bateria} %</span>
              </button>
            </li>
          ))}
        </ul>
      )}

      {tiempoMs !== null && (
        <p className="metrica">
          {busqueda?.bicicletas.length ?? 0} bicicletas en {radio / 1000} km · respuesta en {tiempoMs} ms
        </p>
      )}
    </div>
  )
}
