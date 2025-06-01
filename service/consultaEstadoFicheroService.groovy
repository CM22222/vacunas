package es.salud.vacunas

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ws.util.SoapClient;

// Paquete para poder coger los valores del .yml
import org.springframework.core.env.Environment

 //Servicio que consulta el estado de un fichero a partir del id del fichero
 
@Service
public class consultaEstadoFicheroService {
    


    public String consultarEstadoFichero(String usuario, String ficheroID) {
        try {

                // Saco los valores del application.yml    
            String certPath = env.getProperty("certificado.rutacert")
            String certPassword = env.getProperty("certificado.clavecert")
            String certAlias = env.getProperty("certificado.aliascert")
            String urlEnvio = env.getProperty("estado.url");            

            return ClienteSoap.consultarEstado(usuario, ficheroID, urlEnvio, certPath, certPassword, certAlias);
        } catch (Exception e) {
            throw new RuntimeException("Error consultando el estado del fichero", e);
        }
    }
}
