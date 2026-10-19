package com.bikego.domain;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Bandeja de entrega local para verificar correo y alertas sin un proveedor externo. */
@Entity
public class Mensaje {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    public String destinatario;
    public String canal;
    public String asunto;
    @Column(length=8000) public String contenido;
    public LocalDateTime fecha;
    protected Mensaje() {}
    public Mensaje(String destinatario,String canal,String asunto,String contenido,LocalDateTime fecha) {
        this.destinatario=destinatario;this.canal=canal;this.asunto=asunto;this.contenido=contenido;this.fecha=fecha;
    }
}
