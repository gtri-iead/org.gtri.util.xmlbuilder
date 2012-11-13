/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.gtri.util.xmlbuilder;

import java.io.InputStream;
import java.io.OutputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.gtri.util.iteratee.impl.Consumer;
import org.gtri.util.iteratee.impl.Producer;
import org.gtri.util.xmlbuilder.impl.XmlEvent;
import org.gtri.util.xmlbuilder.impl.XmlReader;
import org.gtri.util.xmlbuilder.impl.XmlWriter;
        
/**
 *
 * @author Lance
 */
public class XmlFactory {
  private XmlFactory() { }
  public static final XmlFactory INSTANCE = new XmlFactory();
  public static XmlFactory instance() { return INSTANCE; }
  public static final int STD_CHUNK_SIZE = 256;
  
  public Producer<XmlEvent> createXmlReader(XMLStreamReader xmlStreamReader, int chunkSize) {
    return new XmlReader(xmlStreamReader, chunkSize);
  }
  
  public Producer<XmlEvent> createXmlReader(XMLStreamReader xmlStreamReader) {
    return createXmlReader(xmlStreamReader, STD_CHUNK_SIZE);
  }
  
  public Producer<XmlEvent> createXmlReader(InputStream in) throws XMLStreamException {
    XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(in);
    return createXmlReader(reader, STD_CHUNK_SIZE);
  }
  
  public Consumer<XmlEvent> createXmlWriter(XMLSerializer serializer) {
    return new XmlWriter(serializer);
  }
  
  public Consumer<XmlEvent> createXmlWriter(OutputStream out, OutputFormat outputFormat) {
    return createXmlWriter(new XMLSerializer(out, outputFormat));
  }
  
  public Consumer<XmlEvent> createXmlWriter(OutputStream out) {
    OutputFormat outputFormat = new OutputFormat("XML","UTF-8",true);
    outputFormat.setIndent(2);
    outputFormat.setIndenting(true);
    return createXmlWriter(new XMLSerializer(out, outputFormat));
  }
}
