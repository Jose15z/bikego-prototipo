import { validarFormulario } from '../components/validarFormulario'
import { useCallback, useState } from 'react'
import { api, ApiError } from '../api'
import type { Bicicleta, Viaje } from '../types'
import QrScanner, { leerQrDeImagen } from '../components/QrScanner'
import MetodoPagoForm from '../components/MetodoPagoForm'

interface Props {
  bicicleta: Bicicleta
  onVolver: () => void
  onViajeIniciado: (v: Viaje) => void
  onMetodoPagoActualizado: () => void
}

type Modo = 'camara' | 'imagen' | 'manual'

const TITULOS: Record<string, string> = {
  QR_INVALIDO: 'Código QR no válido',
  BICICLETA_NO_COINCIDE: 'No es la bicicleta seleccionada',
  BICICLETA_EN_USO: 'Bicicleta en uso',
  BICICLETA_EN_MANTENIMIENTO: 'Bicicleta en mantenimiento',
  BATERIA_INSUFICIENTE: 'Batería insuficiente',
  CUENTA_SUSPENDIDA: 'Cuenta suspendida',
  DEUDA_PENDIENTE: 'Tienes una deuda pendiente',
  VIAJE_EN_CURSO: 'Ya tienes un viaje activo',
  USUARIO_BLOQUEADO: 'Bloqueo temporal',
  SIN_METODO_PAGO: 'Sin método de pago',
  PAGO_RECHAZADO: 'Pago rechazado',
  DESBLOQUEO_FALLIDO: 'No fue posible desbloquear la bicicleta',
  BICICLETA_NO_DISPONIBLE: 'Bicicleta no disponible',
  SIN_CONEXION: 'Sin conexión',
}

/** F-02 Iniciar alquiler de bicicleta mediante codigo QR (SWR-02.1 a SWR-02.7). */
export default function EscanearScreen({ bicicleta, onVolver, onViajeIniciado, onMetodoPagoActualizado }: Props) {
  const [modo, setModo] = useState<Modo>('camara')
  const [idManual, setIdManual] = useState('')
  const [procesando, setProcesando] = useState(false)
  const [error, setError] = useState<ApiError | null>(null)
  const [ultimoQr, setUltimoQr] = useState<string | null>(null)
  const [pagoActualizado, setPagoActualizado] = useState(false)

  const iniciar = useCallback(
    async (contenido: string) => {
      setUltimoQr(contenido)
      setProcesando(true)
      setError(null)
      try {
        const viaje = await api.iniciarAlquiler(bicicleta.id, contenido)
        onViajeIniciado(viaje)
      } catch (e) {
        setError(e instanceof ApiError ? e : null)
      } finally {
        setProcesando(false)
      }
    },
    [bicicleta.id, onViajeIniciado],
  )

  const subirImagen = async (archivo: File | undefined) => {
    if (!archivo) return
    try {
      iniciar(await leerQrDeImagen(archivo))
    } catch {
      setError(new ApiError(422, { codigo: 'QR_INVALIDO', mensaje: 'No se encontró un código QR legible en la imagen', accionSugerida: 'REINTENTAR_O_INGRESAR_ID' }))
    }
  }

  const reintentar = () => {
    setError(null)
    setPagoActualizado(false)
  }

  if (procesando) {
    return (
      <div className="pantalla centro">
        <div className="spinner" />
        <h2>Iniciando alquiler de {bicicleta.id}</h2>
        <p className="sub">Validando cuenta y método de pago y enviando la orden de desbloqueo al candado.</p>
      </div>
    )
  }

  if (error) {
    const esPago = error.accionSugerida === 'ACTUALIZAR_METODO_PAGO'
    const esQr = error.accionSugerida === 'REINTENTAR_O_INGRESAR_ID'
    return (
      <div className="pantalla">
        <div id="escaneo-error" className="tarjeta error">
          <h2>{TITULOS[error.codigo] ?? 'No se pudo iniciar el alquiler'}</h2>
          <p>{error.message}</p>
          <p className="codigo">Código: {error.codigo}</p>
        </div>

        {esPago && !pagoActualizado && (
          <MetodoPagoForm
            onGuardado={() => {
              setPagoActualizado(true)
              onMetodoPagoActualizado()
            }}
          />
        )}
        {esPago && pagoActualizado && ultimoQr && (
          <div className="tarjeta ok">
            <p>Método de pago actualizado.</p>
            <button className="primario" onClick={() => iniciar(ultimoQr)}>Reintentar desbloqueo</button>
          </div>
        )}
        {esQr && <button className="primario" onClick={reintentar}>Reintentar lectura</button>}
        {error.codigo === 'DESBLOQUEO_FALLIDO' && ultimoQr && (
          <button className="primario" onClick={() => iniciar(ultimoQr)}>Intentar de nuevo</button>
        )}
        <button className="secundario" onClick={onVolver}>Volver al mapa</button>
      </div>
    )
  }

  return (
    <div className="pantalla">
      <button className="enlace volver" onClick={onVolver}>← Mapa</button>
      <h1>Escanea el QR de {bicicleta.id}</h1>
      <p className="sub">El código está en el manubrio de la bicicleta.</p>

      <div className="pestanas" role="tablist">
        <button role="tab" aria-selected={modo === 'camara'} className={modo === 'camara' ? 'activo' : ''} onClick={() => setModo('camara')}>Cámara</button>
        <button role="tab" aria-selected={modo === 'imagen'} className={modo === 'imagen' ? 'activo' : ''} onClick={() => setModo('imagen')}>Subir imagen</button>
        <button role="tab" aria-selected={modo === 'manual'} className={modo === 'manual' ? 'activo' : ''} onClick={() => setModo('manual')}>Ingresar ID</button>
      </div>

      {modo === 'camara' && <QrScanner onLeido={iniciar} />}

      {modo === 'imagen' && (
        <div className="tarjeta">
          <label htmlFor="archivo">Foto o captura del código QR</label>
          <input id="archivo" type="file" accept="image/*" onChange={(e) => subirImagen(e.target.files?.[0])} />
        </div>
      )}

      {modo === 'manual' && (
        <form noValidate
          className="tarjeta"
          onSubmit={(e) => {
            e.preventDefault()
            const invalid=validarFormulario(e,'escaneo-error');if(invalid){setError(new ApiError(400,{codigo:'DATOS_INVALIDOS',mensaje:invalid,accionSugerida:null}));return}
            iniciar(idManual)
          }}
        >
          <label htmlFor="idbici">ID impreso bajo el QR</label>
          <input id="idbici" placeholder="BG-001" value={idManual} onChange={(e) => setIdManual(e.target.value)} required />
          <button className="primario">Desbloquear</button>
        </form>
      )}
      <div id="lector-imagen-oculto" hidden />
    </div>
  )
}
