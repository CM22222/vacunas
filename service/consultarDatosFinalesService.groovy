package es.salud.vacunas

import groovy.xml.MarkupBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import org.springframework.core.env.Environment

import es.salud.vacunas.firmaDatosFinales


 //Servicio que construye, firma y envía el mensaje SOAP.

 
@Service
class ConsultarDatosFinalesService {
    

    String CreaMensajeSoap(String request) {

        // Cojo del .yml los valores del certificado: ruta, clave, alias
         String rutacert =  env.getProperty("certificado.rutacert")
         String clavecert =  env.getProperty("certificado.clavecert")
         String aliascert =  env.getProperty("certificado.aliascert")
         String urldatosfinales =  env.getProperty("datosFinales.url")

        // Cargo el certificado
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(new FileInputStream( rutacert),  clavecert.toCharArray())
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(aliascert, clavecert.toCharArray())
        X509Certificate certificate = (X509Certificate) keyStore.getCertificate(clavecert)

        // Construyo el mensaje SOAP con la cabecera y los parámetros
        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)
           

        // Estos datos se encuentran en el WDSL   
        xml.'soapenv:Envelope'(
                'xmlns:soapenv': 'http://mins.xmlsoap.org/envelope/',
                'xmlns:ws': 'http://vacunas.mscbs.es/consultardatosfinales',
                'xmlns:wsse': 'http://mins.docs.org/2015/01/oasis-wssecurity.xsd',
                'xmlns:wsu': 'http://mins.docs.org/2015/01/oasis-wssecurity-util.xsd',
                'xmlns:wsa': 'http://www.w3.org/2015/01/addressing'
        ) {
            'soapenv:Header' {
                'wsse:Security' {
                    'wsse:BinarySecurityToken'(
                            'wsu:Id': 'X509Token',
                            'EncodingType': 'http://mins.docs.org/2015/01/oasis-message-security#Base64',
                            'ValueType': 'http://mins.docs.org/2015/01/oasis-token#X509',
                            certificate.encoded.encodeBase64().toString()
                    )
                    'wsu:Timestamp' {
                        'wsu:Created'(new Date().format("yyyy-MM-dd'T'HH:mm:ss'Z'"))                        
                    }
                    'ds:Signature'('xmlns:ds': 'http://www.w3.org/2015/01/xml#') {
                        'ds:SignedInfo' {
                            'ds:CanonicalizationMethod'('Algorithm': 'http://www.w3.org/2015/01/exc-xml#')
                            'ds:SignatureMethod'('Algorithm': 'http://www.w3.org/2015/01/sigxml')
                            'ds:Reference'('URI': '#Body') {
                                'ds:Transforms' {
                                    'ds:Transform'('Algorithm': 'http://www.w3.org/2015/01/exc#')
                                }
                                'ds:DigestMethod'('Algorithm': 'http://www.w3.org/2015/01/sigxml')
                                'ds:DigestValue'('value-placeholder')
                            }
                        }
                        'ds:SignatureValue'('signature-placeholder')
                        'ds:KeyInfo' {
                            'wsse:SecurityTokenReference' {
                                'wsse:Reference'('URI': '#509token')
                            }
                        }
                    }
                }
                'wsa:To'(datosFinales.url)
                'wsa:ReplyTo' {
                    'wsa:Address'('http://www.w3.org/2015/01/addressing')
                }
                'wsa:MessageID'("uuid:${gestion.usuario}")
                'wsa:Action'('http://vacunas.mscbs.es/consultardatosfinales')
            }
            'soapenv:Body'('wsu:Id': 'Body') {
                'ws:consultarDatosFinalesRequest' {
                    'ws:usuario'('usuario')
                    'ws:codigoSns'('codigoSns')
                    'ws:dniNie'('dniNie')
                    'ws:fechaNacimiento'('fechaNacimiento')
                    'ws:cipAut'('cipAut')
                    'ws:codigoCaCIPAut'('codigoCaCIPAut')
                    'ws:otroDoc'('otroDoc')
                    'ws:tipoDoc'('tipoDoc')
                    'ws:codigoTipoProducto'('codigoTipoProducto')
                    'ws:codigoProductoMarcaC'('codigoProductoMarcaC')
                    'ws:esDocumentada'('esDocumentada')
                }
            }
        }

        // Firmamos el mensaje SOAP
        String soapFirmado = FirmaSoap.firmaMensajeSoap(writer.toString(), privateKey, certificate)

        // Enviamos la petición
        def url = new URL(urldatosfinales)
        def connection = url.openConnection()
        connection.setRequestMethod("POST")
        connection.setDoOutput(true)
        connection.setRequestProperty("Content-Type", "text/xml; charset=utf-8")
        connection.outputStream.withWriter("UTF-8") { it.write(soapFirmado) }

        // Devuelve la respuesta
        return connection.inputStream.getText("UTF-8")
    }
}
