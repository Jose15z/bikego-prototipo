package com.bikego.web;

import com.bikego.service.CodigoError;
import com.bikego.service.NegocioException;
import com.bikego.web.dto.ErrorDto;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ErrorDto> negocio(NegocioException e) {
        return ResponseEntity.status(e.getStatus())
                .body(new ErrorDto(e.getCodigo().name(), e.getMessage(), e.getAccionSugerida()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDto> validacion(MethodArgumentNotValidException e) {
        String detalle = e.getBindingResult().getFieldErrors().stream()
                .map(f -> switch(f.getField()) {
                    case "nombre" -> "Escribe tu nombre completo (3 a 100 caracteres).";
                    case "documento" -> "Documento: usa de 5 a 20 letras, números, puntos o guiones.";
                    case "correo" -> "Escribe un correo electrónico válido.";
                    case "telefono" -> "Teléfono: usa de 7 a 16 dígitos, con prefijo + opcional.";
                    case "fechaNacimiento" -> "Selecciona una fecha de nacimiento anterior a hoy.";
                    case "direccion" -> "Escribe tu dirección (hasta 160 caracteres).";
                    case "password" -> "La contraseña debe tener entre 10 y 128 caracteres.";
                    case "titular" -> "Escribe el nombre del titular de la tarjeta.";
                    default -> f.getDefaultMessage();
                })
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(new ErrorDto(CodigoError.DATOS_INVALIDOS.name(), detalle, null));
    }

    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorDto> formato(Exception e) {
        return ResponseEntity.badRequest().body(new ErrorDto("DATOS_INVALIDOS","Verifica el formato de los datos enviados",null));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorDto> duplicado(Exception e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorDto("DATOS_DUPLICADOS","El correo o documento ya está registrado",null));
    }

    /** Otro usuario tiene tomada la bicicleta en este momento (CA-7). */
    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<ErrorDto> bloqueo(ConcurrencyFailureException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorDto(
                CodigoError.BICICLETA_NO_DISPONIBLE.name(), "Otro usuario está alquilando esta bicicleta", "VOLVER_AL_MAPA"));
    }
}
