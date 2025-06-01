package es.salud.vacunas

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

import es.salud.vacunas.consultarDatosFinalesService


 // Controlador de consultarDatosFinales.
 
 
@Controller
@RequestMapping("consultarDatosFinales")
class consultarDatosFinalesController {

    @Autowired
    consultarDatosFinalesService consultarDatosFinalesService
    
    String consultarDatosFinales(@RequestBody String peticion) {
        return consultarDatosFinalesService.CreaMensajeSoap(peticion)
    }
}
