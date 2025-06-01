package es.salud.vacunas

import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.*
// Paquete para poder coger los valores del .yml
import org.springframework.core.env.Environment

import es.salud.vacunas.consultaEstadoFicheroService

import javax.inject.Inject


 //Controller para consultar el estado de un fichero. Se le pasa como parámetro el Id del fichero
 
@Controller
@RequestMapping("ConsultaEstadoFichero")
class ConsultaEstadoFicheroController {

    @Inject
    FicheroService ficheroService

    // Saco el valor de usuario del fichero propperties.yml    
    String usuario = env.getProperty("gestion.usuario")

    
    String consultarEstadoFichero(
            @RequestPart("usuario") String usuario, @RequestPart("ficheroID") String ficheroID) {

        return consultaEstadoFicheroService.consultarEstadoFichero(usuario, ficheroID)
    }
}
