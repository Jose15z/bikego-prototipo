import type { FormEvent } from 'react'
/** Own validation feedback; never open browser validation bubbles. */
export function validarFormulario(event: FormEvent<HTMLFormElement>, errorId: string): string | null {
  const form=event.currentTarget
  const fields=Array.from(form.elements).filter((e):e is HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement => e instanceof HTMLInputElement || e instanceof HTMLSelectElement || e instanceof HTMLTextAreaElement)
  for(const input of fields){input.removeAttribute('aria-invalid');if(input.getAttribute('aria-describedby')===errorId)input.removeAttribute('aria-describedby')}
  const invalid=fields.find(input=>!input.validity.valid)
  if(!invalid)return null
  invalid.setAttribute('aria-invalid','true');invalid.setAttribute('aria-describedby',errorId);invalid.focus()
  const name=invalid.labels?.[0]?.textContent?.trim() || 'Este campo'
  if(invalid.validity.valueMissing)return invalid instanceof HTMLInputElement && invalid.type==='checkbox'?'Debes aceptar los documentos indicados para crear tu cuenta.':`Completa ${name.toLowerCase()}.`
  if(invalid.validity.typeMismatch)return 'Escribe un correo electrónico válido.'
  if(invalid.validity.tooShort)return `${name}: usa al menos ${'minLength' in invalid ? invalid.minLength : 0} caracteres.`
  if(invalid.validity.rangeOverflow)return 'Revisa la fecha de nacimiento.'
  return `${name}: revisa el formato indicado.`
}
