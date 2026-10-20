import type { Mensaje, Recibo, Zona, Evento, Reporte, Rechazo, AuthResponse, Busqueda, Bicicleta, ErrorApi, Estacion, RegistroPayload, RegistroResponse, Usuario, Viaje } from './types'

const TOKEN_KEY = 'bikego.session'
export const sesion = {
  token: () => localStorage.getItem(TOKEN_KEY),
  guardar: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  limpiar: () => localStorage.removeItem(TOKEN_KEY),
}

export class ApiError extends Error {
  readonly codigo: string
  readonly accionSugerida: string | null
  readonly status: number

  constructor(status: number, body: ErrorApi) {
    super(body.mensaje || (status >= 500 ? 'El servidor no pudo completar la solicitud. Conservamos tus datos; vuelve a intentarlo.' : 'Revisa los datos e intenta de nuevo.'))
    this.status = status
    this.codigo = body.codigo
    this.accionSugerida = body.accionSugerida
  }
}

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  let res: Response
  try {
    res = await fetch(url, {
      ...init,
      headers: {
        'Content-Type': 'application/json',
        ...(sesion.token() ? { Authorization: `Bearer ${sesion.token()}` } : {}),
        ...(init?.headers ?? {}),
      },
    })
  } catch {
    throw new ApiError(0, {
      codigo: 'SIN_CONEXION',
      mensaje: 'No pudimos conectar con BikeGo. Comprueba tu conexión y vuelve a intentarlo.',
      accionSugerida: 'REINTENTAR',
    })
  }
  if (res.status === 204) return undefined as T
  const body = await res.json().catch(() => null)
  if (!res.ok) {
    if (res.status === 401 && !url.includes('/auth/login')) window.dispatchEvent(new Event('sesion-expirada'))
    throw new ApiError(res.status, body ?? { codigo: 'ERROR', mensaje: 'Error inesperado', accionSugerida: null })
  }
  return body as T
}

export const api = {
  mensajes: () => request<Mensaje[]>('/api/mensajes'),
  reenviar: (correo: string, password: string) => request<RegistroResponse>('/api/auth/reenviar', { method: 'POST', body: JSON.stringify({ correo, password }) }),
  historial: () => request<Viaje[]>('/api/viajes'),
  zonas: (lat: number, lng: number) => request<Zona[]>(`/api/zonas?lat=${lat}&lng=${lng}`),
  finalizar: (id: number, latitud: number, longitud: number, precision: number) => request<Recibo>(`/api/viajes/${id}/finalizar`, { method: 'POST', body: JSON.stringify({ latitud, longitud, precision }) }),
  recibo: (id: number) => request<Recibo>(`/api/viajes/${id}/recibo`),
  pagar: (id: number) => request<Recibo>(`/api/viajes/${id}/pago`, { method: 'POST' }),
  validarPago: () => request<Usuario>('/api/usuarios/me/validar-pago', { method: 'POST' }),
  rechazos: () => request<Rechazo[]>('/api/usuarios/me/rechazos'),
  eventos: () => request<Evento[]>('/api/eventos'),
  reportar: (r: Reporte) => request<Evento>('/api/incidentes', { method: 'POST', body: JSON.stringify(r) }),
  flota: () => request<Bicicleta[]>('/api/flota'),
  mantenimiento: (r: Reporte) => request<Evento>('/api/mantenimientos', { method: 'POST', body: JSON.stringify(r) }),
  cerrarMantenimiento: (id: number, bateria: number) => request<Evento>(`/api/mantenimientos/${id}/cerrar`, { method: 'POST', body: JSON.stringify({ bateria }) }),
  revisar: (id: number, accion: string) => request<Recibo>(`/api/incidentes/${id}/revision`, { method: 'POST', body: JSON.stringify({ accion }) }),
  login: (correo: string, password: string) => request<AuthResponse>('/api/auth/login', {
    method: 'POST', body: JSON.stringify({ correo, password }),
  }),
  registro: (datos: RegistroPayload) => request<RegistroResponse>('/api/auth/registro', {
    method: 'POST', body: JSON.stringify(datos),
  }),
  verificar: (correo: string, codigo: string) => request<void>('/api/auth/verificacion', {
    method: 'POST', body: JSON.stringify({ correo, codigo }),
  }),
  me: () => request<Usuario>('/api/auth/me'),
  logout: () => request<void>('/api/auth/logout', { method: 'POST' }),
  estaciones: () => request<Estacion[]>('/api/estaciones'),

  cercanas: (lat: number, lng: number, radio: number) =>
    request<Busqueda>(`/api/bicicletas/cercanas?lat=${lat}&lng=${lng}&radio=${radio}`),

  detalle: (id: string, lat?: number, lng?: number) =>
    request<Bicicleta>(`/api/bicicletas/${id}` + (lat !== undefined ? `?lat=${lat}&lng=${lng}` : '')),

  iniciarAlquiler: (bicicletaSeleccionadaId: string | null, codigoQr: string) =>
    request<Viaje>('/api/alquileres', {
      method: 'POST',
      body: JSON.stringify({ bicicletaSeleccionadaId, codigoQr }),
    }),

  viaje: (id: number) => request<Viaje>(`/api/viajes/${id}`),

  actualizarMetodoPago: (numeroTarjeta: string, vencimiento: string, titular: string) =>
    request<Usuario>('/api/usuarios/me/metodo-pago', {
      method: 'PUT',
      body: JSON.stringify({ numeroTarjeta, vencimiento, titular }),
    }),
}

export const formatoCOP = (valor: number) =>
  new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(valor)

export const formatoDistancia = (metros: number) =>
  metros < 1000 ? `${metros} m` : `${(metros / 1000).toFixed(1).replace('.', ',')} km`
