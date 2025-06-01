package es.salud.vacunas

import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.*

import org.springframework.core.env.Environment

// Importo el Servicio asociado a este Controller
import es.salud.vacunas.ConsultarDatosOrigenService

// Controlador de consultarDatosOrigen

@Controller
@RequestMapping("consultarDatosOrigen")
class ConsultarDatosOrigenController {

    
    @Inject
    ConsultarDatosOrigenService consultarDatosOrigenService

    // Saco el valor de usuario del fichero propperties.yml
    String usuario = env.getProperty("gestion.usuario")

    String consultarDatosOrigen(
    @RequestPart("registroId") String registroId,
    @RequestPart("codigoSns") String codigoSns,
    @RequestPart("dniNie") String dniNie,
    @RequestPart("fechaNacimiento") String fechaNacimiento,
    @RequestPart("cipAut") String cipAut,
    @RequestPart("otroDoc") String otroDoc,
    @RequestPart("tipoDoc") String tipoDoc,
    @RequestPart("codigoTipoProducto") String codigoTipoProducto,
    @RequestPart("codigoProductoMarcaC") String codigoProductoMarcaC,
    @RequestPart("esDocumentada") String esDocumentada
    ) {
      return consultarDatosOrigenService.consultarDatosOrigen(
        usuario, registroId, codigoSns, dniNie, fechaNacimiento,
        cipAut, otroDoc, tipoDoc, codigoTipoProducto, codigoProductoMarcaC, esDocumentada
        )

    }
}
