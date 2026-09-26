import { useCallback, useEffect, useState } from 'react'
import { api } from './api'
import type { Bicicleta, Ubicacion, Usuario, Viaje } from './types'
import MapaScreen from './screens/MapaScreen'
import DetalleScreen from './screens/DetalleScreen'
import EscanearScreen from './screens/EscanearScreen'
import ViajeScreen from './screens/ViajeScreen'
import QrPruebaScreen from './screens/QrPruebaScreen'

type Pantalla =
  | { nombre: 'mapa' }
  | { nombre: 'detalle'; bicicletaId: string }
  | { nombre: 'escanear'; bicicleta: Bicicleta }
  | { nombre: 'viaje'; viaje: Viaje; recibidoEn: number }
  | { nombre: 'qrs' }

export default function App() {
  const [pantalla, setPantalla] = useState<Pantalla>({ nombre: 'mapa' })
  const [usuarios, setUsuarios] = useState<Usuario[]>([])
  const [usuarioId, setUsuarioId] = useState('U-001')
  const [ubicacion, setUbicacion] = useState<Ubicacion | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)

  const cargarUsuarios = useCallback(() => {
    api.usuarios().then(setUsuarios).catch(() => setUsuarios([]))
  }, [])

  useEffect(cargarUsuarios, [cargarUsuarios])

  const usuario = usuarios.find((u) => u.id === usuarioId)

  const reiniciar = async () => {
    await api.reiniciarDemo()
    cargarUsuarios()
    setPantalla({ nombre: 'mapa' })
    setAviso('Datos de prueba reiniciados')
    setTimeout(() => setAviso(null), 2500)
  }

  return (
    <div className="telefono">
      <header className="barra">
        <button className="marca" onClick={() => setPantalla({ nombre: 'mapa' })} aria-label="Ir al mapa">
          <img src="/bike.svg" alt="" width={22} height={22} />
          BikeGo
        </button>
        <div className="barra-acciones">
          <button className="enlace" onClick={() => setPantalla({ nombre: 'qrs' })}>QR de prueba</button>
          <button className="enlace" onClick={reiniciar}>Reiniciar demo</button>
        </div>
      </header>

      <section className="usuario-prueba">
        <label htmlFor="usuario">Usuario de prueba</label>
        <select id="usuario" value={usuarioId} onChange={(e) => setUsuarioId(e.target.value)}>
          {usuarios.map((u) => (
            <option key={u.id} value={u.id}>
              {u.id} · {u.nombre}
            </option>
          ))}
        </select>
        {usuario && <p className="escenario">{usuario.escenario}</p>}
      </section>

      {aviso && <div className="toast">{aviso}</div>}

      <main className="contenido">
        {pantalla.nombre === 'mapa' && (
          <MapaScreen
            ubicacion={ubicacion}
            onUbicacion={setUbicacion}
            onSeleccionar={(id) => setPantalla({ nombre: 'detalle', bicicletaId: id })}
          />
        )}
        {pantalla.nombre === 'detalle' && (
          <DetalleScreen
            bicicletaId={pantalla.bicicletaId}
            ubicacion={ubicacion}
            onVolver={() => setPantalla({ nombre: 'mapa' })}
            onEscanear={(b) => setPantalla({ nombre: 'escanear', bicicleta: b })}
          />
        )}
        {pantalla.nombre === 'escanear' && (
          <EscanearScreen
            bicicleta={pantalla.bicicleta}
            usuarioId={usuarioId}
            onVolver={() => setPantalla({ nombre: 'mapa' })}
            onMetodoPagoActualizado={cargarUsuarios}
            onViajeIniciado={(v) => {
              cargarUsuarios()
              setPantalla({ nombre: 'viaje', viaje: v, recibidoEn: performance.now() })
            }}
          />
        )}
        {pantalla.nombre === 'viaje' && <ViajeScreen viajeInicial={pantalla.viaje} recibidoEn={pantalla.recibidoEn} />}
        {pantalla.nombre === 'qrs' && <QrPruebaScreen onVolver={() => setPantalla({ nombre: 'mapa' })} />}
      </main>

      <footer className="pie">Prototipo Parcial 1 · F-01 y F-02 · Plan de Requisitos v0.3</footer>
    </div>
  )
}
