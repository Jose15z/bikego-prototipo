package com.bikego.web;

import com.bikego.config.BikeGoProperties;
import com.bikego.config.DataSeeder;
import com.bikego.web.dto.ConfigDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DemoController {

    private final DataSeeder seeder;
    private final BikeGoProperties props;

    public DemoController(DataSeeder seeder, BikeGoProperties props) {
        this.seeder = seeder;
        this.props = props;
    }

    @GetMapping("/config")
    public ConfigDto config() {
        return new ConfigDto(props.getRadioInicialMetros(), props.getRadioMaximoMetros(), props.getBateriaMinima(),
                props.getTarifaBase(), props.getTarifaMinuto(), props.getBloqueoPagoMinutos(), props.getIotTimeoutMs());
    }

    /** Solo prototipo: deja los datos como al arrancar, util para repetir la demostracion. */
    @PostMapping("/demo/reiniciar")
    public ResponseEntity<Void> reiniciar() {
        seeder.cargar();
        return ResponseEntity.noContent().build();
    }
}
