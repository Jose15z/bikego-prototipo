import { useEffect, useRef, useState } from 'react'
import { api, formatoCOP } from '../api'
import type { Viaje } from '../types'

interface Props {
  viajeInicial: Viaje
  recibidoEn: number
}

const mmss = (s: number) => {
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const seg = s % 60
  const dos = (n: number) => n.toString().padStart(2, '0')
  return h > 0 ? `${h}:${dos(m)}:${dos(seg)}` : `${dos(m)}:${dos(seg)}`
}

/** SWR-02.8: pantalla Viaje en curso con tiempo, distancia y costo acumulado. */
export default function ViajeScreen({ viajeInicial, recibidoEn }: Props) {
  const [viaje, setViaje] = useState(viajeInicial)
  const [segundos, setSegundos] = useState(viajeInicial.segundosTranscurridos)
  const [msRender, setMsRender] = useState<number | null>(null)
  const inicioLocal = useRef(Date.now() - viajeInicial.segundosTranscurridos * 1000)

  useEffect(() => {
    setMsRender(Math.round(performance.now() - recibidoEn))
  }, [recibidoEn])

  useEffect(() => {
    const t = setInterval(() => setSegundos(Math.floor((Date.now() - inicioLocal.current) / 1000)), 1000)
    return () => clearInterval(t)
  }, [])

  // Sincroniza con el servidor cada 15 s
  useEffect(() => {
    const t = setInterval(() => {
      api.viaje(viaje.idViaje).then((v) => {
        setViaje(v)
        inicioLocal.current = Date.now() - v.segundosTranscurridos * 1000
      }).catch(() => undefined)
    }, 15000)
    return () => clearInterval(t)
  }, [viaje.idViaje])

  const costo = viaje.tarifaBase + Math.ceil(segundos / 60) * viaje.tarifaMinuto
  const distancia = (viaje.velocidadPromedioKmh * segundos) / 3600

  return (
    <div className="pantalla">
      <div className="estado-viaje">Viaje en curso</div>
      <h1>{viaje.idBicicleta} desbloqueada</h1>
      <p className="sub">Viaje #{viaje.idViaje} · inicio {new Date(viaje.horaInicio).toLocaleTimeString('es-CO')}</p>

      <div className="indicadores">
        <div className="indicador">
          <span className="valor">{mmss(segundos)}</span>
          <span className="rotulo">Tiempo</span>
        </div>
        <div className="indicador">
          <span className="valor">{distancia.toFixed(2).replace('.', ',')} km</span>
          <span className="rotulo">Distancia</span>
        </div>
        <div className="indicador">
          <span className="valor">{formatoCOP(costo)}</span>
          <span className="rotulo">Costo acumulado</span>
        </div>
      </div>

      <div className="tarjeta">
        <p className="sub">
          Tarifa de prueba: {formatoCOP(viaje.tarifaBase)} base + {formatoCOP(viaje.tarifaMinuto)} por minuto.
          La distancia es una estimación del prototipo; el seguimiento GPS real hace parte de F-05.
        </p>
      </div>
      {msRender !== null && <p className="metrica">Pantalla mostrada {msRender} ms después de la confirmación del candado (meta 2000 ms, SWR-02.8)</p>}
    </div>
  )
}
