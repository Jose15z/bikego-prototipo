package com.bikego.web;

import com.bikego.service.UsuarioService;
import com.bikego.service.AutenticacionService;
import com.bikego.web.dto.MetodoPagoRequest;
import com.bikego.web.dto.UsuarioDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarios;
    private final AutenticacionService autenticacion;

    public UsuarioController(UsuarioService usuarios, AutenticacionService autenticacion) {
        this.usuarios = usuarios;
        this.autenticacion = autenticacion;
    }

    @PutMapping("/me/metodo-pago")
    public UsuarioDto actualizarMetodoPago(@RequestHeader(value = "Authorization", required = false) String auth,
                                           @Valid @RequestBody MetodoPagoRequest req) {
        String id = autenticacion.autenticar(auth).getId();
        return usuarios.actualizarMetodoPago(id, req);
    }
    @org.springframework.web.bind.annotation.PostMapping("/me/validar-pago")
    public UsuarioDto validar(@RequestHeader(value="Authorization",required=false) String auth) {
        return usuarios.validar(autenticacion.autenticar(auth).getId());
    }
    @org.springframework.web.bind.annotation.GetMapping("/me/rechazos")
    public Object rechazos(@RequestHeader(value="Authorization",required=false) String auth) {
        return usuarios.rechazos(autenticacion.autenticar(auth).getId());
    }
}
