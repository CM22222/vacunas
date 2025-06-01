package es.salud.vacunas

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.core.env.Environment

import es.salud.vacunas.firmaEnvio


import java.io.File;
import java.io.FileOutputStream;
import java.util.UUID;


 //En este servicio se comprueba que el fichero cumple las condiciones de envío,
 // se guarda en el NFS y se envía 
 
@Service
public class EnvioFicheroService {

    // Saco los valores del application.yml
    String nfspath = env.getProperty("gestion.nfs_path")
    String certPath = env.getProperty("certificado.rutacert")
    String certPassword = env.getProperty("certificado.clavecert")
    String certAlias = env.getProperty("certificado.aliascert")
    String urlEnvio = env.getProperty("envio.url");


    public String procesarFichero(String usuarioId, String tipoFichero, MultipartFile contenidoFichero, String nombreFichero) {
        // Compruebo que cumple las condsiciones para poder enviar
        // El fichero es 1 = carga de dosis, 2=borrado de dosis
        if (!tipoFichero.equals("1") && !tipoFichero.equals("2")) {
            throw new IllegalArgumentException("tipoFichero debe ser 1 o 2");
        }
    

        // El fichero enviado tiene que ser de tipo zip o 7z
        if (!nombreFichero.endsWith(".zip") && !nombreFichero.endsWith(".7z")) {
            throw new IllegalArgumentException("El fichero debe ser .zip o .7z");
        }
 
        // El tamaño máximo del fichero debe de ser de 50 Mb. 
        // Cuando se genera el fichero en la aplicación ya se va dividiendo para que ninguna de las partes ocupe más de 49 MB

        if (contenidoFichero.getSize() > 50 * 1024 * 1024) {
            throw new IllegalArgumentException("El fichero no puede superar los 50MB");
        }

        try {
            // El fichero se guardar en el NFS
            // Saco la ruta deel NFS del fichero .yml
            String nfspath = env.getProperty("gestion.nfs_path")
            File destino = new File(nfsPath, nombreFichero);
            try (FileOutputStream fos = new FileOutputStream(destino)) {
                IOUtils.copy(contenidoFichero.getInputStream(), fos);
            }

            // Se envía el fichero
            byte[] contenido = contenidoFichero.getBytes();
            String ficheroId = UUID.randomUUID().toString();


            ClienteSoap.enviarSoap(usuarioId, tipoFichero, nombreFichero, contenido, urlEnvio, certPath, certPassword, certAlias);

            return ficheroId;
        } catch (Exception e) {
            throw new RuntimeException("Error procesando el fichero", e);
        }
    }
}
