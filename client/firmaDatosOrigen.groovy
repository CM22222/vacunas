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

import es.salud.vacunas.firmaDatosOrigen


 
 // Cliente SOAP que construye el mensaje SOAP, lo firma y hace el envío
 Class ClienteSoap {

    static String consultarDatosOrigen(
        String usuario, String registroId, String codigoSns, String dniNie,
        String fechaNacimiento, String cipAut, String otroDoc, String tipoDoc,
        String codigoTipoProducto, String codigoProductoMarcaC, String esDocumentada,
        String urldatosorigen, String rutacert, String clavecert, String aliascert
    ) {
        // Cargamos el certificado desde el keystore
        KeyStore ks = KeyStore.getInstance("PKCS12")
        ks.load(new FileInputStream(rutacert), clavecert.toCharArray())
        PrivateKey privateKey = (PrivateKey) ks.getKey(aliascert, clavecert.toCharArray())
        X509Certificate cert = (X509Certificate) ks.getCertificate(aliascert)

        // Se crea el documento XML SOAP
        def docFactory = DocumentBuilderFactory.newInstance()
        docFactory.setNamespaceAware(true)
        def docBuilder = docFactory.newDocumentBuilder()
        def doc = docBuilder.newDocument()

        // Estos datos nos los indica el wsdl 
        def soapNS = "http://mins.org/envelope/"
        def wsNS = "http://vacunas.mscbs.es/consultardatosorigen"
        def addressingNS = "http://www.w3.org/2015/01/addressing"
        def wsuNS = "http://mins.docs.org/2015/01/oasis-wssecurity-util.xsd"

        // Se crea el envelope
        def envelope = doc.createElementNS(soapNS, "soap:Envelope")
        doc.appendChild(envelope)

        def header = doc.createElementNS(soapNS, "soap:Header")
        def body = doc.createElementNS(soapNS, "soap:Body")
        envelope.appendChild(header)
        envelope.appendChild(body)

        
        def to = doc.createElementNS(addressingNS, "wsa:To")
        to.textContent = urldatosorigen
        header.appendChild(to)

        // Esta url se indica en el wsdl
        def action = doc.createElementNS(addressingNS, "wsa:Action")
        action.textContent = "http://vacunas.mscbs.es/consultardatosorigen"
        header.appendChild(action)

        def messageId = doc.createElementNS(addressingNS, "wsa:MessageID")
        messageId.textContent = "uuid:" + UUID.randomUUID().toString()
        header.appendChild(messageId)

        // Creamos el timestamp
        def timestamp = doc.createElementNS(wsuNS, "wsu:Timestamp")
        def created = doc.createElementNS(wsuNS, "wsu:Created")
        created.textContent = new Date().format("yyyy-MM-dd'T'HH:mm:ss'Z'")
        timestamp.appendChild(created)
        header.appendChild(timestamp)

        // Se crea el cuerpo del mensaje
        def request = doc.createElementNS(wsNS, "tns:consultarDatosOrigenRequest")
        def add = { name, value ->
            def node = doc.createElement(name)
            node.textContent = value
            request.appendChild(node)
        }

        // Valores que enviamos para hacer la petición  
        add("usuario", usuario)
        add("registroId", registroId)
        add("codigoSns", codigoSns)
        add("dniNie", dniNie)
        add("fechaNacimiento", fechaNacimiento)
        add("cipAut", cipAut)
        add("otroDoc", otroDoc)
        add("tipoDoc", tipoDoc)
        add("codigoTipoProducto", codigoTipoProducto)
        add("codigoProductoMarcaC", codigoProductoMarcaC)
        add("esDocumentada", esDocumentada)

        body.appendChild(request)

        // Firmamos el mensaje SOAP
        XMLSignatureFactory sigFactory = XMLSignatureFactory.getInstance("DOM")
        Reference ref = sigFactory.newReference(
            "", // referencia al documento completo
            sigFactory.newDigestMethod(DigestMethod.SHA256, null),
            [sigFactory.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)],
            null, null
        )

        SignedInfo signedInfo = sigFactory.newSignedInfo(
            sigFactory.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, null),
            sigFactory.newSignatureMethod(SignatureMethod.RSA_SHA256, null),
            [ref]
        )

        KeyInfoFactory kif = sigFactory.getKeyInfoFactory()
        X509Data x509Data = kif.newX509Data([cert])
        KeyInfo keyInfo = kif.newKeyInfo([x509Data])

        DOMSignContext signContext = new DOMSignContext(privateKey, envelope)
        signContext.setDefaultNamespacePrefix("ds")
        XMLSignature signature = sigFactory.newXMLSignature(signedInfo, keyInfo)
        signature.sign(signContext)

        // Convertimos a XML 
        def transformer = TransformerFactory.newInstance().newTransformer()
        def writer = new StringWriter()
        transformer.transform(new DOMSource(doc), new StreamResult(writer))
        def soapXml = writer.toString()

        // Enviamos la petición
        def url = new URL(urldatosorigen)
        HttpURLConnection conn = (HttpURLConnection) url.openConnection()
        conn.setRequestMethod("POST")
        conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8")
        conn.setDoOutput(true)

        conn.outputStream.withWriter("UTF-8") { it.write(soapXml) }

        def responseCode = conn.responseCode
        if (responseCode != 200) {
            throw new RuntimeException("Error SOAP: HTTP $responseCode - ${conn.errorStream?.text}")
        }

        // Leer respuesta como XML
        return conn.inputStream.text
    }
}
