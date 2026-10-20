/** One verified asset is used both for display and download. */
export default function CodigoBicicleta({ id }: { id: string }) {
  const archivo = `/qr-prueba/${encodeURIComponent(id)}.png`
  return <figure>
    <img src={archivo} alt={`Código QR de la bicicleta ${id}`} width={160} height={160} loading="lazy" />
    <figcaption><strong>{id}</strong><a href={archivo} download={`${id}.png`}>Descargar QR de {id}</a></figcaption>
  </figure>
}
