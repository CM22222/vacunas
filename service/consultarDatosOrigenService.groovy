package es.salud.vacunas

import groovy.xml.XmlSlurper
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.springframework.core.env.Environment

// Importo el paquete que crea el mensaje y lo envía
import es.salud.vacunas.firmaDatosOrigen

// Servicio que construye, firma y envía el mensaje SOAP de la consulta de datos origen

@Service
public class ConsultarDatosOrigenService {



    public String consultarDatosOrigen(String usuario, String registroId, String codigoSns, String dniNie,
                                       String fechaNacimiento, String cipAut, String otroDoc, String tipoDoc,
                                       String codigoTipoProducto, String codigoProductoMarcaC, String esDocumentada) {
        try {

            // Cojo del .yml los valores del certificado: ruta, clave, alias
            String rutacert =  env.getProperty("certificado.rutacert")
            String clavecert =  env.getProperty("certificado.clavecert")
            String aliascert =  env.getProperty("certificado.aliascert")
            String urldatosfinales =  env.getProperty("datosOrigen.url")


            return ClienteSoap.consultarDatosOrigen(
                usuario, registroId, codigoSns, dniNie, fechaNacimiento,
                cipAut, otroDoc, tipoDoc, codigoTipoProducto, codigoProductoMarcaC, esDocumentada,
                urldatosfinales, rutacert, clavecert, aliascert
            );
        } catch (Exception e) {
            throw new RuntimeException("Error consultando los datos de origen", e);
        }
    }
}
