/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Represents an XML based input which can be processed using xpath.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
public class XMLStructuredInput implements StructuredInput {

    /**
     * Creates a new XMLStructuredInput for the given stream.
     *
     * @param in    the InputStream containing the xml data.
     * @param close determines whether the stream should be closed after parsing or not
     * @throws IOException if an io error occurs while parsing the input xml
     */
    public XMLStructuredInput(InputStream in, boolean close) throws IOException {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(in);
            node = new XMLNodeImpl(doc.getDocumentElement());
            if (close) {
                in.close();
            }
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        } catch (SAXException e) {
            throw new IOException(e);
        }
    }

    private XMLNodeImpl node;

    @Override
    public StructuredNode getNode(String xpath) throws XPathExpressionException {
        return node.queryNode(xpath);
    }

    @Override
    public String toString() {
        return node == null ? "" : node.toString(); //$NON-NLS-1$
    }

    /**
     * Overrides the root node to reset this document to a subtree of the original input
     *
     * @param node the new root node of this input
     */
    public void setNewParent(StructuredNode node) {
        if (node != null && node instanceof XMLNodeImpl) {
            this.node = (XMLNodeImpl) node;
        }
    }
}
