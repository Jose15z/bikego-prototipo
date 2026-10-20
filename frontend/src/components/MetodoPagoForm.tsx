import { useState, type FormEvent } from 'react'
import { validarFormulario } from './validarFormulario'
import { api, ApiError } from '../api'

interface Props {
  onGuardado: () => void
}

/** SWR-02.4 flujo alterno 1: actualizar el metodo de pago sin salir del flujo. */
export default function MetodoPagoForm({ onGuardado }: Props) {
  const [numero, setNumero] = useState('')
  const [vencimiento, setVencimiento] = useState('')
  const [titular, setTitular] = useState('')
  const [mostrar, setMostrar] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [guardando, setGuardando] = useState(false)

  const guardar = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    if(guardando)return
    const invalid=validarFormulario(e,'pago-error');if(invalid){setError(invalid);return}
    const digits=numero.replace(/[\s\u00a0-]/g,'')
    if(!/^[0-9]{13,19}$/.test(digits)){setError('Escribe entre 13 y 19 dígitos de la tarjeta. Se permiten espacios y guiones.');e.currentTarget.querySelector<HTMLInputElement>('#numero')?.focus();return}
    if(!/^(0?[1-9]|1[0-2])\s*\/\s*(20\d{2}|\d{2})$/.test(vencimiento.trim())){setError('Escribe el vencimiento como MM/AA o MM/AAAA; por ejemplo, 12/2029.');e.currentTarget.querySelector<HTMLInputElement>('#vence')?.focus();return}
    setGuardando(true)
    setError(null)
    try {
      await api.actualizarMetodoPago(numero, vencimiento, titular)
      setNumero(''); setVencimiento(''); setTitular(''); onGuardado()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No fue posible guardar la tarjeta')
    } finally {
      setGuardando(false)
    }
  }

  return (
    <form noValidate className="formulario" onSubmit={guardar}>
      <h3>Actualizar método de pago</h3>
      <label htmlFor="numero">Número de tarjeta</label>
      <input id="numero" type={mostrar?'text':'password'} autoComplete="cc-number" inputMode="numeric" maxLength={32} placeholder="4111 1111 1111 1111" value={numero}
             onChange={(e) => setNumero(e.target.value)} required /><button type="button" className="enlace" aria-pressed={mostrar} onClick={()=>setMostrar(!mostrar)}>{mostrar?'Ocultar número de tarjeta':'Mostrar número de tarjeta'}</button>
      <div className="dos-columnas">
        <div>
          <label htmlFor="vence">Vence (MM/AA)</label>
          <input id="vence" autoComplete="cc-exp" placeholder="12/2029" maxLength={10} value={vencimiento} onChange={(e) => setVencimiento(e.target.value)} required />
        </div>
        <div>
          <label htmlFor="titular">Titular</label>
          <input id="titular" autoComplete="cc-name" maxLength={100} value={titular} onChange={(e) => setTitular(e.target.value)} required />
        </div>
      </div>
      <p className="sub">Pagos simulados: no se realizan cargos bancarios. Usa 4111 1111 1111 1111 con una fecha futura. Se guardan un token y los últimos 4 dígitos; nunca el número completo.</p>
      {error && <p id="pago-error" className="alerta-texto" role="alert">{error}</p>}
      <button className="primario" disabled={guardando}>{guardando ? 'Guardando…' : 'Guardar tarjeta'}</button>
    </form>
  )
}
