package com.bikego.web;

import com.bikego.service.UsuarioService;
import com.bikego.web.dto.MetodoPagoRequest;
import com.bikego.web.dto.UsuarioDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarios;

    public UsuarioController(UsuarioService usuarios) {
        this.usuarios = usuarios;
    }

    /** Solo prototipo: lista los usuarios de prueba (no hay inicio de sesion en este alcance). */
    @GetMapping
    public List<UsuarioDto> listar() {
        return usuarios.listar();
    }

    @PutMapping("/{id}/metodo-pago")
    public UsuarioDto actualizarMetodoPago(@PathVariable String id, @Valid @RequestBody MetodoPagoRequest req) {
        return usuarios.actualizarMetodoPago(id, req);
    }
}
