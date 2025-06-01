package es.salud.vacunas

import groovy.xml.MarkupBuilder
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.xml.crypto.dsig.*
import javax.xml.crypto.dsig.dom.DOMSignContext
import javax.xml.crypto.dsig.keyinfo.KeyInfo
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory
import javax.xml.crypto.dsig.keyinfo.X509Data
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import java.util.UUID
import java.util.Base64
import java.net.HttpURLConnection

// Paquete para poder coger los valores del .yml
import org.springframework.core.env.Environment


 //Aquí se monta el mensaje SOAP, se firma y se envía 
 
class ClienteSoap {

    static void enviarSoap(String usuario, String tipoFichero, String nombreFichero, byte[] contenido,
                           String endpoint, String certPath, String certPassword, String certAlias) {

        // Cogemos los datos del certificado
        KeyStore ks = KeyStore.getInstance("PKCS12")
        ks.load(new FileInputStream(certPath), certPassword.toCharArray())
        PrivateKey privateKey = (PrivateKey) ks.getKey(certAlias, certPassword.toCharArray())
        X509Certificate cert = (X509Certificate) ks.getCertificate(certAlias)

        // Crea el documento XML SOAP
        def docFactory = DocumentBuilderFactory.newInstance()
        docFactory.setNamespaceAware(true)
        def docBuilder = docFactory.newDocumentBuilder()
        def doc = docBuilder.newDocument()

        // Se crea la estructura SOAP. Estos valores viene definidos en el Wsdl 
        def soapNS = "http://xmlsoap.org/envelope/"
        def wsNS = "http://vacunas.mscbs.es/cargarfichero"
        def addressingNS = "http://www.w3.org/2015/01/addressing"

        def envelope = doc.createElementNS(soapNS, "soap:Envelope")
        doc.appendChild(envelope)

        def header = doc.createElementNS(soapNS, "soap:Header")
        def body = doc.createElementNS(soapNS, "soap:Body")
        envelope.appendChild(header)
        envelope.appendChild(body)

        def to = doc.createElementNS(addressingNS, "wsa:To")
        to.textContent = endpoint
        header.appendChild(to)

        def action = doc.createElementNS(addressingNS, "wsa:Action")
        action.textContent = "http://vacunas.mscbs.es/cargarfichero/action"
        header.appendChild(action)

        def messageId = doc.createElementNS(addressingNS, "wsa:MessageID")
        messageId.textContent = "uuid:" + UUID.randomUUID().toString()
        header.appendChild(messageId)

        // generamos el timestamp
        def timestamp = doc.createElementNS("http://mins.docs.org/wss/oasis-wssecurity-util.xsd", "wsu:Timestamp")
        def created = doc.createElement("wsu:Created")
        created.textContent = new Date().format("yyyy-MM-dd'T'HH:mm:ss'Z'")
        timestamp.appendChild(created)
        header.appendChild(timestamp)

        // se crea el cuerpo del mensaje
        def request = doc.createElementNS(wsNS, "tns:cargarFicheroRequest")
        def usuarioNode = doc.createElement("usuario")
        usuarioNode.textContent = usuario
        def tipoNode = doc.createElement("tipoFichero")
        tipoNode.textContent = tipoFichero
        def nombreNode = doc.createElement("nombreFichero")
        nombreNode.textContent = nombreFichero
        def contenidoNode = doc.createElement("contenidoFichero")
        contenidoNode.textContent = Base64.encoder.encodeToString(contenido)

        request.appendChild(usuarioNode)
        request.appendChild(tipoNode)
        request.appendChild(nombreNode)
        request.appendChild(contenidoNode)
        body.appendChild(request)

        // Se firma el mensaje
        XMLSignatureFactory sigFactory = XMLSignatureFactory.getInstance("DOM")
        Reference ref = sigFactory.newReference(
                "#Body",
                sigFactory.newDigestMethod(DigestMethod.SHA256, null),
                [sigFactory.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)],
                null, null)

        SignedInfo signedInfo = sigFactory.newSignedInfo(
                sigFactory.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
                sigFactory.newSignatureMethod(SignatureMethod.RSA_SHA256, null),
                [ref])

        KeyInfoFactory kif = sigFactory.getKeyInfoFactory()
        X509Data x509Data = kif.newX509Data([cert])
        KeyInfo keyInfo = kif.newKeyInfo([x509Data])

        DOMSignContext signContext = new DOMSignContext(privateKey, envelope)
        XMLSignature signature = sigFactory.newXMLSignature(signedInfo, keyInfo)
        signature.sign(signContext)

        // Convertimos a String para enviar
        def transformer = TransformerFactory.newInstance().newTransformer()
        def writer = new StringWriter()
        transformer.transform(new DOMSource(doc), new StreamResult(writer))
        def soapXml = writer.toString()

        // Enviar por HTTP 
        def url = new URL(endpoint)
        HttpURLConnection conn = (HttpURLConnection) url.openConnection()
        conn.setRequestMethod("POST")
        conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8")
        conn.setDoOutput(true)

        conn.outputStream.withWriter("UTF-8") { it.write(soapXml) }

        def responseCode = conn.responseCode
        if (responseCode != 200) {
            throw new RuntimeException("Error SOAP: HTTP $responseCode - ${conn.errorStream?.text}")
        }
    }
}
