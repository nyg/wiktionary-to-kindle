package edu.self.w2k.write.epub;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class OpfPostProcessor {

    private static final String OPF_NS = "http://www.idpf.org/2007/opf";

    private final String srcLang;
    private final String trgLang;

    void process(Path epubFile) throws IOException {
        URI jarUri = URI.create("jar:" + epubFile.toUri());
        try (FileSystem fs = FileSystems.newFileSystem(jarUri, Map.of())) {
            String opfFullPath = findOpfPath(fs);
            injectXMetadata(fs, opfFullPath);
        }
    }

    private String findOpfPath(FileSystem fs) throws IOException {
        byte[] containerBytes = Files.readAllBytes(fs.getPath("/META-INF/container.xml"));
        try {
            Document doc = parseNamespaceAware(containerBytes);
            // Try namespace-aware lookup first, then fall back to local name
            NodeList rootfiles = doc.getElementsByTagNameNS("*", "rootfile");
            if (rootfiles.getLength() == 0) {
                rootfiles = doc.getElementsByTagName("rootfile");
            }
            if (rootfiles.getLength() == 0) {
                throw new IOException("No rootfile element in META-INF/container.xml");
            }
            return ((Element) rootfiles.item(0)).getAttribute("full-path");
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("Failed to parse META-INF/container.xml", e);
        }
    }

    private void injectXMetadata(FileSystem fs, String opfFullPath) throws IOException {
        Path opfPath = fs.getPath("/" + opfFullPath);
        byte[] opfBytes = Files.readAllBytes(opfPath);
        try {
            Document doc = parseNamespaceAware(opfBytes);

            // Find <metadata> element (may have opf: prefix or no prefix)
            NodeList metadataList = doc.getElementsByTagNameNS(OPF_NS, "metadata");
            if (metadataList.getLength() == 0) {
                metadataList = doc.getElementsByTagName("metadata");
            }
            if (metadataList.getLength() == 0) {
                throw new IOException("No metadata element in OPF: " + opfFullPath);
            }
            Element metadata = (Element) metadataList.item(0);

            Element xMetadata = doc.createElement("x-metadata");
            Element dictIn = doc.createElement("DictionaryInLanguage");
            dictIn.setTextContent(srcLang);
            Element dictOut = doc.createElement("DictionaryOutLanguage");
            dictOut.setTextContent(trgLang);
            xMetadata.appendChild(dictIn);
            xMetadata.appendChild(dictOut);
            metadata.appendChild(xMetadata);

            Files.write(opfPath, serialize(doc));
        } catch (ParserConfigurationException | SAXException | TransformerException e) {
            throw new IOException("Failed to inject x-metadata into OPF", e);
        }
    }

    private static Document parseNamespaceAware(byte[] bytes)
            throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setEntityResolver((publicId, systemId) ->
                new org.xml.sax.InputSource(new java.io.StringReader("")));
        try (InputStream is = new ByteArrayInputStream(bytes)) {
            return builder.parse(is);
        }
    }

    private static byte[] serialize(Document doc) throws TransformerException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(doc), new StreamResult(baos));
        return baos.toByteArray();
    }
}
