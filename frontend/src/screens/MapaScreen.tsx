import Icono from '../components/Icono'
import { useCallback, useEffect, useState } from 'react'
import { api, ApiError, formatoDistancia } from '../api'
import type { Busqueda, Estacion, Ubicacion } from '../types'
import MapaBicicletas from '../components/MapaBicicletas'

interface Props {
  onQr: () => void
  ubicacion: Ubicacion | null
  onUbicacion: (u: Ubicacion) => void
  onSeleccionar: (bicicletaId: string) => void
}

const RADIOS = [1000, 2000, 3000, 4000, 5000]

/** F-01 Consultar bicicletas disponibles cercanas (SWR-01.1 a SWR-01.5). */
export default function MapaScreen({ ubicacion, onUbicacion, onSeleccionar, onQr }: Props) {
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

  const estacionElegida = estaciones.find(e => e.nombre === ubicacion?.etiqueta)
  return (
    <section className="pantalla explore-panel" id="estaciones" aria-labelledby="titulo-explorar" data-response-ms={tiempoMs ?? undefined}>
      <header className="explore-heading"><div><p className="eyebrow">TU PRÓXIMO RECORRIDO EMPIEZA AQUÍ</p><h2 id="titulo-explorar">Bicicletas cerca de ti</h2><p className="sub">Elige dónde empiezas. Nosotros ponemos las dos ruedas.</p></div><button className="enlace catalogo-link" onClick={onQr}>Códigos de bicicletas <span aria-hidden="true">↗</span></button></header>
      <div className="explore-filters">
        <div className="station-field"><label htmlFor="estacion"><Icono nombre="ubicacion" /> Estación</label><select id="estacion" value={ubicacion?.origen === 'estacion' ? estacionElegida?.id ?? '' : ''} onChange={e => e.target.value ? elegirEstacion(e.target.value) : pedirUbicacion()}>
          <option value="">Mi ubicación actual</option>{estaciones.map(e => <option key={e.id} value={e.id}>{e.nombre}</option>)}
        </select></div>
        <div className="radius-field"><span className="field-caption">Distancia de búsqueda</span><div className="chips" role="group" aria-label="Radio de búsqueda">{RADIOS.map(r => <button key={r} className={r === radio ? 'chip activo' : 'chip'} onClick={() => setRadio(r)} aria-pressed={r === radio}>{r / 1000} km</button>)}</div></div>
        <button className="secundario location-button" onClick={actualizar} disabled={cargando}><Icono nombre="ubicacion" />{cargando ? 'Consultando...' : 'Actualizar ubicación'}</button>
      </div>
      {estacionElegida?.referencia && <p className="station-access"><Icono nombre="ubicacion" /><strong>Punto de encuentro:</strong> {estacionElegida.referencia}</p>}
      {permisoDenegado && !ubicacion && <p className="location-hint">Puedes elegir una estación para explorar sin compartir tu ubicación.</p>}
      {error && <div className="tarjeta error" role="alert">{error}</div>}
      <div className="explore-layout">
        <div className="explore-map">
          {ubicacion ? <MapaBicicletas centro={ubicacion} radioMetros={radio} estaciones={estaciones} bicicletas={busqueda?.bicicletas ?? []} onSeleccionar={onSeleccionar} /> : <div className="map-placeholder"><Icono nombre="mapa" /><h3>Encuentra tu punto de partida</h3><p>Elige una estación o comparte tu ubicación para ver las bicicletas cercanas.</p></div>}
          <div className="map-legend"><span><i className="legend-station">E</i> Estaciones</span><span><i className="legend-bike" /> Bicicletas disponibles</span><span className="map-city">Haz clic en una estación para ver su acceso</span></div>
        </div>
        <aside className="explore-results" aria-label="Bicicletas disponibles"><div className="results-heading"><h3>Listas para salir</h3><span className="explore-count">{busqueda?.bicicletas.length ?? 0}</span></div><p className="sub">{ubicacion ? `Cerca de ${ubicacion.etiqueta}` : 'Tu próxima bicicleta te espera'}</p>
          {cargando && !busqueda && <p className="sub">Buscando bicicletas…</p>}
          {busqueda && busqueda.bicicletas.length === 0 && <div className="empty-bikes"><Icono nombre="bicicleta" /><p>{busqueda.mensaje}</p>{busqueda.puedeAmpliar && <button className="primario" onClick={() => setRadio(Math.min(radio + 1000, busqueda.radioMaximoMetros))}>Ampliar búsqueda</button>}</div>}
          {busqueda && busqueda.bicicletas.length > 0 && <ul className="lista">{busqueda.bicicletas.map(b => <li key={b.id}><button className="fila" onClick={() => onSeleccionar(b.id)} aria-label={`${b.id}, ${b.estacion}, batería ${b.bateria} por ciento`}>
            <span className="bike-thumb"><img src="/bike.svg" alt="" /></span><span className="fila-info"><strong className="fila-id">{b.id}</strong><span>{formatoDistancia(b.distanciaMetros)} de ti <span aria-hidden="true">·</span> Eléctrica</span></span><span className={b.bateria < 40 ? 'bateria baja' : 'bateria'}>{b.bateria}<small>%</small></span>
          </button></li>)}</ul>}
          <p className="results-note"><Icono nombre="rayo" /> La batería indicada se actualiza con la disponibilidad.</p>
        </aside>
      </div>
    </section>
  )
}
