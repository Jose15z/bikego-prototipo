package com.bikego.service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class SeguridadHash {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int ITERACIONES = 210_000;

    private SeguridadHash() { }

    public static String password(String texto) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return ITERACIONES + ":" + Base64.getEncoder().encodeToString(salt) + ":" +
                Base64.getEncoder().encodeToString(pbkdf2(texto.toCharArray(), salt, ITERACIONES));
    }

    public static boolean verificarPassword(String texto, String guardado) {
        if (guardado == null) return false;
        String[] partes = guardado.split(":");
        if (partes.length != 3) return false;
        int iteraciones = Integer.parseInt(partes[0]);
        byte[] salt = Base64.getDecoder().decode(partes[1]);
        byte[] esperado = Base64.getDecoder().decode(partes[2]);
        return MessageDigest.isEqual(esperado, pbkdf2(texto.toCharArray(), salt, iteraciones));
    }

    public static String sha256(String texto) {
        try {
            return Base64.getEncoder().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("No fue posible calcular el hash", e);
        }
    }

    private static byte[] pbkdf2(char[] texto, byte[] salt, int iteraciones) {
        try {
            PBEKeySpec spec = new PBEKeySpec(texto, salt, iteraciones, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("No fue posible derivar la contraseña", e);
        }
    }
}
