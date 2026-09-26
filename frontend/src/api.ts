import type { Busqueda, Bicicleta, ErrorApi, Estacion, Usuario, Viaje } from './types'

export class ApiError extends Error {
  readonly codigo: string
  readonly accionSugerida: string | null
  readonly status: number

  constructor(status: number, body: ErrorApi) {
    super(body.mensaje)
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
      headers: { 'Content-Type': 'application/json', ...(init?.headers ?? {}) },
    })
  } catch {
    throw new ApiError(0, {
      codigo: 'SIN_CONEXION',
      mensaje: 'No hay conexión con el servidor de BikeGo. Revisa que el backend esté corriendo en el puerto 8080.',
      accionSugerida: 'REINTENTAR',
    })
  }
  if (res.status === 204) return undefined as T
  const body = await res.json().catch(() => null)
  if (!res.ok) {
    throw new ApiError(res.status, body ?? { codigo: 'ERROR', mensaje: 'Error inesperado', accionSugerida: null })
  }
  return body as T
}

export const api = {
  estaciones: () => request<Estacion[]>('/api/estaciones'),

  cercanas: (lat: number, lng: number, radio: number) =>
    request<Busqueda>(`/api/bicicletas/cercanas?lat=${lat}&lng=${lng}&radio=${radio}`),

  detalle: (id: string, lat?: number, lng?: number) =>
    request<Bicicleta>(`/api/bicicletas/${id}` + (lat !== undefined ? `?lat=${lat}&lng=${lng}` : '')),

  iniciarAlquiler: (usuarioId: string, bicicletaSeleccionadaId: string | null, codigoQr: string) =>
    request<Viaje>('/api/alquileres', {
      method: 'POST',
      body: JSON.stringify({ usuarioId, bicicletaSeleccionadaId, codigoQr }),
    }),

  viaje: (id: number) => request<Viaje>(`/api/viajes/${id}`),

  usuarios: () => request<Usuario[]>('/api/usuarios'),

  actualizarMetodoPago: (usuarioId: string, numeroTarjeta: string, vencimiento: string, titular: string) =>
    request<Usuario>(`/api/usuarios/${usuarioId}/metodo-pago`, {
      method: 'PUT',
      body: JSON.stringify({ numeroTarjeta, vencimiento, titular }),
    }),

  reiniciarDemo: () => request<void>('/api/demo/reiniciar', { method: 'POST' }),
}

export const formatoCOP = (valor: number) =>
  new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(valor)

export const formatoDistancia = (metros: number) =>
  metros < 1000 ? `${metros} m` : `${(metros / 1000).toFixed(1).replace('.', ',')} km`
