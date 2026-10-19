package com.bikego.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.time.LocalDateTime;

@Entity
public class Sesion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String tokenHash;
    private LocalDateTime creadaEn;
    private LocalDateTime expiraEn;
    private LocalDateTime revocadaEn;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private Usuario usuario;

    protected Sesion() { }

    public Sesion(Usuario usuario, String tokenHash, LocalDateTime creadaEn, LocalDateTime expiraEn) {
        this.usuario = usuario;
        this.tokenHash = tokenHash;
        this.creadaEn = creadaEn;
        this.expiraEn = expiraEn;
    }

    public boolean estaActiva(LocalDateTime ahora) {
        return revocadaEn == null && ahora.isBefore(expiraEn);
    }

    public void revocar(LocalDateTime ahora) { this.revocadaEn = ahora; }
    public Usuario getUsuario() { return usuario; }
}
