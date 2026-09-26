import { useState, type FormEvent } from 'react'
import { api, ApiError } from '../api'

interface Props {
  usuarioId: string
  onGuardado: () => void
}

/** SWR-02.4 flujo alterno 1: actualizar el metodo de pago sin salir del flujo. */
export default function MetodoPagoForm({ usuarioId, onGuardado }: Props) {
  const [numero, setNumero] = useState('')
  const [vencimiento, setVencimiento] = useState('')
  const [titular, setTitular] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [guardando, setGuardando] = useState(false)

  const guardar = async (e: FormEvent) => {
    e.preventDefault()
    setGuardando(true)
    setError(null)
    try {
      await api.actualizarMetodoPago(usuarioId, numero, vencimiento, titular)
      onGuardado()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No fue posible guardar la tarjeta')
    } finally {
      setGuardando(false)
    }
  }

  return (
    <form className="formulario" onSubmit={guardar}>
      <h3>Actualizar método de pago</h3>
      <label htmlFor="numero">Número de tarjeta</label>
      <input id="numero" inputMode="numeric" placeholder="4111 1111 1111 1111" value={numero}
             onChange={(e) => setNumero(e.target.value)} required />
      <div className="dos-columnas">
        <div>
          <label htmlFor="vence">Vence (MM/AA)</label>
          <input id="vence" placeholder="12/29" value={vencimiento} onChange={(e) => setVencimiento(e.target.value)} required />
        </div>
        <div>
          <label htmlFor="titular">Titular</label>
          <input id="titular" value={titular} onChange={(e) => setTitular(e.target.value)} required />
        </div>
      </div>
      <p className="sub">La pasarela tokeniza la tarjeta. BikeGo solo guarda los últimos 4 dígitos.</p>
      {error && <p className="alerta-texto">{error}</p>}
      <button className="primario" disabled={guardando}>{guardando ? 'Validando con la pasarela...' : 'Guardar tarjeta'}</button>
    </form>
  )
}
