
package es.salud.vacunas

import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

import org.springframework.core.env.Environment

import es.salud.vacunas.envioFicheroService
import es.salud.vacunas.firmaEnvio

import javax.inject.Inject


 // Controller para el envío de ficheros 
  
 
@Controller
@RequestMapping("EnvioFichero")
class EnvioFicheroController {

    @Inject
    EnvioFicheroService EnvioFicheroService

    // Saco el valor de usuario del fichero propperties.yml
    String usuario = env.getProperty("gestion.usuario")
    
        // tipoFichero = 1 -> fichero de carga de dosis.  tipofichero = 2 -> fichero de borrado de dosis
        String enviarFichero(
        @RequestPart("tipoFichero") String tipoFichero,
        @RequestPart("contenidoFichero") MultipartFile contenidoFichero,
        @RequestPart("nombreFichero") String nombreFichero) {

        return EnvioFicheroService.procesarFichero(usuario, tipoFichero, contenidoFichero, nombreFichero)
    }
}
