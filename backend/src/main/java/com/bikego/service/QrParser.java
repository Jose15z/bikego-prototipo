package com.bikego.service;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SWR-02.1: extrae el ID de bicicleta del contenido del QR.
 * Formatos aceptados: "BIKEGO:BG-001" (QR impreso en la bicicleta) o "BG-001" (ingreso manual).
 */
public final class QrParser {

    private static final Pattern PATRON = Pattern.compile("^(?:BIKEGO:)?(BG-\\d{3})$");

    private QrParser() { }

    public static Optional<String> extraerIdBicicleta(String contenido) {
        if (contenido == null) {
            return Optional.empty();
        }
        Matcher m = PATRON.matcher(contenido.trim().toUpperCase());
        return m.matches() ? Optional.of(m.group(1)) : Optional.empty();
    }
}
