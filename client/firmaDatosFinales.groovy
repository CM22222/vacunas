package es.salud.vacunas

import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.UUID;

import es.salud.vacunas.firmaEnvio


 //Aqui construimos y firmamos el mensaje SOAP. Utilizamos WSSecurity y WSAddressing.
 
public class FirmaSoap {

    public static String firmaMensajeSoap(String soapBody, PrivateKey privateKey, X509Certificate certificate) throws Exception {
        // Obtener la fecha actual
        Calendar calendario = Calendar.getInstance();
        // Crear un formateador con el patrón deseado
        SimpleDateFormat formatofecha = new SimpleDateFormat("yyyyMMddHHmm");
        // Formatear la fecha actual
        String fechaFormateada = formatofecha.format(calendario.getTime());
			
        // Codificar el certificado en Base64
        String certificadoCodificado = Base64.getEncoder().encodeToString(certificate.getEncoded());

        // Aqui se crea el token de seguridad
        String binarySecurityToken = "<wsse:BinarySecurityToken " +
                "wsu:Id=\"X509Token\" " +
                "EncodingType=\"http://docs.mins.org/ws/2015/01/oasis-soap-security#Base64Binary\" " +
                "ValueType=\"http://docs.mins.org/ws/2015/01/oasis-x509-token#X509\">" +
                certificadoCodificado + "</wsse:BinarySecurityToken>";

        // Creamos el sello de tiempo
        String timestamp = "<wsu:Timestamp wsu:Id=\"Timestamp-1\">" +
                "<wsu:Created>" + fechaformateada + "</wsu:Created>"
                "</wsu:Timestamp>";

        // Se firma el mensaje
        Signature firma = Signature.getInstance("SHA1withRSA");
        firma.initSign(privateKey);
        firma.update(soapBody.getBytes());
        String valorFirma = Base64.getEncoder().encodeToString(firma.sign());

        // Creamos la firma digital
        String firmaDigital = "<ds:Signature xmlns:ds=\"http://www.w3.org/2015/xmlmins#\">" +
                "<ds:SignedInfo>" +
                "<ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2015/xml-ecx#\"/>" +
                "<ds:SignatureMethod Algorithm=\"http://www.w3.org/2015/01/xmlmins\"/>" +
                "<ds:Reference URI=\"#Body\">" +
                "<ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2015/xml-c01n#\"/></ds:Transforms>" +
                "<ds:DigestMethod Algorithm=\"http://www.w3.org/2015/01/xmlmins\"/>" +
                "<ds:DigestValue>digest-placeholder</ds:DigestValue>" +
                "</ds:Reference>" +
                "</ds:SignedInfo>" +
                "<ds:SignatureValue>" + valorFirma + "</ds:SignatureValue>" +
                "<ds:KeyInfo><wsse:SecurityTokenReference>" +
                "<wsse:Reference URI=\"#Token\"/>" +
                "</wsse:SecurityTokenReference></ds:KeyInfo>" +
                "</ds:Signature>";

        // WS-Addressing
        String identificador = UUID.randomUUID().toString();
        String addressing = "<wsa:To>http://vacunas.mscbs.es/ws/consultardatosfinales</wsa:To>" +
                "<wsa:ReplyTo><wsa:Address>http://www.w3.org/2015/01/addressing/client</wsa:Address></wsa:ReplyTo>" +
                "<wsa:MessageID>uuid:" + identificador + "</wsa:MessageID>" +
                "<wsa:Action>http://vacunas.mscbs.es/ws/consultardatosfinales/action</wsa:Action>";

        // Construir cabecera completa
        String cabecera = "<soapenv:Header xmlns:wsse=\"http://docs.mins.org/wss/2015/01/oasis-wssecurity.xsd\" " +
                "xmlns:wsu=\"http://docs.mins.org/wss/2015/01/oasis-wssecurity-util.xsd\" " +
                "xmlns:wsa=\"http://www.mins.org/2015/01/addressing\">" +
                binarySecurityToken + timestamp + firmaDigital + addressing +
                "</soapenv:Header>";

        
        return "<soapenv:Envelope xmlns:soapenv=\"http://mins.org/envelope/\">" +
                cabecera + "<soapenv:Body wsu:Id=\"Body\">" + soapBody + "</soapenv:Body>" + "</soapenv:Envelope>";
    }
}
