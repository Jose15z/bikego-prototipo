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
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(new ErrorDto(CodigoError.DATOS_INVALIDOS.name(), detalle, null));
    }

    /** Otro usuario tiene tomada la bicicleta en este momento (CA-7). */
    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<ErrorDto> bloqueo(ConcurrencyFailureException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorDto(
                CodigoError.BICICLETA_NO_DISPONIBLE.name(), "Otro usuario está alquilando esta bicicleta", "VOLVER_AL_MAPA"));
    }
}
