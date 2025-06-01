package es.salud.vacunas

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
import java.net.HttpURLConnection


 //Cliente SOAP que construye el mensaje SOAP, lo firma y lo envía.
 
class ClienteSoap {

    static String consultarEstado(String usuario, String ficheroID, String endpoint, String certPath, String certPassword, String certAlias) {

        // Cargamos el certificado
        KeyStore ks = KeyStore.getInstance("PKCS12")
        ks.load(new FileInputStream(certPath), certPassword.toCharArray())
        PrivateKey privateKey = (PrivateKey) ks.getKey(certAlias, certPassword.toCharArray())
        X509Certificate cert = (X509Certificate) ks.getCertificate(certAlias)

        // Se crea el documento XML 
        def docFactory = DocumentBuilderFactory.newInstance()
        docFactory.setNamespaceAware(true)
        def docBuilder = docFactory.newDocumentBuilder()
        def doc = docBuilder.newDocument()

        // Se crea la estructura SOAP. Los valores se obtienen del WDSL 
        def soapNS = "http://mins.xmlsoap.org/envelope/"
        def wsNS = "http://vacunas.mscbs.es/consultarestadofichero"
        def addressingNS = "http://www.w3.org/2015/01/addressing"

        def envelope = doc.createElementNS(soapNS, "soap:Envelope")
        doc.appendChild(envelope)

        def header = doc.createElementNS(soapNS, "soap:Header")
        def body = doc.createElementNS(soapNS, "soap:Body")
        envelope.appendChild(header)
        envelope.appendChild(body)

        // WS-Addressing
        def to = doc.createElementNS(addressingNS, "wsa:To")
        to.textContent = endpoint
        header.appendChild(to)

        def action = doc.createElementNS(addressingNS, "wsa:Action")
        action.textContent = "http://vacunas.mscbs.es/consultarestadofichero/action"
        header.appendChild(action)

        def messageId = doc.createElementNS(addressingNS, "wsa:MessageID")
        messageId.textContent = "uuid:" + UUID.randomUUID().toString()
        header.appendChild(messageId)

        // Generamos el timestamp
        def timestamp = doc.createElementNS("http://mins.docs./2015/01/oasis-wssecurity-util.xsd", "wsu:Timestamp")
        def created = doc.createElement("wsu:Created")
        created.textContent = new Date().format("yyyy-MM-dd'T'HH:mm:ss'Z'")
        timestamp.appendChild(created)
        header.appendChild(timestamp)

        // Formamos el cuerpo del mensaje
        def request = doc.createElementNS(wsNS, "tns:consultarEstadoFicheroRequest")
        def usuarioNode = doc.createElement("usuario")
        usuarioNode.textContent = usuario
        def ficheroIdNode = doc.createElement("ficheroId")
        ficheroIdNode.textContent = ficheroID

        request.appendChild(usuarioNode)
        request.appendChild(ficheroIdNode)
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

        // Convertimos a String
        def transformer = TransformerFactory.newInstance().newTransformer()
        def writer = new StringWriter()
        transformer.transform(new DOMSource(doc), new StreamResult(writer))
        def soapXml = writer.toString()

        // Enviamos por HTTP POST
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

        return conn.inputStream.text
    }
}
